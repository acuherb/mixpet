package com.mix.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.common.BizException;
import com.mix.common.LabResultEvaluator;
import com.mix.mapper.BillMapper;
import com.mix.mapper.LabRefRangeMapper;
import com.mix.mapper.LabResultRevisionMapper;
import com.mix.mapper.DoctorMapper;
import com.mix.mapper.LabOrderItemMapper;
import com.mix.mapper.LabOrderMapper;
import com.mix.mapper.LabTestMapper;
import com.mix.mapper.PetMapper;
import com.mix.model.dto.LabOrderDto;
import com.mix.model.entity.*;
import com.mix.model.vo.LabOrderVo;
import com.mix.model.vo.LabResultVo;
import com.mix.model.vo.RequisitionVo;
import com.mix.service.IClientNotificationService;
import com.mix.service.ILabOrderService;
import com.mix.service.IMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 化验单 Service 实现
 */
@Service
public class LabOrderServiceImpl extends ServiceImpl<LabOrderMapper, LabOrder> implements ILabOrderService {
    private final IMedicalRecordService recordService;
    private final LabTestMapper labTestMapper;
    private final LabOrderItemMapper labOrderItemMapper;
    private final BillMapper billMapper;
    private final PetMapper petMapper;
    private final DoctorMapper doctorMapper;
    private final LabRefRangeMapper labRefRangeMapper;
    private final LabResultRevisionMapper labResultRevisionMapper;
    private final IClientNotificationService notificationService;
    private static final ObjectMapper SNAPSHOT = new ObjectMapper();

