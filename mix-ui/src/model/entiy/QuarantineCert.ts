/**
 * 检疫单（版式移植自 PetSystem ReportPris_jianyi.mrt，A5 横向）
 */
export interface QuarantineItem {
    drugName: string;
    drugSpec?: string | null;
    drugUnit?: string | null;
    quantity?: number | null;
    /** 实际用量文本（单次×频次×天数） */
    dosage?: string | null;
    usageInfo?: string | null;
    singleDose?: number | null;
    singleDoseUnit?: string | null;
    dailyTimes?: number | null;
    days?: number | null;
}

export interface QuarantineCert {
    certNo: string | null;
    recordId: number;
    petId?: number | null;
    clientId?: number | null;
    doctorId?: number | null;
    ownerName?: string | null;
    ownerTel?: string | null;
    petName?: string | null;
    /** 动物种类（犬/猫…） */
    species?: string | null;
    breed?: string | null;
    sex?: string | null;
    ageDesc?: string | null;
    weight?: number | null;
    /** 文字记录/检疫结论 */
    content?: string | null;
    items?: QuarantineItem[];
    doctorName?: string | null;
    createTime?: string | null;
    /** 该病历是否已开具过（true 时前端走补打） */
    existed?: boolean;
}
