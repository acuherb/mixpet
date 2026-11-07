package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.RecordDto;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.entity.MedicalRecordRevision;
import com.mix.model.entity.Doctor;
import com.mix.model.vo.PetHistoryVo;
import com.mix.model.vo.RecordFullVo;
import com.mix.model.vo.RecordQueryVo;
import com.mix.model.vo.RecordTodoVo;
import com.mix.model.vo.RecordVo;
import com.mix.model.vo.RxSheetVo;

import java.util.List;

/**
 * 电子病历 Service
 */
public interface IMedicalRecordService extends IService<MedicalRecord> {
    /** 医生接诊：保存病历+处方明细，生成未付账单，挂号单标记完成（事务） */
    Result saveRecord(RecordVo vo, Doctor doctor);

    /** 宠物的就诊史分页 */
    IPage<RecordDto> selectPetRecords(Integer petId, int numPage, int pageSize);

    /** 单份病历详情（联查） */
    RecordDto selectRecordOne(Integer recordId);

    /** 某病历的复诊链（后续复诊记录，按时间正序） */
    List<RecordDto> selectRevisits(Integer recordId);

    /** 按挂号单查已生成的病历（接诊后取 recordId 用） */
    RecordDto selectByAppointment(Integer appointmentId);

    /** 兽医处方笺数据（国标）：聚合病历+宠物+医生+处方 */
    RxSheetVo selectRxSheet(Integer recordId);

    /** 作废单条处方：冲减未付账单的药费 */
    Result cancelPrescription(Integer prescriptionId);

    /** 往已有病历追加处方（未付款账单累加药费） */
    Result addPrescriptions(Integer recordId, java.util.List<com.mix.model.entity.Prescription> list);

    /** 往已有病历追加套餐（未付款账单累加套餐费） */
    Result addPackages(Integer recordId, java.util.List<Integer> packageIds);

    /** 完结就诊：有未完成事项时返回清单（force=false）；force=true 强制完结 */
    Result finishRecord(Integer recordId, boolean force);

    /** 作废就诊（仅接诊中）：填写作废原因 */
    Result cancelRecord(Integer recordId, String reason);

    /** 未完绪病历修改（仅接诊中）：留痕 + 版本号 +1 + 乐观锁，原审核自动失效 */
    Result updateRecord(MedicalRecord record);

    /** 病历审核（仅已完结）：写入审核人/时间 */
    Result reviewRecord(Integer recordId);

    /** 病历修改历史：按版本倒序的变更前快照 */
    List<MedicalRecordRevision> listRevisions(Integer recordId);

    /** 宠物病史速览（接诊侧栏）：档案+过敏/既往史+最近就诊+近期用药+检验异常 */
    PetHistoryVo selectPetHistory(Integer petId);

    /** 病例全量档案（一个接口拿全：病历+处方+套餐+化验含明细+影像+住院+手术+免疫+随访+复诊链） */
    RecordFullVo selectRecordFull(Integer recordId);

    /** 病历多条件检索（管理端/医生端病例中心） */
    IPage<RecordDto> selectRecordPage(int numPage, int pageSize, RecordQueryVo query);

    /** 诊疗待办统计（工作台/看板）：接诊中/待出结果/待报告/待手术/待随访 */
    RecordTodoVo selectRecordTodo();
}
