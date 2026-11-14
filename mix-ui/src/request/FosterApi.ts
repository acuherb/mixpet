import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {FosterCardDto} from "@/model/DO/FosterCardDto";
import {YFosterList} from "@/model/VO/BackQuery";
import {FosterDto} from "@/model/DO/FosterDto";
import {Foster} from "@/model/entiy/Foster";
import {Res} from "@/request/Res";
import {FosterLog, FosterLogVo, FosterPhoto} from "@/model/VO/FosterCare";

// 主页匿名展示墙（脱敏）
export const reqFosterPet = (pageSize: number): Promise<Page<FosterCardDto>> =>
    reqApi<string, Page<FosterCardDto>>("foster/pet?pageSize=" + pageSize)
// 后台寄养列表
export const reqFosterList = (page: YFosterList): Promise<Page<FosterDto>> =>
    reqApi<string, Page<FosterDto>>("foster/page", {params: page})
export const reqAddFoster = (data: Foster): Promise<Res> =>
    reqApi.post<string, Res>("foster", data)
export const reqUpdateFoster = (data: Foster): Promise<Res> =>
    reqApi.put<string, Res>("foster", data)
/** 待入托 → 寄养中（独立接口，护士可操作） */
export const reqStartFoster = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("foster/start/" + id, {})
/**
 * 接回结算：金额可人工改价（必须带说明留痕），押金冲抵后净额入账。
 * settleMoney 不传则由后端按天数×单价自动定价。
 */
export const reqBackFoster = (id: number, backDate: string, settleMoney?: number, adjustNote?: string): Promise<Res> =>
    reqApi.put<string, Res>("foster/back/" + id, {backDate, settleMoney, adjustNote})
export const reqDelFoster = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("foster/" + id)
export const reqDelFosterBatch = (ids: number[]): Promise<Res> =>
    reqApi.delete<string, Res>("foster/batch/" + ids)

/** 员工单份详情（结算单打印） */
export const reqFosterOne = (id: number): Promise<FosterDto> =>
    reqApi<string, FosterDto>("foster/one/" + id)

// ── 顾客端「我的寄养」 ────────────────────────────────
export const reqFosterSelf = (): Promise<FosterDto[]> =>
    reqApi<string, FosterDto[]>("foster/self")
export const reqFosterSelfOne = (id: number): Promise<FosterDto> =>
    reqApi<string, FosterDto>("foster/self/" + id)

// ── 每日照护记录 / 照片 ───────────────────────────────
export const reqFosterLogs = (fosterId: number): Promise<FosterLog[]> =>
    reqApi<string, FosterLog[]>("foster/log/" + fosterId)
export const reqFosterPhotos = (fosterId: number): Promise<FosterPhoto[]> =>
    reqApi<string, FosterPhoto[]>("foster/photo/" + fosterId)
export const reqAddFosterLog = (data: FosterLogVo): Promise<Res> =>
    reqApi.post<string, Res>("foster/log", data)
/** 上传一张照片，成功时 res.data 为文件名，需随 addLog 一起落库 */
export const reqUploadFosterPhoto = (file: File): Promise<Res> => {
    const fd = new FormData()
    fd.append('file', file)
    return reqApi.post<string, Res>("foster/photo/upload", fd)
}
/** 照片读取走后端归属校验接口，不暴露物理文件名 */
export const fosterPhotoUrl = (photoId: number): string =>
    "/api/foster/photo/file/" + photoId
