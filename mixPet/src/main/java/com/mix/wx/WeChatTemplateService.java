package com.mix.wx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mix.model.entity.Client;
import com.mix.service.IClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公众号模板消息骨架（4 类）：挂号成功 / 就诊提醒 / 缴费提醒 / 免疫到期提醒。
 * 设计要点：
 * - 旁路：enabled=false 或客户未绑 openid 或未配模板 ID → 静默跳过，绝不影响主业务；
 * - 所有挂点调用处都用 try-catch 包裹（见各 Service 挂点注释）；
 * - 数据 key 采用老版通用模板字段（first/keyword1..3/remark），接入真实模板后按其字段定义微调 key；
 * - access_token 内存缓存（过期前 5 分钟刷新）；骨架不做分布式共享，单实例部署足够。
 */
@Slf4j
@Service
public class WeChatTemplateService {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final WeChatProperties props;
    private final IClientService clientService;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    /** access_token 缓存：[token, 过期时间戳(ms)] */
    private volatile String accessToken;
    private volatile long tokenExpireAt;

    public WeChatTemplateService(WeChatProperties props, IClientService clientService) {
        this.props = props;
        this.clientService = clientService;
    }

    // ===== 4 类模板入口（业务挂点调用） =====

    /** ①挂号成功通知（挂点：AppointmentServiceImpl.reserve 新建挂号成功后） */
    public void sendAppointmentSuccess(Integer clientId, String clientName, String doctorName, LocalDateTime time) {
        send("registerSuccess", clientId, Map.of(
                "first", "您的挂号已确认",
                "keyword1", clientName == null ? "" : clientName,
                "keyword2", doctorName == null ? "" : doctorName,
                "keyword3", time == null ? "" : DTF.format(time),
                "remark", "请按时到院就诊，如需改期请提前联系前台"));
    }

    /** ②就诊提醒（挂点：MedicalRecordServiceImpl 接诊建档后，提醒顾客就诊流程已开始） */
    public void sendVisitNotice(Integer clientId, String clientName, String doctorName) {
        send("visitRemind", clientId, Map.of(
                "first", "医生已接诊，请您前往诊室",
                "keyword1", clientName == null ? "" : clientName,
                "keyword2", doctorName == null ? "" : doctorName,
                "keyword3", DTF.format(LocalDateTime.now()),
                "remark", "就诊过程如有疑问请随时联系前台"));
    }

    /** ③缴费提醒（挂点：MedicalRecordServiceImpl 生成待付账单后） */
    public void sendPayRemind(Integer clientId, Integer billId, BigDecimal due) {
        send("payRemind", clientId, Map.of(
                "first", "您有一笔待缴费账单",
                "keyword1", "账单 #" + billId,
                "keyword2", "￥" + (due == null ? "0" : due.toPlainString()),
                "keyword3", DTF.format(LocalDateTime.now()),
                "remark", "可在公众号内使用余额或微信支付完成缴费"));
    }

    /** ④免疫到期提醒（挂点：ImmuneRecordServiceImpl.saveImmune 登记成功且能推算下次接种时间） */
    public void sendImmuneRemind(Integer clientId, String drugName, LocalDateTime nextTime) {
        send("immuneRemind", clientId, Map.of(
                "first", "宠物免疫接种提醒",
                "keyword1", drugName == null ? "" : drugName,
                "keyword2", nextTime == null ? "" : DTF.format(nextTime),
                "keyword3", "到期请按时接种",
                "remark", "可提前联系医院预约接种时间"));
    }

    // ===== 发送核心 =====

    private void send(String tplKey, Integer clientId, Map<String, String> data) {
        if (!props.isEnabled()) return;
        String templateId = templateIdOf(tplKey);
        if (templateId == null || templateId.isBlank() || "placeholder".equals(templateId)) return;
        Client client = clientId == null ? null : clientService.getById(clientId);
        String openid = client == null ? null : client.getClientOpenid();
        if (openid == null || openid.isBlank()) return;
        try {
            postTemplate(openid, templateId, data);
        } catch (Exception e) {
            log.warn("微信模板消息发送失败 tpl={} clientId={}: {}", tplKey, clientId, e.getMessage());
        }
    }

    private String templateIdOf(String key) {
        return switch (key) {
            case "registerSuccess" -> props.getTemplateIds().getRegisterSuccess();
            case "visitRemind" -> props.getTemplateIds().getVisitRemind();
            case "payRemind" -> props.getTemplateIds().getPayRemind();
            case "immuneRemind" -> props.getTemplateIds().getImmuneRemind();
            default -> null;
        };
    }

    /** POST /cgi-bin/message/template/send；非零 errcode 抛异常（上层吞掉记日志） */
    private void postTemplate(String openid, String templateId, Map<String, String> data) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", templateId);
        Map<String, Object> tplData = new LinkedHashMap<>();
        data.forEach((k, v) -> tplData.put(k, Map.of("value", v == null ? "" : v)));
        body.put("data", tplData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = json.readTree(resp.body());
        if (node.path("errcode").asInt(0) != 0)
            throw new IllegalStateException("模板消息 errcode=" + node.path("errcode").asInt() + " " + node.path("errmsg").asText());
    }

    /** GET /cgi-bin/token，内存缓存，过期前 5 分钟刷新 */
    private synchronized String accessToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireAt) return accessToken;
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + props.getAppId() + "&secret=" + props.getAppSecret();
        HttpResponse<String> resp = http.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        JsonNode node = json.readTree(resp.body());
        if (!node.hasNonNull("access_token"))
            throw new IllegalStateException("获取 access_token 失败：" + resp.body());
        accessToken = node.get("access_token").asText();
        long expiresIn = node.path("expires_in").asLong(7200);
        tokenExpireAt = System.currentTimeMillis() + (expiresIn - 300) * 1000;
        return accessToken;
    }
}
