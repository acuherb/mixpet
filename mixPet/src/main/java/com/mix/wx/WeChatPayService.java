package com.mix.wx;

import com.mix.common.BizException;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Client;
import com.mix.service.IBillService;
import com.mix.service.IClientService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 V3（JSAPI）骨架：参数占位未启用时抛 BizException，不初始化证书配置。
 * 单据号规则：BILL{billId}T{时间戳} —— 同一账单可多次重新拉起支付（失败/过期重建单），
 * 回调侧解析 billId 后走 BillService.payByWeChat 的 CAS 收款（防重）。
 */
@Slf4j
@Service
public class WeChatPayService {
    private final WeChatProperties props;
    private final IBillService billService;
    private final IClientService clientService;
    /** 懒构建：首次真实调用才加载商户私钥/平台证书，占位配置不影响启动 */
    private volatile RSAAutoCertificateConfig payConfig;

    public WeChatPayService(WeChatProperties props, IBillService billService, IClientService clientService) {
        this.props = props;
        this.billService = billService;
        this.clientService = clientService;
    }

    public boolean enabled() {
        return props.isEnabled();
    }

    private RSAAutoCertificateConfig config() {
        if (!enabled()) throw new BizException("微信支付未启用（mix.wx.enabled=false）");
        if (payConfig == null) {
            synchronized (this) {
                if (payConfig == null) {
                    payConfig = new RSAAutoCertificateConfig.Builder()
                            .merchantId(props.getMchId())
                            .privateKeyFromPath(props.getPrivateKeyPath())
                            .merchantSerialNumber(props.getMchSerialNo())
                            .apiV3Key(props.getApiV3Key())
                            .build();
                }
            }
        }
        return payConfig;
    }

    /** 应付金额（分）：优先生成列 real_money，回退 total_money */
    public static BigDecimal dueOf(Bill bill) {
        return bill.getRealMoney() != null ? bill.getRealMoney() : bill.getTotalMoney();
    }

    /**
     * JSAPI 预下单：校验账单归属与未付状态 → 拉起支付 → 商户单号落库 → 返回 WeixinJSBridge.invoke 参数。
     */
    public Map<String, String> createJsapiPay(Integer clientId, Integer billId) {
        Bill bill = billService.getById(billId);
        if (bill == null || !clientId.equals(bill.getClientId()))
            throw new BizException("账单不存在或不属于当前客户");
        if (Boolean.TRUE.equals(bill.getPayStatus())) throw new BizException("该账单已收款，请勿重复支付");
        Client client = clientService.getById(clientId);
        String openid = client == null ? null : client.getClientOpenid();
        if (openid == null || openid.isBlank())
            throw new BizException("尚未绑定微信，请先在公众号完成绑定");

        BigDecimal due = dueOf(bill);
        if (due == null || due.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("账单金额异常");

        String outTradeNo = "BILL" + billId + "T" + System.currentTimeMillis();
        PrepayRequest request = new PrepayRequest();
        request.setAppid(props.getAppId());
        request.setMchid(props.getMchId());
        request.setDescription("宠物医院诊疗费(账单#" + billId + ")");
        request.setNotifyUrl(props.getNotifyUrl());
        request.setOutTradeNo(outTradeNo);
        Amount amount = new Amount();
        amount.setTotal(due.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).intValue());
        request.setAmount(amount);
        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);

        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config()).build();
        PrepayWithRequestPaymentResponse pay = service.prepayWithRequestPayment(request);
        // 商户单号落库：查单/退款/对账都依赖它
        billService.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getWxOutTradeNo, outTradeNo)
                .update();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("timeStamp", pay.getTimeStamp());
        params.put("nonceStr", pay.getNonceStr());
        params.put("package", pay.getPackageVal());
        params.put("signType", pay.getSignType());
        params.put("paySign", pay.getPaySign());
        log.info("微信预下单成功 billId={} due={} outTradeNo={}", billId, due, outTradeNo);
        return params;
    }

    /** 回调验签解密器（WxPayController 用） */
    public RSAAutoCertificateConfig configForNotify() {
        return config();
    }

    /** 查单（对账用）：返回微信侧交易状态；未预下单过抛异常 */
    public Transaction queryByBillId(Integer billId) {
        Bill bill = billService.getById(billId);
        if (bill == null || bill.getWxOutTradeNo() == null) throw new BizException("该账单无微信预下单记录");
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config()).build();
        QueryOrderByOutTradeNoRequest q = new QueryOrderByOutTradeNoRequest();
        q.setMchid(props.getMchId());
        q.setOutTradeNo(bill.getWxOutTradeNo());
        return service.queryOrderByOutTradeNo(q);
    }

    /**
     * 微信退款（骨架）：按微信侧应付金额原路退。接入时需与 BillServiceImpl.refund 联动
     * （仅微信支付的账单走此通道，现金/余额退款仍走原逻辑）。
     */
    public Refund createRefund(Integer billId, BigDecimal refundFee, String reason) {
        Bill bill = billService.getById(billId);
        if (bill == null || bill.getWxOutTradeNo() == null) throw new BizException("该账单无微信预下单记录");
        RefundService service = new RefundService.Builder().config(config()).build();
        CreateRequest req = new CreateRequest();
        req.setOutTradeNo(bill.getWxOutTradeNo());
        req.setOutRefundNo("R" + billId + "T" + System.currentTimeMillis());
        req.setReason(reason == null || reason.isBlank() ? "账单退款" : reason);
        req.setNotifyUrl(props.getNotifyUrl().replace("/pay/notify", "/refund/notify"));
        AmountReq amount = new AmountReq();
        amount.setRefund(refundFee.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValue());
        amount.setTotal(dueOf(bill).movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValue());
        amount.setCurrency("CNY");
        req.setAmount(amount);
        return service.create(req);
    }

    /** 商户单号 → billId（BILL{billId}T{ts}） */
    public static Integer billIdOfOutTradeNo(String outTradeNo) {
        if (outTradeNo == null || !outTradeNo.startsWith("BILL")) return null;
        int t = outTradeNo.indexOf('T');
        if (t < 5) return null;
        try {
            return Integer.valueOf(outTradeNo.substring(4, t));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
