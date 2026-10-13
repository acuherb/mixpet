import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {Vaccine} from "@/model/entiy/Vaccine";
import {ImmuneRecord} from "@/model/entiy/ImmuneRecord";
import reqApi from "@/request/reqAPI";

// 疫苗字典 -------------------------------------------------------------
export const reqVaccineList = (): Promise<Vaccine[]> =>
    reqApi<string, Vaccine[]>("vaccine/list")
export const reqVaccinePage = (params: { vaccineName: string; numPage: number; pageSize: number }): Promise<Page<Vaccine>> =>
    reqApi<string, Page<Vaccine>>("vaccine/page", {params})
export const reqAddVaccine = (data: Vaccine): Promise<Res> =>
    reqApi.put<string, Res>("vaccine", data)
export const reqUpdateVaccine = (data: Vaccine): Promise<Res> =>
    reqApi.put<string, Res>("vaccine", data)
export const reqDelVaccine = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("vaccine/" + id)

// 接种档案 -------------------------------------------------------------
export const reqSaveImmune = (data: { recordId: number | null; vaccineId: number; vaccinateTime: string; note: string }): Promise<Res> =>
    reqApi.post<string, Res>("immune", data)
export const reqImmunePage = (params: { petName: string; dueSoon: boolean | null; numPage: number; pageSize: number }): Promise<Page<ImmuneRecord>> =>
    reqApi<string, Page<ImmuneRecord>>("immune/page", {params})
export const reqImmuneByPet = (petId: number): Promise<ImmuneRecord[]> =>
    reqApi<string, ImmuneRecord[]>("immune/pet/" + petId)