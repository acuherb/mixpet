/**
 * 住院记录（联查）
 */
export interface HospitalDto {
    hospitalId: number;
    recordId: number;
    petId: number;
    clientId: number;
    doctorId: number;
    roomNo: string | null;
    inTime: string | null;
    outTime: string | null;
    dailyFee: number | null;
    /** 床位费/天 */
    bedFee?: number | null;
    /** 护理费/天 */
    nursingFee?: number | null;
    /** 输液费/天 */
    infusionFee?: number | null;
    /** 已收押金 */
    deposit?: number | null;
    days: number | null;
    /** false住院中 true已出院 */
    status: boolean;
    note: string | null;
    createTime?: string | null;
    petName?: string;
    clientName?: string;
    doctorName?: string;
    diagnosis?: string;
}