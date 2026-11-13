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
    /** 频次：每日几次（数量=单次剂量×频次×天数） */
    dailyTimes?: number | null;
    /** 单次剂量(单位同药剂单位) */
    singleDose?: number | null;
    /** 单次剂量单位 */
    singleDoseUnit?: string | null;
    /** 用法，如 每日2次，每次1片 */
    usageInfo: string | null;
    /** 前端态：自动算量提示（不提交后端） */
    doseTip?: string;
    /** 前端态：用药天数（仅参与算量，不入库） */
    durationDays?: number;
    /** 前端态：自动算得的数量（与 quantity 不一致=医生手工改过，提示红色） */
    doseQty?: number;
}

export const examplePrescription = (): Prescription => {
    return {
        drugId: 0,
        quantity: 1,
        dailyTimes: 2,
        singleDose: null,
        singleDoseUnit: null,
        usageInfo: "",
        doseTip: "",
        durationDays: 1
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
    /** 现病史：症状从出现到本次就诊的发展过程 */
    presentIllness?: string | null;
    /** 既往病史：过去与本次判断相关的重要医疗信息 */
    pastHistory?: string | null;
    physicalExam: string | null;
    diagnosis: string | null;
    treatment: string | null;
    advice: string | null;
    /** 内部备注（仅员工可见） */
    internalNote: string | null;
    revisitDate: string | null;
    /** 关联的初诊/上次数病历id（复诊链） */
    originRecordId?: number | null;
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
    /** 血压(收缩/舒张,如 120/80) */
    bloodPressure?: string | null;
    feeRegister: number;
    feeTreat: number;
    /** 就诊状态：0接诊中 1已完结 2已作废 */
    status?: number | null;
    /** 完结时间 */
    finishTime?: string | null;
    /** 作废原因 */
    cancelReason?: string | null;
    /** 版本号：修改时回传做乐观锁，版本不一致后端拒绝覆盖 */
    version?: number | null;
    /** 完结电子签名与审核状态 */
    signedBy?: string | null;
    signedAt?: string | null;
    reviewStatus?: number | null;
    reviewBy?: string | null;
    reviewAt?: string | null;
    /** 审核退回理由 */
    reviewNote?: string | null;
    /** SOAP 的 A：鉴别诊断 / 诊断依据 / 结构化编码 */
    differentialDiagnosis?: string | null;
    diagnosisBasis?: string | null;
    diagnosisCode?: string | null;
    diagnosisCodeSystem?: string | null;
}

/** 病历修改留痕：某次变更前的整本快照 */
export interface RecordRevision {
    revisionId: number;
    recordId: number;
    revisionNo: number;
    snapshotJson: string;
    editorId?: number | null;
    editorName?: string | null;
    changeReason?: string | null;
    createTime?: string | null;
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
        feeTreat: 0,
        differentialDiagnosis: "",
        diagnosisBasis: "",
        diagnosisCode: null,
        diagnosisCodeSystem: null
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
