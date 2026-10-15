import {Pet} from "@/model/entiy/Pet";
import {RecordDto} from "@/model/DO/RecordDto";

export interface ImmuneRecordLite {
    immuneId?: number;
    drugName?: string | null;
    doseCnt?: number | null;
    vaccinateTime?: string | null;
    nextTime?: string | null;
}
export interface HospitalRecordLite {
    hospitalId?: number;
    roomNo?: string | null;
    inTime?: string | null;
    outTime?: string | null;
    status?: boolean | null;
    days?: number | null;
}

/**
 * 一页式宠物健康档案
 */
export interface PetHealth {
    pet: Pet;
    visits: RecordDto[];
    immune: ImmuneRecordLite[];
    hospital: HospitalRecordLite[];
    weightTrend: { date: string; weight: number }[];
    visitCount: number;
    lastVisitDate: string | null;
    lastDiagnosis: string | null;
    nextVaccineDate: string | null;
}
