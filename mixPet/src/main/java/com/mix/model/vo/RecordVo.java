package com.mix.model.vo;

import com.mix.model.entity.MedicalRecord;
import com.mix.model.entity.Prescription;
import com.mix.model.entity.ServicePackage;
import lombok.Data;

import java.util.List;

/**
 * 医生接诊保存病历的请求：病历 + 处方 + 套餐 + 一次性开的检查/手术申请
 */
@Data
public class RecordVo {
    /** 病历（appointmentId 必填，其余由医生填写） */
    private MedicalRecord record;
    /** 处方明细（drugId + quantity + usageInfo，价格等信息以后端药品库为准） */
    private List<Prescription> prescriptions;
    /** 选购的套餐（packageId，价格等以后端套餐库为准） */
    private List<ServicePackage> packages;
    /** 接诊时一次性开的检验项 id 列表（生成一张化验单，价格以字典为准） */
    private List<Integer> labTestIds;
    /** 接诊时一次性开的影像项目 id（生成一张影像申请单） */
    private Integer imageItemId;
    /** 影像检查部位/目的 */
    private String imageExamPart;
    /** 接诊时一次性提交的手术申请 */
    private SurgeryVo surgery;
}
