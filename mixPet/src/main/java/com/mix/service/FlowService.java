package com.mix.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mix.mapper.BillMapper;
import com.mix.mapper.FollowUpMapper;
import com.mix.mapper.ImageOrderMapper;
import com.mix.mapper.LabOrderMapper;
import com.mix.mapper.MedicalRecordMapper;
import com.mix.model.dto.AppointmentDto;
import com.mix.model.dto.AppointmentDto2;
import com.mix.model.dto.BillDto;
import com.mix.model.dto.DoctorFlowVo;
import com.mix.model.dto.FlowItem;
import com.mix.model.dto.FlowStage;
import com.mix.model.dto.FollowUpDto;
import com.mix.model.dto.ImageOrderDto;
import com.mix.model.dto.LabOrderDto;
import com.mix.model.dto.RecordDto;
import com.mix.model.vo.RecordQueryVo;
import com.mix.model.vo.StatOverviewVo;
import com.mix.util.UserUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 诊疗流（闭环状态机）脊柱服务。
 * <p>
 * 五个环节并非独立页面，而是同一位患者"就诊旅程"上的连续状态：
 * 待诊 → 接诊中 → 待结果 → 待结算 → 待随访。本服务从各业务表派生出当前快照，
 * 任一单据状态变化（如化验出结果、账单收款）都会让它自动流向下一环节——这是闭环的"骨"。
 * 单一接口返回全部环节，前端无需并发 6 个请求。
 */
@Service
public class FlowService {

    private final MedicalRecordMapper recordMapper;
    private final LabOrderMapper labMapper;
    private final ImageOrderMapper imageMapper;
    private final BillMapper billMapper;
    private final FollowUpMapper followMapper;
    private final IAppointmentService appointmentService;
    private final IStatisticsService statisticsService;

    public FlowService(MedicalRecordMapper recordMapper, LabOrderMapper labMapper,
                       ImageOrderMapper imageMapper, BillMapper billMapper,
                       FollowUpMapper followMapper, IAppointmentService appointmentService,
                       IStatisticsService statisticsService) {
        this.recordMapper = recordMapper;
        this.labMapper = labMapper;
        this.imageMapper = imageMapper;
        this.billMapper = billMapper;
        this.followMapper = followMapper;
        this.appointmentService = appointmentService;
        this.statisticsService = statisticsService;
    }

    /** 患者上下文随跳转带走：列表页据此预填搜索词，内联抽屉顶部展示「正在处理谁」，
     *  避免同一只宠物在结果页/收银台被反复搜索、甚至找错人。 */
    private static String withPatient(String path, String pet, String client, String idKey, Object id) {
        String sep = path.contains("?") ? "&" : "?";
        return path + sep + "seaName=" + u(pet) + "&petName=" + u(pet) + "&clientName=" + u(client)
                + (idKey != null && id != null ? "&" + idKey + "=" + id : "");
    }

