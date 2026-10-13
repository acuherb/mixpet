/**
 * 麻醉记录（一手术一条）
 */
export interface AnesthesiaRecord {
    anesthesiaId?: number;
    surgeryId: number;
    anesthesiaType: string | null;
    anesthetist: string | null;
    startTime: string | null;
    endTime: string | null;
    drugUsed: string | null;
    vitals: string | null;
    note: string | null;
}
