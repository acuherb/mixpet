/**
 * 兽用处方笺数据（734号国标：前记/正文/后记）
 * 注意：原 model/entiy/RxSheet.ts 仅含基础字段，这里重建为完整国标字段
 */
export interface RxSheet {
    recordId: number;
    /** 旧编号 No.x（兼容） */
    rxNo: string;
    /** 病历创建时间（兼容旧展示） */
    createTime: string | null;
    /** 开具日期 = prescription.create_time（回落病历创建时间） */
    prescriptionTime: string | null;
    /** 处方笺编号 RX-YYYYMMDD-XXXX（未打印为 null） */
    printNo: string | null;
    /** 联次份数 1=仅第三联 3=三联全打 */
    copies: number | null;
    /** 毛色（pet.pet_color 或补录） */
    hairColor: string | null;
    /** 动物种类（补录） */
    species: string | null;
    /** 休药期（宠物处方 "—"） */
    withdrawalPeriod: string | null;
    /** 发药人（bill.dispense_by 或补录；空则留手写位） */
    dispenser: string | null;
    /** VALID / VOID（未打印为 null） */
    supplementStatus: string | null;

    clientName: string;
    petName: string;
    petVariety: string | null;
    petSex: boolean | null;
    petBirthday: string | null;
    petWeight: number | null;
    petChip: string | null;
    diagnosis: string | null;
    doctorName: string;
    doctorCert: string | null;
    departmentName: string | null;

    items: RxItem[];
    totalAmount: number | null;
}

/** 处方明细行 */
export interface RxItem {
    drugName: string;
    drugSpec: string | null;
    drugUnit: string | null;
    drugPrice: number | null;
    quantity: number;
    dailyTimes?: number | null;
    singleDose?: number | null;
    singleDoseUnit?: string | null;
    usageInfo: string | null;
}

/** 打印预检入参 */
export interface RxPrintPrepareVo {
    recordId: number;
    prescriptionId?: number | null;
    hairColor?: string | null;
    species?: string | null;
    withdrawalPeriod?: string | null;
    dispenser?: string | null;
    copies?: number;
}

/** 打印预检出参 */
export interface RxPrintResultVo {
    code: number;
    msg: string;
    sheet: RxSheet | null;
    supplement: {
        printNo: string;
        hairColor: string | null;
        species: string | null;
        withdrawalPeriod: string | null;
        dispenser: string | null;
        copies: number | null;
        status: string;
        printedAt: string | null;
    } | null;
}

/** 处方笺档案列表项 */
export interface RxArchiveVo {
    printNo: string;
    prescriptionTime: string | null;
    clientName: string;
    petName: string;
    doctorName: string;
    doctorCert: string | null;
    copies: number | null;
    printedAt: string | null;
    status: string;
    voidReason: string | null;
    recordId: number;
}
