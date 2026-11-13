import {Pet} from "@/model/entiy/Pet";
import {Client} from "@/model/entiy/Client";
import {Prescription} from "@/model/entiy/MedicalRecord";
import {ImmuneRecord} from "@/model/entiy/ImmuneRecord";
import {HospitalDto} from "@/model/DO/HospitalDto";
import {Surgery} from "@/model/entiy/Surgery";
import {LabOrder} from "@/model/entiy/LabOrder";
import {ImageOrder} from "@/model/entiy/ImageOrder";
import {RecordDto} from "@/model/DO/RecordDto";

/**
 * 宠物就诊档案（后端 /pet/archive/{petId} 原样返回）
 */
export interface PetArchive {
    pet: Pet;
    client: Client | null;
    /** 全部病历，时间倒序 */
    visits: RecordDto[];
    /** recordId → 该次处方明细 */
    prescriptions: Record<number, Prescription[]>;
    immune: ImmuneRecord[];
    hospital: HospitalDto[];
    surgery: Surgery[];
    lab: LabOrder[];
    image: ImageOrder[];
    stat: PetArchiveStat;
}

export interface PetArchiveStat {
    visitCount: number;
    firstVisit: string | null;
    lastVisit: string | null;
    lastDiagnosis: string | null;
    nextImmune: string | null;
    weight: number | null;
    totalFee: number | null;
}
