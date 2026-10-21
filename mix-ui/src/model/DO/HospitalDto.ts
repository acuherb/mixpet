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