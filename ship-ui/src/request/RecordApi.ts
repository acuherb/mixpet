import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {RecordDto, RecordDetailVo} from "@/model/DO/RecordDto";
import {RecordVo} from "@/model/entiy/MedicalRecord";
import {Res} from "@/request/Res";

// 医生接诊：保存病历+处方
export const reqSaveRecord = (data: RecordVo): Promise<Res> =>
    reqApi.post<string, Res>("record/save", data)
// 宠物就诊史
export const reqPetRecords = (petId: number, numPage = 1, pageSize = 20): Promise<Page<RecordDto>> =>
    reqApi<string, Page<RecordDto>>(`record/pet/${petId}`, {params: {numPage, pageSize}})
// 病历详情（含处方）
export const reqRecordOne = (recordId: number): Promise<RecordDetailVo> =>
    reqApi<string, RecordDetailVo>("record/one/" + recordId)
