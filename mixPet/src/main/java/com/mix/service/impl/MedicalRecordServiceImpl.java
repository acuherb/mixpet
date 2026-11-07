package com.mix.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.common.BizException;
import com.mix.mapper.BillMapper;
import com.mix.mapper.ClientMapper;
import com.mix.mapper.DoctorMapper;
import com.mix.mapper.DrugMapper;
import com.mix.mapper.FollowUpMapper;
import com.mix.mapper.HospitalMapper;
import com.mix.mapper.ImmuneRecordMapper;
import com.mix.mapper.ImageOrderMapper;
import com.mix.mapper.ImageTestMapper;
import com.mix.mapper.LabOrderItemMapper;
import com.mix.mapper.LabOrderMapper;
import com.mix.mapper.LabTestMapper;
import com.mix.mapper.MedicalRecordMapper;
import com.mix.mapper.MedicalRecordRevisionMapper;
import com.mix.mapper.PetMapper;
import com.mix.mapper.PrescriptionMapper;
import com.mix.mapper.RecordPackageMapper;
import com.mix.mapper.RxPrintSupplementMapper;
import com.mix.mapper.SurgeryMapper;
import com.mix.model.dto.RecordDto;
import com.mix.model.entity.*;
import com.mix.model.vo.PetHistoryLab;
import com.mix.model.vo.PetHistoryRecord;
import com.mix.model.vo.PetHistoryRx;
import com.mix.model.vo.PetHistoryVo;
import com.mix.model.vo.RecordFullVo;
import com.mix.model.vo.RecordQueryVo;
import com.mix.model.vo.RecordTodoVo;
import com.mix.model.vo.RecordVo;
import com.mix.model.vo.RxSheetVo;
import com.mix.model.vo.SurgeryVo;
import com.mix.service.IAppointmentService;
import com.mix.service.IMedicalRecordService;
import com.mix.service.IMemberService;
import com.mix.service.IServicePackageService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import com.mix.util.StrUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * 电子病历 Service 实现
 */
@Service
public class MedicalRecordServiceImpl extends ServiceImpl<MedicalRecordMapper, MedicalRecord> implements IMedicalRecordService {
    private final IAppointmentService appointmentService;
    private final DrugMapper drugMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final BillMapper billMapper;
    private final RxPrintSupplementMapper rxPrintSupplementMapper;
    private final PetMapper petMapper;
    private final DoctorMapper doctorMapper;
    private final IServicePackageService packageService;
    private final RecordPackageMapper recordPackageMapper;
    private final LabTestMapper labTestMapper;
    private final LabOrderMapper labOrderMapper;
    private final LabOrderItemMapper labOrderItemMapper;
    private final ImageTestMapper imageTestMapper;
    private final ImageOrderMapper imageOrderMapper;
    private final SurgeryMapper surgeryMapper;
    private final IMemberService memberService;
    private final FollowUpMapper followUpMapper;
    private final HospitalMapper hospitalMapper;
    private final ImmuneRecordMapper immuneRecordMapper;
    private final ClientMapper clientMapper;
    private final MedicalRecordRevisionMapper revisionMapper;
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper();

