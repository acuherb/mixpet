import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {Surgery, SurgeryVo} from "@/model/entiy/Surgery";
import {AnesthesiaRecord} from "@/model/entiy/AnesthesiaRecord";
import {SurgeryConsent} from "@/model/entiy/SurgeryConsent";
import {ConsentSheet} from "@/model/VO/ConsentSheet";
import reqApi from "@/request/reqAPI";

// 手术列表分页
export const reqSurgeryPage = (params: { numPage: number; pageSize: number; status?: number | null }): Promise<Page<Surgery>> =>
    reqApi<string, Page<Surgery>>("surgery/page", {params})
// 某病历的手术单
export const reqSurgeryByRecord = (recordId: number): Promise<Surgery[]> =>
    reqApi<string, Surgery[]>("surgery/record/" + recordId)
// 手术单详情（含麻醉记录）
export const reqSurgeryOne = (surgeryId: number): Promise<Surgery> =>
    reqApi<string, Surgery>("surgery/" + surgeryId)
// 新建手术单（排程+计费）
export const reqCreateSurgery = (data: SurgeryVo): Promise<Res> =>
    reqApi.post<string, Res>("surgery", data)
// 更新手术单（术前/术中/术后/状态/时间）
export const reqUpdateSurgery = (data: Surgery): Promise<Res> =>
    reqApi.put<string, Res>("surgery", data)
// 保存/更新麻醉记录
export const reqSaveAnesthesia = (data: AnesthesiaRecord): Promise<Res> =>
    reqApi.put<string, Res>("surgery/anesthesia", data)
// 签署/更新手术知情同意书
export const reqSignConsent = (data: { surgeryId: number; risks: string; agree: boolean; signer: string; note?: string }): Promise<Res> =>
    reqApi.post<string, Res>("surgery/consent", data)
// 某手术的同意书
export const reqConsentOne = (surgeryId: number): Promise<SurgeryConsent> =>
    reqApi<string, SurgeryConsent>("surgery/consent/" + surgeryId)
// 可打印的手术知情同意书数据
export const reqConsentSheet = (surgeryId: number): Promise<ConsentSheet> =>
    reqApi<string, ConsentSheet>("surgery/consent/sheet/" + surgeryId)
