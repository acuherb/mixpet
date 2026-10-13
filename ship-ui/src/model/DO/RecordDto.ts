import {MedicalRecord} from "@/model/entiy/MedicalRecord";

/**
 * 就诊史列表行 / 病历详情（联查）
 */
export interface RecordDto extends MedicalRecord {
    /** 列表/详情返回时必有 */
    recordId: number;
    feeDrug?: number | null;
    totalMoney?: number | null;
    payStatus?: boolean | null;
    createTime?: string | null;
    petName?: string;
    clientName?: string;
    doctorName?: string;
    departmentName?: string;
}

/**
 * 病历详情响应：病历 + 处方列表 + 套餐
 */
export interface RecordDetailVo {
    record: RecordDto;
    prescriptions: import("@/model/entiy/MedicalRecord").Prescription[];
    packages: import("@/model/entiy/ServicePackage").RecordPackage[];
}
