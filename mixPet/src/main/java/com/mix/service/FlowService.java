package com.mix.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mix.mapper.BillMapper;
import com.mix.mapper.FollowUpMapper;
import com.mix.mapper.ImageOrderMapper;
import com.mix.mapper.LabOrderMapper;
import com.mix.mapper.MedicalRecordMapper;
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
import com.mix.util.UserUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public FlowService(MedicalRecordMapper recordMapper, LabOrderMapper labMapper,
                       ImageOrderMapper imageMapper, BillMapper billMapper,
                       FollowUpMapper followMapper, IAppointmentService appointmentService) {
        this.recordMapper = recordMapper;
        this.labMapper = labMapper;
        this.imageMapper = imageMapper;
        this.billMapper = billMapper;
        this.followMapper = followMapper;
        this.appointmentService = appointmentService;
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
                            "/doctorPage/d-appoint?appointmentId=" + a.getAppointmentId(), a.getClientName()));
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
                    "/doctorPage/d-appoint?appointmentId=" + r.getAppointmentId(), r.getClientName()));
        }

        // 3) 待结果：化验待检(0) + 影像待查(false)
        FlowStage result = new FlowStage("result", "待结果", "已开单·待回报", "/doctorPage/z-examResult", "去录入");
        for (LabOrderDto l : labMapper.selectLabOrderPage(new Page<>(1, 50), 0).getRecords()) {
            result.add(new FlowItem("l" + l.getOrderId(), l.getPetName(),
                    "化验 · 待回报",
                    Boolean.TRUE.equals(l.getCriticalFlag()) ? "危急" : null,
                    Boolean.TRUE.equals(l.getCriticalFlag()) ? "danger" : null,
                    "result", "录入化验结果", "/doctorPage/z-examResult?tab=lab", l.getClientName()));
        }
        for (ImageOrderDto im : imageMapper.selectImageOrderPage(new Page<>(1, 50), false).getRecords()) {
            result.add(new FlowItem("i" + im.getOrderId(), im.getPetName() == null ? im.getItemName() : im.getPetName(),
                    im.getItemName() + " · 待报告", null, null,
                    "result", "录入影像报告", "/doctorPage/z-examResult?tab=image", im.getClientName()));
        }

        // 4) 待结算：账单未收款(0)
        FlowStage pay = new FlowStage("pay", "待结算", "已完结·未收款", "/doctorPage/y-billList?payStatus=0", "去收银");
        for (BillDto b : billMapper.selectBillPage(new Page<>(1, 50), "", 0).getRecords()) {
            BigDecimal money = b.getTotalMoney();
            String moneyStr = money != null ? String.format("%.2f", money.doubleValue()) : "0.00";
            pay.add(new FlowItem("b" + b.getBillId(), b.getPetName(),
                    b.getClientName() + " · ￥" + moneyStr, null, null,
                    "pay", "收款结算", "/doctorPage/y-billList?payStatus=0", b.getClientName()));
        }

        // 5) 待随访：随访未完成(false)
        FlowStage follow = new FlowStage("follow", "待随访", "已结算·待回访", "/doctorPage/z-follow", "去随访");
        for (FollowUpDto f : followMapper.selectFollowPage(new Page<>(1, 50), false, "").getRecords()) {
            String due = f.getPlanTime() == null ? "" : f.getPlanTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            follow.add(new FlowItem("f" + f.getFollowId(), f.getPetName(),
                    f.getClientName() + " · 约" + due, null, null,
                    "follow", "回访", "/doctorPage/z-follow", f.getClientName()));
        }

        stages.add(wait); stages.add(treat); stages.add(result); stages.add(pay); stages.add(follow);
        return new DoctorFlowVo(stages, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private Integer currentDoctorId() {
        Object u = UserUtil.getUser();
        if (u instanceof com.mix.model.entity.Doctor d) return d.getDoctorId();
        return null;
    }
}