    private static String u(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    public DoctorFlowVo doctorPipeline() {
        LocalDate today = LocalDate.now();
        List<FlowStage> stages = new ArrayList<>();

        // 1) 待诊：当前医生今日已挂号、未接诊、未爽约
        FlowStage wait = new FlowStage("wait", "待诊", "今日已挂号·未接诊", "/doctorPage/d-appoint", "去接诊");
        Integer doctorId = currentDoctorId();
        if (doctorId != null) {
            for (AppointmentDto2 a : appointmentService.getDoctorLog(doctorId)) {
                if (a.getAppointmentDate() != null && a.getAppointmentDate().toLocalDate().equals(today)
                        && !Boolean.TRUE.equals(a.getIsSuc()) && !Boolean.TRUE.equals(a.getMiss())) {
                    wait.add(new FlowItem("a" + a.getAppointmentId(), a.getPetName(),
                            a.getClientName() + " · " + a.getAppointmentDate().format(DateTimeFormatter.ofPattern("HH:mm")),
                            null, null, "wait", "接诊",
                            withPatient("/doctorPage/d-appoint?appointmentId=" + a.getAppointmentId(),
                                    a.getPetName(), a.getClientName(), null, null), a.getClientName()));
                }
            }
        }

        // 2) 接诊中：病历状态=0（全诊所）
        FlowStage treat = new FlowStage("treat", "接诊中", "已开诊·未完结", "/doctorPage/d-appoint", "继续接诊");
        for (RecordDto r : recordMapper.selectRecordPage(new Page<>(1, 50),
                new RecordQueryVo(null, "", "", "", "", 0, "", "")).getRecords()) {
            treat.add(new FlowItem("r" + r.getRecordId(), r.getPetName(),
                    r.getClientName() + " · " + (r.getDiagnosis() == null ? "接诊中" : r.getDiagnosis()),
                    null, null, "treat", "继续接诊",
                    withPatient("/doctorPage/d-appoint?appointmentId=" + r.getAppointmentId(),
                            r.getPetName(), r.getClientName(), "recordId", r.getRecordId()), r.getClientName()));
        }

        // 3) 待结果：化验待检(0) + 影像待查(false)
        FlowStage result = new FlowStage("result", "待结果", "已开单·待回报", "/doctorPage/z-examResult", "去录入");
        for (LabOrderDto l : labMapper.selectLabOrderPage(new Page<>(1, 50), 0).getRecords()) {
            result.add(new FlowItem("l" + l.getOrderId(), l.getPetName(),
                    "化验 · 待回报",
                    Boolean.TRUE.equals(l.getCriticalFlag()) ? "危急" : null,
                    Boolean.TRUE.equals(l.getCriticalFlag()) ? "danger" : null,
                    "result", "录入化验结果",
                    withPatient("/doctorPage/z-examResult?tab=lab", l.getPetName(), l.getClientName(),
                            "orderId", l.getOrderId()), l.getClientName()));
        }
        for (ImageOrderDto im : imageMapper.selectImageOrderPage(new Page<>(1, 50), false).getRecords()) {
            result.add(new FlowItem("i" + im.getOrderId(), im.getPetName() == null ? im.getItemName() : im.getPetName(),
                    im.getItemName() + " · 待报告", null, null,
                    "result", "录入影像报告",
                    withPatient("/doctorPage/z-examResult?tab=image", im.getPetName(), im.getClientName(),
                            "orderId", im.getOrderId()), im.getClientName()));
        }

        // 4) 待结算：账单未收款(0)
        FlowStage pay = new FlowStage("pay", "待结算", "已完结·未收款", "/doctorPage/y-billList?payStatus=0", "去收银");
        for (BillDto b : billMapper.selectBillPage(new Page<>(1, 50), "", 0).getRecords()) {
            BigDecimal money = b.getTotalMoney();
            String moneyStr = money != null ? String.format("%.2f", money.doubleValue()) : "0.00";
            pay.add(new FlowItem("b" + b.getBillId(), b.getPetName(),
                    b.getClientName() + " · ￥" + moneyStr, null, null,
                    "pay", "收款结算",
                    withPatient("/doctorPage/y-billList?payStatus=0", b.getPetName(), b.getClientName(),
                            "billId", b.getBillId()), b.getClientName()));
        }

        // 5) 待随访：随访未完成(false)
        FlowStage follow = new FlowStage("follow", "待随访", "已结算·待回访", "/doctorPage/z-follow", "去随访");
        for (FollowUpDto f : followMapper.selectFollowPage(new Page<>(1, 50), false, "").getRecords()) {
            String due = f.getPlanTime() == null ? "" : f.getPlanTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            follow.add(new FlowItem("f" + f.getFollowId(), f.getPetName(),
                    f.getClientName() + " · 约" + due, null, null,
                    "follow", "回访",
                    withPatient("/doctorPage/z-follow", f.getPetName(), f.getClientName(),
                            "followId", f.getFollowId()), f.getClientName()));
        }

        stages.add(wait); stages.add(treat); stages.add(result); stages.add(pay); stages.add(follow);
        return new DoctorFlowVo(stages, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * 前台接待流：今日预约 → 待收银 → 今日已诊。
     * 权限由 FlowController.receptionPipeline 上的 @Secured(NURSE) 强制。
     */
    public DoctorFlowVo receptionPipeline() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");

        FlowStage wait = new FlowStage("appt", "今日预约", "全部预约·按到店引导", "/backstage/y-appointmentList", "去挂号");
        FlowStage done = new FlowStage("done", "今日已诊", "已接诊完成", "/backstage/y-appointmentList", "查看");
        for (AppointmentDto a : appointmentService.selectByPage("", 0, 1, 200).getRecords()) {
            if (a.getAppointmentDate() == null || !a.getAppointmentDate().toLocalDate().equals(today)) continue;
            String sub = a.getClientName() + " · " + a.getAppointmentDate().format(hm);
            if (Boolean.TRUE.equals(a.getIsSuc())) {
                done.add(new FlowItem("d" + a.getAppointmentId(), a.getPetName(),
                        a.getClientName() + " · 已完成", null, null,
                        "done", "复盘", "/backstage/y-appointmentList", a.getClientName()));
            } else {
                wait.add(new FlowItem("a" + a.getAppointmentId(), a.getPetName(), sub, null, null,
                        "appt", "办理/分诊", "/backstage/y-appointmentList", a.getClientName()));
            }
        }

        FlowStage pay = new FlowStage("pay", "待收银", "已完结·未收款", "/backstage/y-billList?payStatus=0", "去收银");
        for (BillDto b : billMapper.selectBillPage(new Page<>(1, 50), "", 0).getRecords()) {
            BigDecimal money = b.getTotalMoney();
            pay.add(new FlowItem("b" + b.getBillId(), b.getPetName(),
                    b.getClientName() + " · ￥" + (money == null ? "0.00" : String.format("%.2f", money.doubleValue())),
                    null, null, "pay", "收款", "/backstage/y-billList?payStatus=0", b.getClientName()));
        }

        List<FlowStage> stages = List.of(wait, pay, done);
        return new DoctorFlowVo(stages, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * 管理驾驶舱：把经营指标也表达成一条"流"（营收→挂号→结算→发药→医技→住院→疫苗→药品）。
     * 权限由 FlowController.managerPipeline 上的 @Secured(MANAGER) 强制。
     */
    public DoctorFlowVo managerPipeline() {
        StatOverviewVo s = statisticsService.selectOverview();
        List<FlowStage> stages = new ArrayList<>();
        stages.add(metric("rev", "今日营收", "营业收入", s.getTodayRevenue() != null ? 1 : 0,
                "￥" + money(s.getTodayRevenue()), "今日营业收入 · 月 " + money(s.getMonthRevenue()),
                "/backstage/z-report", null));
        stages.add(metric("appt", "今日挂号", "预约/到店", nvl(s.getTodayAppoint()),
                nvl(s.getTodayAppoint()) + " 笔", "今日预约 · 已接诊 " + nvl(s.getTodayRecord()),
                "/backstage/y-appointmentList", null));
        stages.add(metric("pay", "待结算", "未收款账单", nvl(s.getPendingBills()),
                nvl(s.getPendingBills()) + " 笔", "未收款账单", "/backstage/y-billList?payStatus=0",
                nvl(s.getPendingBills()) > 0 ? "danger" : null));
        stages.add(metric("dis", "待发药", "已收款待发药", nvl(s.getPendingDispense()),
                nvl(s.getPendingDispense()) + " 笔", "待发药", "/backstage/y-billList", null));
        stages.add(metric("exam", "待检/待影像", "医技积压", nvl(s.getPendingLab()) + nvl(s.getPendingImage()),
                nvl(s.getPendingLab()) + "/" + nvl(s.getPendingImage()),
                "化验 " + nvl(s.getPendingLab()) + " · 影像 " + nvl(s.getPendingImage()),
                "/doctorPage/z-examResult", null));
        stages.add(metric("hosp", "住院中", "在院动物", nvl(s.getInHospital()),
                nvl(s.getInHospital()) + " 例", "在院动物", "/backstage/z-hospital", null));
        stages.add(metric("vac", "到期疫苗", "待提醒", nvl(s.getDueVaccine()),
                nvl(s.getDueVaccine()) + " 只", "到期/逾期免疫", "/doctorPage/z-immune", null));
        long drug = nvl(s.getLowStock()) + nvl(s.getExpiringDrug());
        stages.add(metric("drug", "药品预警", "低库存/近效期", drug,
                "低 " + nvl(s.getLowStock()) + " · 近效 " + nvl(s.getExpiringDrug()),
                "低库存 " + nvl(s.getLowStock()) + " · 近效期 " + nvl(s.getExpiringDrug()),
                "/backstage/y-drugList", drug > 0 ? "warn" : null));
        return new DoctorFlowVo(stages, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /** 经营指标环节：单卡片承载数值，count 用于角标 */
    private FlowStage metric(String key, String title, String desc, long count,
                             String chip, String sub, String path, String cls) {
        FlowStage st = new FlowStage(key, title, desc, path, "查看");
        st.add(new FlowItem(key, chip, sub, null, cls, title, "查看", path, null));
        st.count = (int) count;
        return st;
    }

    private long nvl(Long v) { return v == null ? 0 : v; }

    private String money(BigDecimal v) {
        return "￥" + (v == null ? "0.00" : String.format("%.2f", v.doubleValue()));
    }

    private Integer currentDoctorId() {
        Object u = UserUtil.getUser();
        if (u instanceof com.mix.model.entity.Doctor d) return d.getDoctorId();
        return null;
    }
}
