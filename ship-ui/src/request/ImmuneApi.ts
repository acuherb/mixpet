import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {ImmuneRecord} from "@/model/entiy/ImmuneRecord";
import reqApi from "@/request/reqAPI";

// 接种档案 -------------------------------------------------------------
// 疫苗已合并进药品字典(drug_type=疫苗)，下拉用 reqDrugVaccineList（见 DrugApi）
export const reqSaveImmune = (data: { recordId: number | null; drugId: number; vaccinateTime: string; note: string }): Promise<Res> =>
    reqApi.post<string, Res>("immune", data)
export const reqImmunePage = (params: { petName: string; dueSoon: boolean | null; numPage: number; pageSize: number }): Promise<Page<ImmuneRecord>> =>
    reqApi<string, Page<ImmuneRecord>>("immune/page", {params})
export const reqImmuneByPet = (petId: number): Promise<ImmuneRecord[]> =>
    reqApi<string, ImmuneRecord[]>("immune/pet/" + petId)