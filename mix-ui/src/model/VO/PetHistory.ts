/**
 * 宠物病史速览（医生接诊侧栏）
 */
export interface PetHistory {
    petName: string;
    petVariety: string | null;
    petSex: boolean | null;
    petBirthday: string | null;
    petSterilization: boolean | null;
    petChip: string | null;
    /** 过敏史 */
    petAllergy: string | null;
    /** 既往病史 */
    petHistory: string | null;
    /** 最近就诊（时间倒序，最多5条） */
    recentRecords: PetHistoryRecord[];
    /** 近期用药（最多10条） */
    recentRx: PetHistoryRx[];
    /** 近期检验异常项（最多10条） */
    abnormalLabs: PetHistoryLab[];
}

export interface PetHistoryRecord {
    recordId: number;
    createTime: string | null;
    diagnosis: string | null;
    doctorName: string | null;
}

export interface PetHistoryRx {
    drugName: string;
    usageInfo: string | null;
    createTime: string | null;
}

export interface PetHistoryLab {
    itemName: string;
    resultValue: string | null;
    refRange: string | null;
    itemUnit: string | null;
    resultTime: string | null;
}
