/**
 * 住院每日监护记录
 */
export interface HospitalMonitor {
    monitorId: number;
    hospitalId: number;
    petId: number;
    monitorTime: string | null;
    temperature: number | null;
    heartRate: number | null;
    respiratoryRate: number | null;
    weight: number | null;
    mentalState: string | null;
    appetite: string | null;
    excretion: string | null;
    medication: string | null;
    treatment: string | null;
    note: string | null;
    recorder: string | null;
    createTime?: string | null;
}