    @Autowired
    public LabOrderServiceImpl(IMedicalRecordService recordService, LabTestMapper labTestMapper,
                               LabOrderItemMapper labOrderItemMapper, BillMapper billMapper,
                               PetMapper petMapper, DoctorMapper doctorMapper,
                               LabRefRangeMapper labRefRangeMapper,
                               LabResultRevisionMapper labResultRevisionMapper,
                               IClientNotificationService notificationService) {
        this.recordService = recordService;
        this.labTestMapper = labTestMapper;
        this.labOrderItemMapper = labOrderItemMapper;
        this.billMapper = billMapper;
        this.petMapper = petMapper;
        this.doctorMapper = doctorMapper;
        this.labRefRangeMapper = labRefRangeMapper;
        this.labResultRevisionMapper = labResultRevisionMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(LabOrderVo vo, Doctor doctor) {
        if (vo == null || vo.getRecordId() == null) throw new BizException("缺少病历信息");
        MedicalRecord record = recordService.getById(vo.getRecordId());
        if (record == null) throw new BizException("病历不存在");
        if (vo.getTestIds() == null || vo.getTestIds().isEmpty()) throw new BizException("请至少选择一项检验");
        // 校验检验项并累计费用（价格以字典为准）
        BigDecimal fee = BigDecimal.ZERO;
        List<LabOrderItem> items = new ArrayList<>();
        // 参考区间按宠物物种/性别/月龄分层匹配，命中结果冗余进明细快照
        Pet pet = petMapper.selectById(record.getPetId());
        for (Integer testId : vo.getTestIds()) {
            LabTest test = labTestMapper.selectById(testId);
            if (test == null || Boolean.FALSE.equals(test.getActive()))
                throw new BizException("检验项不存在或已停用");
            LabOrderItem item = new LabOrderItem();
            item.setTestId(test.getTestId());
            item.setItemName(test.getTestName());
            item.setItemGroup(test.getTestGroup());
            item.setItemUnit(test.getTestUnit());
            item.setRefRange(test.getRefRange());
            LabRefRange range = findRange(test.getTestId(), pet);
            if (range != null) {
                item.setRefLow(range.getRefLow());
                item.setRefHigh(range.getRefHigh());
                item.setCriticalLow(range.getCriticalLow());
                item.setCriticalHigh(range.getCriticalHigh());
                if (range.getRefText() != null && !range.getRefText().isBlank())
                    item.setRefRange(range.getRefText());
            }
            items.add(item);
            if (test.getTestPrice() != null) fee = fee.add(test.getTestPrice());
        }
        // 归属以病历为准，不信任前端
        LabOrder order = new LabOrder();
        order.setRecordId(record.getRecordId());
        order.setPetId(record.getPetId());
        order.setClientId(record.getClientId());
        order.setDoctorId(doctor.getDoctorId());
        order.setOrderNote(vo.getOrderNote());
        order.setFee(fee);
        order.setStatus(false);
        this.save(order);
        for (LabOrderItem item : items) {
            item.setOrderId(order.getOrderId());
            labOrderItemMapper.insert(item);
        }
        // 计费：优先累加进还未支付的账单，否则单独生成一张化验账单
        Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, record.getRecordId())
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
        if (unpaid != null) {
            billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                    .eq(Bill::getBillId, unpaid.getBillId())
                    .setSql("fee_lab = fee_lab + " + fee.toPlainString())
                    .setSql("total_money = total_money + " + fee.toPlainString()));
        } else {
            Bill bill = new Bill();
            bill.setClientId(record.getClientId());
            bill.setPetId(record.getPetId());
            bill.setRecordId(record.getRecordId());
            bill.setFeeRegister(BigDecimal.ZERO);
            bill.setFeeTreat(BigDecimal.ZERO);
            bill.setFeeDrug(BigDecimal.ZERO);
            bill.setFeeLab(fee);
            bill.setFeeHospital(BigDecimal.ZERO);
            bill.setFeePackage(BigDecimal.ZERO);
            bill.setFeeSurgery(BigDecimal.ZERO);
            bill.setTotalMoney(fee);
            bill.setPayStatus(false);
            bill.setPayType(false);
            billMapper.insert(bill);
        }
        return Result.success("开单成功，化验费已计入账单（￥" + fee.toPlainString() + "）");
    }

    @Override
    public IPage<LabOrderDto> selectPage(Integer status, int numPage, int pageSize) {
        return baseMapper.selectLabOrderPage(new Page<>(numPage, pageSize), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveResult(LabResultVo vo, String operator) {
        if (vo == null || vo.getOrderId() == null) throw new BizException("缺少化验单信息");
        LabOrder order = this.getById(vo.getOrderId());
        if (order == null) throw new BizException("化验单不存在");
        boolean revising = Boolean.TRUE.equals(order.getStatus());
        if (revising) {
            if (vo.getReviseReason() == null || vo.getReviseReason().isBlank())
                throw new BizException("结果已出具，修订必须填写修订原因");
            if (order.getReviewBy() != null)
                throw new BizException("该报告已复核，不可直接修订");
            writeResultRevision(order, operator, vo.getReviseReason());
        }
        boolean anyCritical = false;
        if (vo.getItems() != null) {
            for (LabResultVo.LabResultItemVo r : vo.getItems()) {
                if (r == null || r.getItemId() == null) continue;
                LabOrderItem item = labOrderItemMapper.selectById(r.getItemId());
                // 明细必须属于本单，防止前端拿别单的 itemId 篡改
                if (item == null || !vo.getOrderId().equals(item.getOrderId())) continue;
                item.setResultValue(r.getResultValue());
                item.setIsAbnormal(r.getIsAbnormal());
                // 异常/危急一律由服务端按参考区间复算，不信任前端传值
                LabResultEvaluator.Verdict verdict = LabResultEvaluator.evaluate(item);
                labOrderItemMapper.update(null, Wrappers.<LabOrderItem>lambdaUpdate()
                        .eq(LabOrderItem::getItemId, r.getItemId())
                        .set(LabOrderItem::getResultValue, r.getResultValue())
                        .set(LabOrderItem::getIsAbnormal, verdict.abnormal())
                        .set(LabOrderItem::getCriticalFlag, verdict.critical())
                        .set(LabOrderItem::getResultTime, LocalDateTime.now()));
                anyCritical |= verdict.critical();
            }
        }
        int revision = revising ? (order.getResultRevision() == null ? 1 : order.getResultRevision() + 1) : 0;
        this.lambdaUpdate()
                .eq(LabOrder::getOrderId, vo.getOrderId())
                .set(LabOrder::getStatus, true)
                .set(LabOrder::getResultNote, vo.getResultNote())
                .set(LabOrder::getResultBy, operator)
                .set(LabOrder::getReportFile, vo.getReportFile())
                .set(LabOrder::getSpecimenType, vo.getSpecimenType())
                .set(LabOrder::getCollectTime, vo.getCollectTime())
                .set(LabOrder::getReceiveTime, vo.getReceiveTime())
                .set(LabOrder::getCriticalFlag, anyCritical)
                .set(LabOrder::getResultRevision, revision)
                // 录入/修订后复核失效，必须重新双签
                .set(LabOrder::getReviewBy, null)
                .set(LabOrder::getReviewAt, null)
                .update();
        // 报告出具/修订后主动告诉顾客，不用等他自己刷新档案
        notificationService.notifyReport(order.getClientId(),
                revising ? "检验报告有更新" : "检验报告已出具",
                "就诊号 #" + order.getRecordId() + " 的检验报告" + (revising ? "已修订" : "已出具")
                        + (anyCritical ? "，存在危急值，请尽快联系接诊医生" : "，可在「宠物信息」中查看完整结果"),
                "REPORT_LAB", "lab_order", order.getOrderId());
        String tip = anyCritical ? "（含危急值，请立即通知接诊医生）" : "";
        return Result.success((revising ? "检验结果已修订" : "检验结果已保存") + tip);
    }

    @Override
    public Result reviewResult(Integer orderId, String operator) {
        LabOrder order = this.getById(orderId);
        if (order == null) throw new BizException("化验单不存在");
        if (!Boolean.TRUE.equals(order.getStatus())) throw new BizException("尚未出具结果，无法复核");
        if (order.getReviewBy() != null) throw new BizException("该报告已复核，无需重复操作");
        this.lambdaUpdate().eq(LabOrder::getOrderId, orderId)
                .set(LabOrder::getReviewBy, operator)
                .set(LabOrder::getReviewAt, LocalDateTime.now())
                .update();
        return Result.success("报告已复核（审核人：" + operator + "）");
    }

    /** 结果修订前保存整单快照（单头 + 全部明细），可追溯到任意一版 */
    private void writeResultRevision(LabOrder order, String operator, String reason) {
        LabResultRevision rev = new LabResultRevision();
        rev.setOrderId(order.getOrderId());
        rev.setRevisionNo(order.getResultRevision() == null ? 0 : order.getResultRevision());
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("order", order);
        snapshot.put("items", labOrderItemMapper.selectList(Wrappers.<LabOrderItem>lambdaQuery()
                .eq(LabOrderItem::getOrderId, order.getOrderId())));
        try {
            rev.setSnapshotJson(SNAPSHOT.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            rev.setSnapshotJson("{}");
        }
        rev.setOperator(operator);
        rev.setChangeReason(reason);
        labResultRevisionMapper.insert(rev);
    }

    /** 按物种/性别/月龄匹配最具体的参考区间；无匹配时回退字典里的通用文本区间 */
    private LabRefRange findRange(Integer testId, Pet pet) {
        List<LabRefRange> all = labRefRangeMapper.selectList(Wrappers.<LabRefRange>lambdaQuery()
                .eq(LabRefRange::getTestId, testId)
                .eq(LabRefRange::getActive, true));
        if (all.isEmpty()) return null;
        String species = speciesOf(pet);
        String sex = pet == null || pet.getPetSex() == null ? null : (pet.getPetSex() ? "公" : "母");
        Integer months = pet == null || pet.getPetBirthday() == null ? null
                : (int) ChronoUnit.MONTHS.between(pet.getPetBirthday(), LocalDate.now());
        return all.stream()
                .filter(r -> fits(r, species, sex, months))
                .max(Comparator.comparingInt(this::specificity))
                .orElse(null);
    }

    private boolean fits(LabRefRange r, String species, String sex, Integer months) {
        if (r.getSpecies() != null && !r.getSpecies().isBlank() && !r.getSpecies().equals(species)) return false;
        if (r.getSex() != null && !r.getSex().isBlank() && !r.getSex().equals(sex)) return false;
        if (r.getAgeMinMonth() != null && (months == null || months < r.getAgeMinMonth())) return false;
        if (r.getAgeMaxMonth() != null && (months == null || months > r.getAgeMaxMonth())) return false;
        return true;
    }

    /** 越具体的规则优先级越高：物种 > 性别 > 月龄 */
    private int specificity(LabRefRange r) {
        int score = 0;
        if (r.getSpecies() != null && !r.getSpecies().isBlank()) score += 4;
        if (r.getSex() != null && !r.getSex().isBlank()) score += 2;
        if (r.getAgeMinMonth() != null || r.getAgeMaxMonth() != null) score += 1;
        return score;
    }

    /** pet_variety 常写「金毛/犬」「英短/猫」，归一到犬/猫；其余按原文匹配 */
    private String speciesOf(Pet pet) {
        String v = pet == null ? null : pet.getPetVariety();
        if (v == null) return null;
        if (v.contains("犬") || v.contains("狗")) return "犬";
        if (v.contains("猫")) return "猫";
        return v;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancel(Integer orderId) {
        LabOrder order = this.getById(orderId);
        if (order == null) throw new BizException("化验单不存在");
        if (Boolean.TRUE.equals(order.getIsCancel())) throw new BizException("该化验单已作废");
        if (Boolean.TRUE.equals(order.getStatus())) throw new BizException("已出结果的化验单不可作废");
        Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, order.getRecordId())
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
        if (unpaid == null) throw new BizException("未找到可冲减的未付账单（可能已收款，请走退款流程）");
        billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                .eq(Bill::getBillId, unpaid.getBillId())
                .setSql("fee_lab = fee_lab - " + order.getFee().toPlainString())
                .setSql("total_money = total_money - " + order.getFee().toPlainString()));
        this.lambdaUpdate().eq(LabOrder::getOrderId, orderId).set(LabOrder::getIsCancel, true).update();
        return Result.success("化验单已作废，费用已冲减");
    }

    @Override
    public List<LabOrderDto> selectByRecord(Integer recordId) {
        List<LabOrder> orders = this.lambdaQuery()
                .eq(LabOrder::getRecordId, recordId)
                .orderByDesc(LabOrder::getCreateTime)
                .list();
        List<LabOrderDto> result = new ArrayList<>();
        for (LabOrder o : orders) {
            LabOrderDto dto = new LabOrderDto();
            dto.setOrderId(o.getOrderId());
            dto.setRecordId(o.getRecordId());
            dto.setPetId(o.getPetId());
            dto.setClientId(o.getClientId());
            dto.setDoctorId(o.getDoctorId());
            dto.setOrderNote(o.getOrderNote());
            dto.setFee(o.getFee());
            dto.setStatus(o.getStatus());
            dto.setResultNote(o.getResultNote());
            dto.setResultBy(o.getResultBy());
            dto.setReportFile(o.getReportFile());
            dto.setCreateTime(o.getCreateTime());
            dto.setSpecimenType(o.getSpecimenType());
            dto.setCollectTime(o.getCollectTime());
            dto.setReceiveTime(o.getReceiveTime());
            dto.setReviewBy(o.getReviewBy());
            dto.setReviewAt(o.getReviewAt());
            dto.setCriticalFlag(o.getCriticalFlag());
            dto.setResultRevision(o.getResultRevision());
            dto.setItems(labOrderItemMapper.selectList(
                    Wrappers.<LabOrderItem>lambdaQuery().eq(LabOrderItem::getOrderId, o.getOrderId())));
            result.add(dto);
        }
        return result;
    }

    @Override
    public RequisitionVo selectRequisition(Integer orderId) {
        LabOrder order = this.getById(orderId);
        if (order == null) return null;
        RequisitionVo vo = new RequisitionVo();
        vo.setType("检验申请单");
        vo.setOrderId(orderId);
        vo.setOrderNo("No." + orderId);
        vo.setCreateTime(order.getCreateTime());
        vo.setFee(order.getFee());
        vo.setRemark(order.getOrderNote());
        LabOrderDto dto = selectByRecord(order.getRecordId()).stream()
                .filter(d -> d.getOrderId().equals(orderId)).findFirst().orElse(null);
        if (dto != null) {
            vo.setClientName(dto.getClientName());
            vo.setPetName(dto.getPetName());
            vo.setDoctorName(dto.getDoctorName());
            List<RequisitionVo.ReqItem> items = new ArrayList<>();
            if (dto.getItems() != null) {
                for (LabOrderItem it : dto.getItems()) {
                    RequisitionVo.ReqItem r = new RequisitionVo.ReqItem();
                    r.setName(it.getItemName());
                    r.setGroup(it.getItemGroup());
                    r.setRefRange(it.getRefRange());
                    r.setUnit(it.getItemUnit());
                    items.add(r);
                }
            }
            vo.setItems(items);
        }
        fillHeader(vo, order.getRecordId(), order.getPetId(), order.getDoctorId());
        return vo;
    }

    /** 补头部：诊断 + 宠物详情 + 医生资格证号 */
    private void fillHeader(RequisitionVo vo, Integer recordId, Integer petId, Integer doctorId) {
        MedicalRecord record = recordService.getById(recordId);
        if (record != null) vo.setDiagnosis(record.getDiagnosis());
        Pet pet = petMapper.selectById(petId);
        if (pet != null) {
            vo.setPetVariety(pet.getPetVariety());
            vo.setPetSex(pet.getPetSex());
            vo.setPetBirthday(pet.getPetBirthday());
            vo.setPetWeight(pet.getPetWeight());
            vo.setPetChip(pet.getPetChip());
        }
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor != null) vo.setDoctorCert(doctor.getDoctorCert());
    }
}