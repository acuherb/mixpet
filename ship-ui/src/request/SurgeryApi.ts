import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {Surgery, SurgeryVo} from "@/model/entiy/Surgery";
import {AnesthesiaRecord} from "@/model/entiy/AnesthesiaRecord";
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
