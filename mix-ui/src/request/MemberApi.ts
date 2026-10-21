import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {ClientCountCard, CountCardTemplate, MemberLevel} from "@/model/entiy/Member";

// 会员等级
export const reqMemberLevels = (): Promise<MemberLevel[]> =>
    reqApi<string, MemberLevel[]>("member/level")
export const reqSaveMemberLevel = (data: MemberLevel): Promise<Res> =>
    reqApi.post<string, Res>("member/level", data)
export const reqUpdateMemberLevel = (data: MemberLevel): Promise<Res> =>
    reqApi.put<string, Res>("member/level", data)
export const reqDelMemberLevel = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("member/level/" + id)
// 次卡模板
export const reqCardTemplates = (): Promise<CountCardTemplate[]> =>
    reqApi<string, CountCardTemplate[]>("member/cardTemplate")
export const reqSaveCardTemplate = (data: CountCardTemplate): Promise<Res> =>
    reqApi.post<string, Res>("member/cardTemplate", data)
export const reqDelCardTemplate = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("member/cardTemplate/" + id)
// 客户会员操作
export const reqClientCards = (clientId: number): Promise<ClientCountCard[]> =>
    reqApi<string, ClientCountCard[]>("member/cards/" + clientId)
export const reqBuyCard = (clientId: number, templateId: number): Promise<Res> =>
    reqApi.post<string, Res>("member/buyCard", {}, {params: {clientId, templateId}})
export const reqUseCard = (cardId: number): Promise<Res> =>
    reqApi.post<string, Res>("member/useCard/" + cardId)
export const reqExchangePoints = (clientId: number, points: number): Promise<Res> =>
    reqApi.post<string, Res>("member/exchange", {}, {params: {clientId, points}})
