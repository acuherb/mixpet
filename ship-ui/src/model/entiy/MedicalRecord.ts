import {ServicePackage} from "@/model/entiy/ServicePackage";
import {SurgeryVo} from "@/model/entiy/Surgery";

/**
 * 处方明细
 */
export interface Prescription {
    prescriptionId?: number;
    recordId?: number | null;
    drugId: number;
    /** 冗余快照：开方时的名称/规格/单位/单价 */
    drugName?: string;
    drugSpec?: string | null;
    drugUnit?: string | null;
    drugPrice?: number | null;
    quantity: number;
    /** 用法，如 每日2次，每次1片 */
    usageInfo: string | null;
}

export const examplePrescription = (): Prescription => {
    return {
        drugId: 0,
        quantity: 1,
        usageInfo: ""
    }
}

/**
 * 电子病历（医生接诊填写）
 */
export interface MedicalRecord {
    recordId?: number;
    appointmentId: number;
    petId?: number | null;
    clientId?: number | null;
    doctorId?: number | null;
    departmentId?: number | null;
    chiefComplaint: string | null;
    physicalExam: string | null;
    diagnosis: string | null;
    treatment: string | null;
    advice: string | null;
    /** 内部备注（仅员工可见） */
    internalNote: string | null;
    revisitDate: string | null;
    /** 分诊级别：1急诊 2普通 3复诊 */
    triageLevel?: number | null;
    /** 就诊体重(kg) */
    weight?: number | null;
    /** 体温(℃) */
    temperature?: number | null;
    /** 心率(次/分) */
    heartRate?: number | null;
    /** 呼吸(次/分) */
    respiratoryRate?: number | null;
    feeRegister: number;
    feeTreat: number;
}

export const exampleMedicalRecord = (appointmentId: number): MedicalRecord => {
    return {
        appointmentId: appointmentId,
        chiefComplaint: "",
        physicalExam: "",
        diagnosis: "",
        treatment: "",
        advice: "",
        internalNote: "",
        revisitDate: null,
        feeRegister: 10,
        feeTreat: 0
    }
}

/**
 * 保存病历的请求体
 */
export interface RecordVo {
    record: MedicalRecord;
    prescriptions: Prescription[];
    packages: ServicePackage[];
    /** 接诊时一次性开的检验项 id 列表 */
    labTestIds: number[];
    /** 接诊时一次性开的影像项目 id */
    imageItemId: number | null;
    /** 影像检查部位/目的 */
    imageExamPart: string;
    /** 接诊时一次性提交的手术申请 */
    surgery: SurgeryVo | null;
}
