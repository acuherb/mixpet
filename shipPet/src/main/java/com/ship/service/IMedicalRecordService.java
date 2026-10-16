package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.entity.Doctor;
import com.ship.model.vo.RecordVo;
import com.ship.model.vo.RxSheetVo;

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
    Result addPrescriptions(Integer recordId, java.util.List<com.ship.model.entity.Prescription> list);

    /** 往已有病历追加套餐（未付款账单累加套餐费） */
    Result addPackages(Integer recordId, java.util.List<Integer> packageIds);

    /** 完结就诊：有未完成事项时返回清单（force=false）；force=true 强制完结 */
    Result finishRecord(Integer recordId, boolean force);

    /** 作废就诊（仅接诊中）：填写作废原因 */
    Result cancelRecord(Integer recordId, String reason);

    /** 未完绪病历修改（仅接诊中）：主诉/查体/诊断/治疗/医嘱/内部备注/复诊/分诊/体征 */
    Result updateRecord(MedicalRecord record);
}
