package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.vo.PetHistoryVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 电子病历 Mapper
 */
@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {
    /** 宠物的就诊史（联查宠物/客户/医生/科室名与账单费用） */
    IPage<RecordDto> selectPetRecords(IPage<RecordDto> page, Integer petId);

    /** 单份病历详情（联查） */
    RecordDto selectRecordOne(Integer recordId);

    /** 某医生的接诊病历（开化验单时选择病历用） */
    List<RecordDto> selectDoctorRecords(@Param("doctorId") Integer doctorId, @Param("limit") int limit);

    /** 随访提醒：复诊日期在30天内（含逾期）的病历 */
    List<RecordDto> selectDueRevisits();

    /** 宠物近期用药（跨处方+病历，最近10条） */
    List<PetHistoryVo.RecentRx> selectRecentRxByPet(@Param("petId") Integer petId);

    /** 宠物近期检验异常项（跨化验明细+化验单+病历，最近10条） */
    List<PetHistoryVo.AbnormalLab> selectAbnormalLabByPet(@Param("petId") Integer petId);
}