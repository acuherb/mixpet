package com.ship.model.vo;

import com.ship.model.entity.MedicalRecord;
import com.ship.model.entity.Prescription;
import com.ship.model.entity.ServicePackage;
import lombok.Data;

import java.util.List;

/**
 * 医生接诊保存病历的请求：病历内容 + 处方明细 + 套餐
 */
@Data
public class RecordVo {
    /** 病历（appointmentId 必填，其余由医生填写） */
    private MedicalRecord record;
    /** 处方明细（drugId + quantity + usageInfo，价格等信息以后端药品库为准） */
    private List<Prescription> prescriptions;
    /** 选购的套餐（packageId，价格等以后端套餐库为准） */
    private List<ServicePackage> packages;
}
