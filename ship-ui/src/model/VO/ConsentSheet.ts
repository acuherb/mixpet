/**
 * 可打印的手术/麻醉知情同意书数据
 */
export interface ConsentSheet {
    surgeryId: number;
    consentNo: string | null;
    createTime: string | null;
    clientName: string | null;
    petName: string | null;
    petVariety: string | null;
    petSex: boolean | null;
    petBirthday: string | null;
    petWeight: number | null;
    petChip: string | null;
    diagnosis: string | null;
    surgeryName: string | null;
    surgeryType: string | null;
    anesthesiaType: string | null;
    scheduledTime: string | null;
    doctorName: string | null;
    risks: string | null;
    agree: boolean | null;
    signer: string | null;
    signedTime: string | null;
    operator: string | null;
}
