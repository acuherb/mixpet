/**
 * 手术/麻醉知情同意书
 */
export interface SurgeryConsent {
    consentId: number;
    surgeryId: number;
    surgeryName: string | null;
    anesthesiaType: string | null;
    risks: string | null;
    agree: boolean | null;
    signer: string | null;
    signedTime: string | null;
    operator: string | null;
    note: string | null;
}
