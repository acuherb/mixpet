import {Prescription} from "@/model/entiy/MedicalRecord";

/**
 * 兽医处方笺数据（国标）
 */
export interface RxSheet {
    recordId: number;
    rxNo: string;
    createTime: string;
    clientName: string;
    petName: string;
    petVariety: string | null;
    petSex: boolean | null;
    petBirthday: string | null;
    petWeight: number | null;
    petChip: string | null;
    diagnosis: string;
    doctorName: string;
    doctorCert: string | null;
    departmentName: string;
    items: Prescription[];
    totalAmount: number;
}
