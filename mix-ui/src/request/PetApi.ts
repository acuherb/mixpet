import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {Pet} from "@/model/entiy/Pet";
import {PageQuery, YPetList} from "@/model/VO/BackQuery";
import {PetDto} from "@/model/DO/PetDto";
import {Res} from "@/request/Res";
import {NameDo} from "@/model/DO/NameDo";
import {PetHealth} from "@/model/entiy/PetHealth";

// 主页查询宠物
export const reqFourPet = (): Promise<Page<Pet>> =>
    reqApi<string, Page<Pet>>("pet/four?numPage=1&pageSize=6")
// 查看领养宠物
export const reqPetSee = (page: PageQuery): Promise<Page<Pet>> =>
    reqApi<string, Page<Pet>>("pet/four", {params: page})
// 查看宠物详情
export const reqPetOne = (petId: number): Promise<Pet> =>
    reqApi<string, Pet>("pet/one?petId=" + petId)
// 一页式宠物健康档案
export const reqPetHealth = (petId: number): Promise<PetHealth> =>
    reqApi<string, PetHealth>("pet/health/" + petId)
// 查看用户的宠物
export const reqClientPet = (): Promise<Pet[]> =>
    reqApi<string, Pet[]>("pet/clientOne")
// 管理员后台宠物列表
export const reqPetList = (page: YPetList): Promise<Page<PetDto>> =>
    reqApi<string, Page<PetDto>>("pet/page", {params: page})
// 管理员新增宠物
export const reqAddPet = (data: Pet): Promise<Res> =>
    reqApi.post<string, Res>("pet", data)
// 顾客自己添加宠物
export const reqAddSelfPet = (data: Pet): Promise<Res> =>
    reqApi.post<string, Res>("pet/self", data)
// 管理员修改宠物
export const reqUpdatePet = (data: Pet): Promise<Res> =>
    reqApi.put<string, Res>("pet", data)
// 管理员删除宠物
export const reqDelPet = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("pet/" + id)
// 管理员批量删除宠物
export const reqDelPetBatch = (ids: number[]): Promise<Res> =>
    reqApi.delete<string, Res>("pet/batch/" + ids)
// 管理员通过用户名查看宠物
export const reqPetNameByClientId = (clientId: number, petId: number): Promise<NameDo[]> => {
    if (petId == null || petId === 0) return reqApi<string, NameDo[]>("pet/client?clientId=" + clientId)
    else return reqApi<string, NameDo[]>("pet/client?clientId=" + clientId + "&petId=" + petId)
}
// 管理员查看无主的宠物
export const reqPetNoClient = (): Promise<NameDo[]> =>
    reqApi<string, NameDo[]>("pet/noClient")
// 宠物过户
export const reqTransferPet = (data: {
  petId: number,
  toClientId: number,
  transferDate: string,
  reason: string
}): Promise<Res> => reqApi.put<string, Res>("pet/transfer", data)
// 查询过户历史
export const reqTransferHistory = (petId: number): Promise<any[]> =>
    reqApi<string, any[]>("pet/transfer/" + petId)

// 恢复停用宠物
export const reqActivatePet = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("pet/activate/" + id)
// 停用宠物（手动）
export const reqDeactivatePet = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("pet/deactivate/" + id)
// 宠物入栏（有主变无主，遗弃/托管）
export const reqIntakePet = (id: number, reason: string): Promise<Res> =>
    reqApi.put<string, Res>("pet/intake/" + id + "?reason=" + encodeURIComponent(reason))
