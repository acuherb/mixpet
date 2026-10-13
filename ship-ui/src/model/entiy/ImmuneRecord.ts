/**
 * 宠物接种档案（联查）
 */
export interface ImmuneRecord {
    immuneId: number;
    recordId: number | null;
    petId: number;
    clientId: number;
    doctorId: number | null;
    vaccineId: number;
    vaccineName: string;
    doseCnt: number;
    vaccinateTime: string;
    nextTime: string | null;
    note: string | null;
    createTime?: string | null;
    petName?: string;
    clientName?: string;
    doctorName?: string;
}