    @Autowired
    public MedicalRecordServiceImpl(IAppointmentService appointmentService, DrugMapper drugMapper,
                                    PrescriptionMapper prescriptionMapper, BillMapper billMapper,
                                    RxPrintSupplementMapper rxPrintSupplementMapper,
                                    PetMapper petMapper, DoctorMapper doctorMapper,
                                    IServicePackageService packageService, RecordPackageMapper recordPackageMapper,
                                    LabTestMapper labTestMapper, LabOrderMapper labOrderMapper,
                                    LabOrderItemMapper labOrderItemMapper, ImageTestMapper imageTestMapper,
                                    ImageOrderMapper imageOrderMapper, SurgeryMapper surgeryMapper,
                                    IMemberService memberService, FollowUpMapper followUpMapper,
                                    HospitalMapper hospitalMapper, ImmuneRecordMapper immuneRecordMapper,
                                    ClientMapper clientMapper, MedicalRecordRevisionMapper revisionMapper) {
        this.appointmentService = appointmentService;
        this.drugMapper = drugMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.billMapper = billMapper;
        this.rxPrintSupplementMapper = rxPrintSupplementMapper;
        this.petMapper = petMapper;
        this.doctorMapper = doctorMapper;
        this.packageService = packageService;
        this.recordPackageMapper = recordPackageMapper;
        this.labTestMapper = labTestMapper;
        this.labOrderMapper = labOrderMapper;
        this.labOrderItemMapper = labOrderItemMapper;
        this.imageTestMapper = imageTestMapper;
        this.imageOrderMapper = imageOrderMapper;
        this.surgeryMapper = surgeryMapper;
        this.memberService = memberService;
        this.followUpMapper = followUpMapper;
        this.hospitalMapper = hospitalMapper;
        this.immuneRecordMapper = immuneRecordMapper;
        this.clientMapper = clientMapper;
        this.revisionMapper = revisionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveRecord(RecordVo vo, Doctor doctor) {
        MedicalRecord record = vo == null ? null : vo.getRecord();
        if (record == null || record.getAppointmentId() == null) throw new BizException("缺少挂号单信息");
        // 校验挂号单：存在、归属本人、未接诊过
        Appointment appoint = appointmentService.getById(record.getAppointmentId());
        if (appoint == null) throw new BizException("挂号单不存在");
        if (!doctor.getDoctorId().equals(appoint.getDoctorId())) throw new BizException("只能接诊自己的挂号单");
        if (this.lambdaQuery().eq(MedicalRecord::getAppointmentId, appoint.getAppointmentId()).count() > 0)
            throw new BizException("该挂号单已接诊过，不能重复建档");
        // 归属信息以挂号单为准，不信任前端
        record.setRecordId(null);
        record.setPetId(appoint.getPetId());
        record.setClientId(appoint.getClientId());
        record.setDoctorId(doctor.getDoctorId());
        record.setDepartmentId(appoint.getDepartmentId());
        // 复诊链：若挂号单关联了初诊/上次数病历，写入新病历
        if (appoint.getOriginRecordId() != null) record.setOriginRecordId(appoint.getOriginRecordId());
        if (record.getFeeRegister() == null) record.setFeeRegister(BigDecimal.ZERO);
        if (record.getFeeTreat() == null) record.setFeeTreat(BigDecimal.ZERO);
        if (record.getFeeRegister().compareTo(BigDecimal.ZERO) < 0 || record.getFeeTreat().compareTo(BigDecimal.ZERO) < 0)
            throw new BizException("费用不能为负数");
        // 处方：校验药品与库存，价格等以药品库为准做快照
        BigDecimal feeDrug = BigDecimal.ZERO;
        List<Prescription> prescriptions = new ArrayList<>();
        if (vo.getPrescriptions() != null) {
            for (Prescription p : vo.getPrescriptions()) {
                if (p == null || p.getDrugId() == null || p.getQuantity() == null || p.getQuantity() <= 0) continue;
                Drug drug = drugMapper.selectById(p.getDrugId());
                if (drug == null || Boolean.FALSE.equals(drug.getActive()))
                    throw new BizException("药品不存在或已停用");
                if (drug.getDrugStock() == null || drug.getDrugStock() < p.getQuantity())
                    throw new BizException("「" + drug.getDrugName() + "」库存不足（剩余 " + drug.getDrugStock() + "）");
                p.setPrescriptionId(null);
                p.setRecordId(null);
                p.setDrugName(drug.getDrugName());
                p.setDrugSpec(drug.getDrugSpec());
                p.setDrugUnit(drug.getDrugUnit());
                p.setDrugPrice(drug.getDrugPrice());
                feeDrug = feeDrug.add(drug.getDrugPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
                prescriptions.add(p);
            }
        }
        // 新接诊默认接诊中；版本从 1 起，后续每次内容修改 +1
        record.setStatus(0);
        record.setVersion(1);
        record.setReviewStatus(0);
        this.save(record);
        for (Prescription p : prescriptions) {
            p.setRecordId(record.getRecordId());
            prescriptionMapper.insert(p);
        }
        // 套餐：校验 + 快照 + 记录 + 计费
        BigDecimal feePackage = BigDecimal.ZERO;
        if (vo.getPackages() != null) {
            for (ServicePackage pkg : vo.getPackages()) {
                if (pkg == null || pkg.getPackageId() == null) continue;
                ServicePackage real = packageService.getById(pkg.getPackageId());
                if (real == null || !Boolean.TRUE.equals(real.getActive()))
                    throw new BizException("套餐不存在或已停用");
                RecordPackage rp = new RecordPackage();
                rp.setRecordId(record.getRecordId());
                rp.setPackageId(real.getPackageId());
                rp.setPackageName(real.getPackageName());
                rp.setPrice(real.getPrice());
                recordPackageMapper.insert(rp);
                if (real.getPrice() != null) feePackage = feePackage.add(real.getPrice());
            }
        }
        // 一次性开的检验单：勾选检验项 → 建化验单主表+明细（快照）→ 计 fee_lab
        BigDecimal feeLab = BigDecimal.ZERO;
        if (vo.getLabTestIds() != null && !vo.getLabTestIds().isEmpty()) {
            LabOrder order = new LabOrder();
            order.setRecordId(record.getRecordId());
            order.setPetId(record.getPetId());
            order.setClientId(record.getClientId());
            order.setDoctorId(record.getDoctorId());
            order.setFee(BigDecimal.ZERO);
            order.setStatus(false);
            labOrderMapper.insert(order);
            for (Integer testId : vo.getLabTestIds()) {
                if (testId == null) continue;
                LabTest test = labTestMapper.selectById(testId);
                if (test == null || !Boolean.TRUE.equals(test.getActive()))
                    throw new BizException("检验项不存在或已停用");
                LabOrderItem item = new LabOrderItem();
                item.setOrderId(order.getOrderId());
                item.setTestId(test.getTestId());
                item.setItemName(test.getTestName());
                item.setItemGroup(test.getTestGroup());
                item.setItemUnit(test.getTestUnit());
                item.setRefRange(test.getRefRange());
                labOrderItemMapper.insert(item);
                if (test.getTestPrice() != null) feeLab = feeLab.add(test.getTestPrice());
            }
            labOrderMapper.update(null, Wrappers.<LabOrder>lambdaUpdate()
                    .eq(LabOrder::getOrderId, order.getOrderId())
                    .set(LabOrder::getFee, feeLab));
        }
        // 一次性开的影像申请单
        BigDecimal feeImage = BigDecimal.ZERO;
        if (vo.getImageItemId() != null) {
            ImageTest img = imageTestMapper.selectById(vo.getImageItemId());
            if (img == null) throw new BizException("影像项目不存在");
            ImageOrder imageOrder = new ImageOrder();
            imageOrder.setRecordId(record.getRecordId());
            imageOrder.setPetId(record.getPetId());
            imageOrder.setClientId(record.getClientId());
            imageOrder.setDoctorId(record.getDoctorId());
            imageOrder.setItemId(img.getItemId());
            imageOrder.setItemName(img.getItemName());
            imageOrder.setGroupName(img.getGroupName());
            imageOrder.setFee(img.getPrice());
            imageOrder.setExamPart(vo.getImageExamPart());
            imageOrder.setStatus(false);
            imageOrderMapper.insert(imageOrder);
            if (img.getPrice() != null) feeImage = feeImage.add(img.getPrice());
        }
        // 一次性提交的手术申请
        BigDecimal feeSurgery = BigDecimal.ZERO;
        if (vo.getSurgery() != null && vo.getSurgery().getRecordId() == null
                && vo.getSurgery().getSurgeryName() != null && !vo.getSurgery().getSurgeryName().isBlank()) {
            SurgeryVo sv = vo.getSurgery();
            BigDecimal sFee = sv.getFee() == null ? BigDecimal.ZERO : sv.getFee();
            if (sFee.compareTo(BigDecimal.ZERO) < 0) throw new BizException("手术费不能为负数");
            Surgery surgery = new Surgery();
            surgery.setRecordId(record.getRecordId());
            surgery.setPetId(record.getPetId());
            surgery.setClientId(record.getClientId());
            surgery.setDoctorId(record.getDoctorId());
            surgery.setSurgeryName(sv.getSurgeryName());
            surgery.setSurgeryType(sv.getSurgeryType());
            surgery.setAnesthesiaType(sv.getAnesthesiaType());
            surgery.setScheduledTime(sv.getScheduledTime());
            surgery.setSurgeon(sv.getSurgeon());
            surgery.setAssistant(sv.getAssistant());
            surgery.setFee(sFee);
            surgery.setStatus(0);
            surgeryMapper.insert(surgery);
            feeSurgery = sFee;
        }
        // 生成未付账单
        Bill bill = new Bill();
        bill.setClientId(record.getClientId());
        bill.setPetId(record.getPetId());
        bill.setRecordId(record.getRecordId());
        bill.setFeeRegister(record.getFeeRegister());
        bill.setFeeTreat(record.getFeeTreat());
        bill.setFeeDrug(feeDrug);
        bill.setFeePackage(feePackage);
        bill.setFeeLab(feeLab);
        bill.setFeeImage(feeImage);
        bill.setFeeSurgery(feeSurgery);
        bill.setTotalMoney(record.getFeeRegister().add(record.getFeeTreat())
                .add(feeDrug).add(feePackage).add(feeLab).add(feeImage).add(feeSurgery));
        // 会员折扣：按客户等级快照折扣率（discount_money/real_money 为数据库生成列，随 total_money 自动折算）
        bill.setDiscount(memberService.discountOf(record.getClientId()));
        bill.setPayStatus(false);
        bill.setPayType(false);
        billMapper.insert(bill);
        // 挂号单标记完成
        appointmentService.lambdaUpdate()
                .eq(Appointment::getAppointmentId, appoint.getAppointmentId())
                .set(Appointment::getIsSuc, true)
                .update();
        // 完整性软提示：缺项拼进 msg 提醒医生（不阻断保存，前端保存成功提示即展示）
        List<String> lacks = new ArrayList<>();
        if (StrUtil.isWhite(record.getChiefComplaint())) lacks.add("主诉");
        if (StrUtil.isWhite(record.getPresentIllness())) lacks.add("现病史");
        if (StrUtil.isWhite(record.getPastHistory())) lacks.add("既往病史");
        if (StrUtil.isWhite(record.getPhysicalExam())) lacks.add("体格检查");
        if (StrUtil.isWhite(record.getDiagnosis())) lacks.add("诊断");
        if (record.getWeight() == null) lacks.add("体重(体征)");
        return Result.success(lacks.isEmpty()
                ? "接诊完成，已生成收费账单"
                : "接诊完成，已生成收费账单。提醒：" + String.join("、", lacks) + " 尚未填写");
    }

    @Override
    public IPage<RecordDto> selectPetRecords(Integer petId, int numPage, int pageSize) {
        return baseMapper.selectPetRecords(new Page<>(numPage, pageSize), petId);
    }

    @Override
    public RecordDto selectRecordOne(Integer recordId) {
        RecordDto dto = baseMapper.selectRecordOne(recordId);
        // 复诊链：回填初诊/上次就诊的诊断，便于追溯
        if (dto != null && dto.getOriginRecordId() != null) {
            MedicalRecord origin = this.getById(dto.getOriginRecordId());
            if (origin != null) dto.setOriginDiagnosis(origin.getDiagnosis());
        }
        return dto;
    }

    @Override
    public List<RecordDto> selectRevisits(Integer recordId) {
        List<MedicalRecord> revisits = this.lambdaQuery()
                .eq(MedicalRecord::getOriginRecordId, recordId)
                .orderByAsc(MedicalRecord::getCreateTime)
                .list();
        List<RecordDto> result = new ArrayList<>();
        for (MedicalRecord r : revisits) {
            RecordDto dto = baseMapper.selectRecordOne(r.getRecordId());
            if (dto != null) result.add(dto);
        }
        return result;
    }

    @Override
    public RecordDto selectByAppointment(Integer appointmentId) {
        MedicalRecord r = this.lambdaQuery().eq(MedicalRecord::getAppointmentId, appointmentId).one();
        return r == null ? null : baseMapper.selectRecordOne(r.getRecordId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelPrescription(Integer prescriptionId) {
        Prescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null) throw new BizException("处方不存在");
        if (Boolean.TRUE.equals(p.getIsCancel())) throw new BizException("该处方已作废");
        MedicalRecord owner = this.getById(p.getRecordId());
        if (owner == null) throw new BizException("病历不存在");
        assertEditable(owner);
        Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, p.getRecordId())
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
        if (unpaid == null) throw new BizException("未找到可冲减的未付账单（可能已收款，请走退款流程）");
        // 先抢占作废权：仅「未作废」的行能被更新（affected=0 说明并发下已被他人作废，不再冲减账单）
        int affected = prescriptionMapper.update(null, Wrappers.<Prescription>lambdaUpdate()
                .eq(Prescription::getPrescriptionId, prescriptionId)
                .eq(Prescription::getIsCancel, false)
                .set(Prescription::getIsCancel, true));
        if (affected == 0) throw new BizException("该处方已作废");
        BigDecimal amount = (p.getDrugPrice() == null ? BigDecimal.ZERO : p.getDrugPrice())
                .multiply(BigDecimal.valueOf(p.getQuantity() == null ? 0 : p.getQuantity()));
        billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                .eq(Bill::getBillId, unpaid.getBillId())
                .setSql("fee_drug = fee_drug - " + amount.toPlainString())
                .setSql("total_money = total_money - " + amount.toPlainString()));
        return Result.success("处方已作废，费用已冲减");
    }

    /** 某病历的未付款账单（追加处方/套餐用） */
    private Bill unpaidBill(Integer recordId) {
        return billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, recordId)
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addPrescriptions(Integer recordId, List<Prescription> list) {
        MedicalRecord record = this.getById(recordId);
        if (record == null) throw new BizException("病历不存在");
        assertEditable(record);
        if (list == null || list.isEmpty()) throw new BizException("请至少添加一种药品");
        Bill bill = unpaidBill(recordId);
        if (bill == null) throw new BizException("该病历账单已收款或不存在，无法追加（如需追加请先退款）");
        BigDecimal add = BigDecimal.ZERO;
        for (Prescription p : list) {
            if (p == null || p.getDrugId() == null || p.getQuantity() == null || p.getQuantity() <= 0)
                throw new BizException("药品或数量不合法");
            Drug drug = drugMapper.selectById(p.getDrugId());
            if (drug == null || Boolean.FALSE.equals(drug.getActive())) throw new BizException("药品不存在或已停用");
            if (drug.getDrugStock() == null || drug.getDrugStock() < p.getQuantity())
                throw new BizException("「" + drug.getDrugName() + "」库存不足（剩余 " + drug.getDrugStock() + "）");
            p.setPrescriptionId(null);
            p.setRecordId(recordId);
            p.setDrugName(drug.getDrugName());
            p.setDrugSpec(drug.getDrugSpec());
            p.setDrugUnit(drug.getDrugUnit());
            p.setDrugPrice(drug.getDrugPrice());
            prescriptionMapper.insert(p);
            add = add.add(drug.getDrugPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
        }
        // 生成列 real_money/discount_money 会随 total_money 自动折算，无需手工维护
        billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                .eq(Bill::getBillId, bill.getBillId())
                .setSql("fee_drug = fee_drug + " + add.toPlainString())
                .setSql("total_money = total_money + " + add.toPlainString()));
        return Result.success("已追加处方，药费 +￥" + add.toPlainString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addPackages(Integer recordId, List<Integer> packageIds) {
        MedicalRecord record = this.getById(recordId);
        if (record == null) throw new BizException("病历不存在");
        assertEditable(record);
        if (packageIds == null || packageIds.isEmpty()) throw new BizException("请至少选择一个套餐");
        Bill bill = unpaidBill(recordId);
        if (bill == null) throw new BizException("该病历账单已收款或不存在，无法追加（如需追加请先退款）");
        BigDecimal add = BigDecimal.ZERO;
        for (Integer pid : packageIds) {
            if (pid == null) continue;
            ServicePackage real = packageService.getById(pid);
            if (real == null || !Boolean.TRUE.equals(real.getActive())) throw new BizException("套餐不存在或已停用");
            RecordPackage rp = new RecordPackage();
            rp.setRecordId(recordId);
            rp.setPackageId(real.getPackageId());
            rp.setPackageName(real.getPackageName());
            rp.setPrice(real.getPrice());
            recordPackageMapper.insert(rp);
            if (real.getPrice() != null) add = add.add(real.getPrice());
        }
        // 生成列 real_money/discount_money 会随 total_money 自动折算，无需手工维护
        billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                .eq(Bill::getBillId, bill.getBillId())
                .setSql("fee_package = fee_package + " + add.toPlainString())
                .setSql("total_money = total_money + " + add.toPlainString()));
        return Result.success("已追加套餐，套餐费 +￥" + add.toPlainString());
    }

    @Override
    public RxSheetVo selectRxSheet(Integer recordId) {
        RecordDto record = baseMapper.selectRecordOne(recordId);
        if (record == null) return null;
        RxSheetVo vo = new RxSheetVo();
        vo.setRecordId(record.getRecordId());
        vo.setRxNo("No." + record.getRecordId());
        vo.setCreateTime(record.getCreateTime());
        vo.setClientName(record.getClientName());
        vo.setPetName(record.getPetName());
        vo.setDiagnosis(record.getDiagnosis());
        vo.setDoctorName(record.getDoctorName());
        vo.setDepartmentName(record.getDepartmentName());
        // 宠物信息（品种/性别/生日/体重/芯片）
        Pet pet = petMapper.selectById(record.getPetId());
        if (pet != null) {
            vo.setPetVariety(pet.getPetVariety());
            vo.setPetSex(pet.getPetSex());
            vo.setPetBirthday(pet.getPetBirthday());
            vo.setPetWeight(pet.getPetWeight());
            vo.setPetChip(pet.getPetChip());
        }
        // 开方兽医执业资格证号
        Doctor doctor = doctorMapper.selectById(record.getDoctorId());
        if (doctor != null) vo.setDoctorCert(doctor.getDoctorCert());
        // 处方明细与药费合计
        List<Prescription> items = prescriptionMapper.selectByRecord(recordId);
        vo.setItems(items == null ? new ArrayList<>() : items);
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (Prescription p : items) {
                if (p.getDrugPrice() != null && p.getQuantity() != null) {
                    total = total.add(p.getDrugPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
                }
            }
        }
        vo.setTotalAmount(total);
        // 开具日期（R2）：处方行创建时间优先，回落病历创建时间（编号日期亦取此）
        LocalDateTime pTime = record.getCreateTime();
        if (items != null && !items.isEmpty() && items.get(0).getCreateTime() != null) {
            pTime = items.get(0).getCreateTime();
        }
        vo.setPrescriptionTime(pTime);
        // 处方笺打印补充（未打印则为 null，不阻断展示）
        if (items != null && !items.isEmpty()) {
            RxPrintSupplement sup = rxPrintSupplementMapper.selectOne(Wrappers.<RxPrintSupplement>lambdaQuery()
                    .eq(RxPrintSupplement::getPrescriptionId, items.get(0).getPrescriptionId())
                    .last("limit 1"));
            if (sup != null) {
                vo.setPrintNo(sup.getPrintNo());
                vo.setCopies(sup.getCopies());
                vo.setHairColor(sup.getHairColor());
                vo.setSpecies(sup.getSpecies());
                vo.setWithdrawalPeriod(sup.getWithdrawalPeriod());
                vo.setDispenser(sup.getDispenser());
                vo.setSupplementStatus(sup.getStatus());
            }
        }
        return vo;
    }

    @Override
    public Result finishRecord(Integer recordId, boolean force) {
        MedicalRecord record = this.getById(recordId);
        if (record == null) throw new BizException("病历不存在");
        if (record.getStatus() == null || record.getStatus() != 0)
            throw new BizException(record.getStatus() != null && record.getStatus() == 2
                    ? "该就诊已作废，不能完结" : "该就诊已完结，无需重复操作");
        // 完整性硬校验：主诉/诊断必填（完结=病历归档，最后一道关）
        if (StrUtil.isWhite(record.getChiefComplaint()) || StrUtil.isWhite(record.getDiagnosis()))
            throw new BizException("主诉/诊断未填写，为避免病历不完整请先补全");
        // 未完成事项软检查：有卡壳事项时返回清单，由前端确认后 force=true 强制完结
        if (!force) {
            List<String> pending = new ArrayList<>();
            long lab = labOrderMapper.selectCount(Wrappers.<LabOrder>lambdaQuery()
                    .eq(LabOrder::getRecordId, recordId).eq(LabOrder::getStatus, false));
            if (lab > 0) pending.add(lab + " 张化验单待出结果");
            long image = imageOrderMapper.selectCount(Wrappers.<ImageOrder>lambdaQuery()
                    .eq(ImageOrder::getRecordId, recordId).eq(ImageOrder::getStatus, false));
            if (image > 0) pending.add(image + " 张影像单待出报告");
            long surgery = surgeryMapper.selectCount(Wrappers.<Surgery>lambdaQuery()
                    .eq(Surgery::getRecordId, recordId).eq(Surgery::getStatus, 0));
            if (surgery > 0) pending.add(surgery + " 台手术待进行/待记录");
            long follow = followUpMapper.selectCount(Wrappers.<FollowUp>lambdaQuery()
                    .eq(FollowUp::getRecordId, recordId).eq(FollowUp::getStatus, false));
            if (follow > 0) pending.add(follow + " 条随访待完成");
            if (!pending.isEmpty())
                throw new BizException("还有未完成事项：" + String.join("；", pending) + "。确认仍要完结请再次提交");
        }
        // 完结即电子签名：签名人取登录态，回退到接诊医生，打印页不再留空白
        String signer = currentDoctorName();
        if (signer == null && record.getDoctorId() != null) {
            Doctor d = doctorMapper.selectById(record.getDoctorId());
            if (d != null) signer = d.getDoctorName();
        }
        this.lambdaUpdate().eq(MedicalRecord::getRecordId, recordId)
                .set(MedicalRecord::getStatus, 1)
                .set(MedicalRecord::getFinishTime, LocalDateTime.now())
                .set(MedicalRecord::getSignedBy, signer)
                .set(MedicalRecord::getSignedAt, LocalDateTime.now())
                .update();
        return Result.success("就诊已完结");
    }

    @Override
    public Result cancelRecord(Integer recordId, String reason) {
        MedicalRecord record = this.getById(recordId);
        if (record == null) throw new BizException("病历不存在");
        if (record.getStatus() == null || record.getStatus() != 0)
            throw new BizException("仅接诊中的就诊可以作废");
        if (reason == null || reason.isBlank()) throw new BizException("请填写作废原因");
        this.lambdaUpdate().eq(MedicalRecord::getRecordId, recordId)
                .set(MedicalRecord::getStatus, 2)
                .set(MedicalRecord::getCancelReason, reason.trim())
                .update();
        return Result.success("就诊已作废");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateRecord(MedicalRecord form) {
        if (form == null || form.getRecordId() == null) throw new BizException("缺少病历id");
        MedicalRecord record = this.getById(form.getRecordId());
        if (record == null) throw new BizException("病历不存在");
        assertEditable(record);
        // 乐观锁：前端带上的版本必须与库中一致，避免两个窗口互相静默覆盖
        if (form.getVersion() != null && record.getVersion() != null
                && !form.getVersion().equals(record.getVersion()))
            throw new BizException("病历已被他人修改（当前 V" + record.getVersion() + "），请刷新后重新编辑");
        // 变更前先落整本快照：病历不再被覆盖得无影无踪
        writeRevision(record, "修改病历内容");
        int nextVersion = (record.getVersion() == null ? 1 : record.getVersion()) + 1;
        // 只更新内容字段，费用不动（费用变动请走处方作废/退款流程）
        this.lambdaUpdate().eq(MedicalRecord::getRecordId, form.getRecordId())
                .set(MedicalRecord::getChiefComplaint, form.getChiefComplaint())
                .set(MedicalRecord::getPresentIllness, form.getPresentIllness())
                .set(MedicalRecord::getPastHistory, form.getPastHistory())
                .set(MedicalRecord::getPhysicalExam, form.getPhysicalExam())
                .set(MedicalRecord::getDiagnosis, form.getDiagnosis())
                .set(MedicalRecord::getTreatment, form.getTreatment())
                .set(MedicalRecord::getAdvice, form.getAdvice())
                .set(MedicalRecord::getInternalNote, form.getInternalNote())
                .set(MedicalRecord::getRevisitDate, form.getRevisitDate())
                .set(MedicalRecord::getTriageLevel, form.getTriageLevel())
                .set(MedicalRecord::getWeight, form.getWeight())
                .set(MedicalRecord::getTemperature, form.getTemperature())
                .set(MedicalRecord::getHeartRate, form.getHeartRate())
                .set(MedicalRecord::getRespiratoryRate, form.getRespiratoryRate())
                .set(MedicalRecord::getBloodPressure, form.getBloodPressure())
                .set(MedicalRecord::getDifferentialDiagnosis, form.getDifferentialDiagnosis())
                .set(MedicalRecord::getDiagnosisBasis, form.getDiagnosisBasis())
                .set(MedicalRecord::getDiagnosisCode, form.getDiagnosisCode())
                .set(MedicalRecord::getDiagnosisCodeSystem, form.getDiagnosisCodeSystem())
                .set(MedicalRecord::getVersion, nextVersion)
                // 内容变了，原审核结论自动失效，需重新审核
                .set(MedicalRecord::getReviewStatus, 0)
                .set(MedicalRecord::getReviewBy, null)
                .set(MedicalRecord::getReviewAt, null)
                .set(MedicalRecord::getReviewNote, null)
                .update();
        return Result.success("病历已更新（V" + nextVersion + "，需重新审核）");
    }

    @Override
    public Result reviewRecord(Integer recordId) {
        MedicalRecord record = this.getById(recordId);
        if (record == null) throw new BizException("病历不存在");
        if (record.getStatus() == null || record.getStatus() != 1)
            throw new BizException("仅已完结的病历可审核");
        if (record.getReviewStatus() != null && record.getReviewStatus() == 1)
            throw new BizException("该病历已审核，无需重复操作");
        String reviewer = currentDoctorName();
        if (reviewer == null && record.getDoctorId() != null) {
            Doctor d = doctorMapper.selectById(record.getDoctorId());
            if (d != null) reviewer = d.getDoctorName();
        }
        writeRevision(record, "病历审核");
        this.lambdaUpdate().eq(MedicalRecord::getRecordId, recordId)
                .set(MedicalRecord::getReviewStatus, 1)
                .set(MedicalRecord::getReviewBy, reviewer == null ? "系统" : reviewer)
                .set(MedicalRecord::getReviewAt, LocalDateTime.now())
                .set(MedicalRecord::getReviewNote, null)
                .update();
        return Result.success("病历审核完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result rejectRecord(Integer recordId, String reason) {
        MedicalRecord record = this.getById(recordId);
        if (record == null) throw new BizException("病历不存在");
        if (record.getStatus() == null || record.getStatus() != 1)
            throw new BizException("仅已完结的病历可退回");
        if (reason == null || reason.isBlank()) throw new BizException("请填写退回理由");
        if (record.getReviewStatus() != null && record.getReviewStatus() == 2)
            throw new BizException("该病历已退回，请勿重复操作");
        writeRevision(record, "病历审核退回：" + reason.trim());
        String reviewer = currentDoctorName();
        this.lambdaUpdate().eq(MedicalRecord::getRecordId, recordId)
                .set(MedicalRecord::getReviewStatus, 2)
                .set(MedicalRecord::getReviewBy, reviewer == null ? "系统" : reviewer)
                .set(MedicalRecord::getReviewAt, LocalDateTime.now())
                .set(MedicalRecord::getReviewNote, reason.trim())
                // 退回后回到接诊中，医生才能按意见修订；重新完结时重新签名
                .set(MedicalRecord::getStatus, 0)
                .update();
        return Result.success("病历已退回接诊医生，状态恢复为接诊中");
    }

    @Override
    public List<MedicalRecordRevision> listRevisions(Integer recordId) {
        return revisionMapper.selectList(Wrappers.<MedicalRecordRevision>lambdaQuery()
                .eq(MedicalRecordRevision::getRecordId, recordId)
                .orderByDesc(MedicalRecordRevision::getRevisionNo));
    }

    /** 完结/作废后单据只读：所有内容/单据写入口统一校验 */
    private void assertEditable(MedicalRecord record) {
        if (record.getStatus() == null || record.getStatus() != 0)
            throw new BizException("该就诊已完结/已作废，单据只读");
    }

    /** 变更前整本快照 + 登录态操作人（不信任前端传参） */
    private void writeRevision(MedicalRecord old, String reason) {
        MedicalRecordRevision rev = new MedicalRecordRevision();
        rev.setRecordId(old.getRecordId());
        rev.setRevisionNo(old.getVersion() == null ? 1 : old.getVersion());
        try {
            rev.setSnapshotJson(SNAPSHOT_MAPPER.writeValueAsString(old));
        } catch (JsonProcessingException e) {
            rev.setSnapshotJson("{}");
        }
        Object user = UserUtil.getUser();
        if (user instanceof Doctor doctor) {
            rev.setEditorId(doctor.getDoctorId());
            rev.setEditorName(doctor.getDoctorName());
        } else {
            rev.setEditorName(user == null ? "系统" : "员工");
        }
        rev.setChangeReason(reason);
        revisionMapper.insert(rev);
    }

    /** 当前登录员工姓名（签名/审核人一律取登录态） */
    private String currentDoctorName() {
        Object user = UserUtil.getUser();
        return user instanceof Doctor doctor ? doctor.getDoctorName() : null;
    }

    @Override
    public PetHistoryVo selectPetHistory(Integer petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return null;
        // 最近就诊（取前5条）
        List<PetHistoryRecord> briefs = new ArrayList<>();
        List<RecordDto> recent = baseMapper.selectPetRecords(new Page<>(1, 5), petId).getRecords();
        if (recent != null) {
            for (RecordDto d : recent) {
                briefs.add(new PetHistoryRecord(d.getRecordId(), d.getCreateTime(), d.getDiagnosis(), d.getDoctorName()));
            }
        }
        return new PetHistoryVo(
                pet.getPetName(), pet.getPetVariety(), pet.getPetSex(), pet.getPetBirthday(),
                pet.getPetSterilization(), pet.getPetChip(), pet.getPetAllergy(), pet.getPetHistory(),
                briefs,
                baseMapper.selectRecentRxByPet(petId),
                baseMapper.selectAbnormalLabByPet(petId));
    }

    @Override
    public RecordFullVo selectRecordFull(Integer recordId) {
        RecordDto record = baseMapper.selectRecordOne(recordId);
        if (record == null) return null;
        // 复诊链：回填初诊/上次就诊的诊断
        if (record.getOriginRecordId() != null) {
            MedicalRecord origin = this.getById(record.getOriginRecordId());
            if (origin != null) record.setOriginDiagnosis(origin.getDiagnosis());
        }
        // 化验单（主表 + 明细）
        List<RecordFullVo.LabOrderFull> labFull = new ArrayList<>();
        List<LabOrder> labOrders = labOrderMapper.selectList(Wrappers.<LabOrder>lambdaQuery()
                .eq(LabOrder::getRecordId, recordId).orderByDesc(LabOrder::getCreateTime));
        for (LabOrder o : labOrders) {
            List<LabOrderItem> items = labOrderItemMapper.selectList(Wrappers.<LabOrderItem>lambdaQuery()
                    .eq(LabOrderItem::getOrderId, o.getOrderId()).orderByAsc(LabOrderItem::getItemId));
            labFull.add(new RecordFullVo.LabOrderFull(o, items));
        }
        return new RecordFullVo(
                record,
                prescriptionMapper.selectByRecord(recordId),
                recordPackageMapper.selectList(Wrappers.<RecordPackage>lambdaQuery()
                        .eq(RecordPackage::getRecordId, recordId).orderByAsc(RecordPackage::getId)),
                labFull,
                imageOrderMapper.selectList(Wrappers.<ImageOrder>lambdaQuery()
                        .eq(ImageOrder::getRecordId, recordId).orderByDesc(ImageOrder::getCreateTime)),
                hospitalMapper.selectList(Wrappers.<HospitalRecord>lambdaQuery()
                        .eq(HospitalRecord::getRecordId, recordId).orderByDesc(HospitalRecord::getCreateTime)),
                surgeryMapper.selectList(Wrappers.<Surgery>lambdaQuery()
                        .eq(Surgery::getRecordId, recordId).orderByAsc(Surgery::getSurgeryId)),
                immuneRecordMapper.selectList(Wrappers.<ImmuneRecord>lambdaQuery()
                        .eq(ImmuneRecord::getRecordId, recordId).orderByDesc(ImmuneRecord::getCreateTime)),
                followUpMapper.selectList(Wrappers.<FollowUp>lambdaQuery()
                        .eq(FollowUp::getRecordId, recordId).orderByAsc(FollowUp::getPlanTime)),
                this.selectRevisits(recordId));
    }

    @Override
    public IPage<RecordDto> selectRecordPage(int numPage, int pageSize, RecordQueryVo query) {
        return baseMapper.selectRecordPage(new Page<>(numPage, pageSize), query);
    }

    @Override
    public RecordTodoVo selectRecordTodo() {
        // 接诊中的病历
        long treating = this.lambdaQuery().eq(MedicalRecord::getStatus, 0).count();
        // 待出结果的化验单 / 待出报告的影像单
        long lab = labOrderMapper.selectCount(Wrappers.<LabOrder>lambdaQuery().eq(LabOrder::getStatus, false));
        long image = imageOrderMapper.selectCount(Wrappers.<ImageOrder>lambdaQuery().eq(ImageOrder::getStatus, false));
        // 待进行/待记录的手术（status=0 待手术）
        long surgery = surgeryMapper.selectCount(Wrappers.<Surgery>lambdaQuery().eq(Surgery::getStatus, 0));
        // 待随访
        long follow = followUpMapper.selectCount(Wrappers.<FollowUp>lambdaQuery().eq(FollowUp::getStatus, false));
        // 待审核的自助注册顾客（audit_status=0，注册后等管理员放行）
        long clientPending = clientMapper.selectCount(Wrappers.<Client>lambdaQuery()
                .eq(Client::getAuditStatus, 0));
        return new RecordTodoVo(treating, lab, image, surgery, follow, clientPending);
    }
}
