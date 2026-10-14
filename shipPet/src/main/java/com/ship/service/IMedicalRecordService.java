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
}
