package com.mix.wx;

import com.mix.common.BizException;
import com.mix.model.entity.Client;
import com.mix.security.model.RoleConst;
import com.mix.service.IBillService;
import com.mix.util.UserUtil;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 微信支付入口：①顾客自助拉起 JSAPI 支付（登录 + 归属校验）②微信异步回调（SecurityConfig 放行 + SDK 验签解密）。
 * 退款走 WeChatPayService.createRefund（由 BillServiceImpl.refund 在微信支付账单场景调用，骨架期未接线）。
 */
@Slf4j
@RestController
@RequestMapping("/wx")
public class WxPayController {
    private final WeChatPayService payService;
    private final IBillService billService;

    public WxPayController(WeChatPayService payService, IBillService billService) {
        this.payService = payService;
        this.billService = billService;
    }

    /** 顾客自助拉起微信支付：返回 WeixinJSBridge.invoke('getBrandWCPayRequest') 所需参数 */
    @PostMapping("/pay/prepay/{billId}")
    @Secured({RoleConst.CLIENT, RoleConst.MANAGER, RoleConst.ADMIN})
    public Map<String, String> prepay(@PathVariable Integer billId) {
        if (!(UserUtil.getUser() instanceof Client client) || client.getClientId() == null)
            throw new BizException("仅顾客账号可自助支付");
        return payService.createJsapiPay(client.getClientId(), billId);
    }

    /**
     * 微信支付异步回调：支付成功 → payByWeChat CAS 收款（幂等，重复通知安全）；其余事件仅记日志。
     * 必须按微信规范返回：成功 {"code":"SUCCESS"}，失败 HTTP 5xx 让微信重试。
     */
    @PostMapping("/pay/notify")
    public ResponseEntity<Map<String, String>> notify(HttpServletRequest request, @RequestBody String body) {
        try {
            RequestParam param = new RequestParam.Builder()
                    .serialNumber(request.getHeader("Wechatpay-Serial"))
                    .nonce(request.getHeader("Wechatpay-Nonce"))
                    .signature(request.getHeader("Wechatpay-Signature"))
                    .timestamp(request.getHeader("Wechatpay-Timestamp"))
                    .body(body)
                    .build();
            NotificationParser parser = new NotificationParser(payService.configForNotify());
            Transaction tx = parser.parse(param, Transaction.class);
            if (tx != null && Transaction.TradeStateEnum.SUCCESS == tx.getTradeState()) {
                Integer billId = WeChatPayService.billIdOfOutTradeNo(tx.getOutTradeNo());
                if (billId == null) {
                    log.warn("微信回调商户单号无法解析：{}", tx.getOutTradeNo());
                } else {
                    billService.payByWeChat(billId, tx.getTransactionId());
                    log.info("微信支付回调成功 billId={} transactionId={}", billId, tx.getTransactionId());
                }
            } else {
                log.info("微信回调交易状态（未处理）：{}", tx == null ? "null" : tx.getTradeState());
            }
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception e) {
            log.error("微信回调处理失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", "FAILED", "message", e.getMessage() == null ? "失败" : e.getMessage()));
        }
    }
}
