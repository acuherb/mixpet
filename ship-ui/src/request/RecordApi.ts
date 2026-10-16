import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {RecordDto, RecordDetailVo} from "@/model/DO/RecordDto";
import {Prescription, RecordVo} from "@/model/entiy/MedicalRecord";
import {MedicalRecord} from "@/model/entiy/MedicalRecord";
import {RxSheet} from "@/model/entiy/RxSheet";
import {Res} from "@/request/Res";

// 医生接诊：保存病历+处方
export const reqSaveRecord = (data: RecordVo): Promise<Res> =>
    reqApi.post<string, Res>("record/save", data)
// 按挂号单查已生成病历（接诊后取 recordId）
export const reqRecordByAppointment = (appointmentId: number): Promise<RecordDto> =>
    reqApi<string, RecordDto>("record/appointment/" + appointmentId)
// 兽医处方笺数据（国标）
export const reqRxSheet = (recordId: number): Promise<RxSheet> =>
    reqApi<string, RxSheet>("record/rx/" + recordId)
// 宠物就诊史
export const reqPetRecords = (petId: number, numPage = 1, pageSize = 20): Promise<Page<RecordDto>> =>
    reqApi<string, Page<RecordDto>>(`record/pet/${petId}`, {params: {numPage, pageSize}})
// 病历详情（含处方）
export const reqRecordOne = (recordId: number): Promise<RecordDetailVo> =>
    reqApi<string, RecordDetailVo>("record/one/" + recordId)
// 某病历的复诊链（后续复诊记录）
export const reqRecordRevisits = (recordId: number): Promise<RecordDto[]> =>
    reqApi<string, RecordDto[]>("record/revisits/" + recordId)
// 往已有病历追加处方（陆续加药）
export const reqAddRecordPrescription = (data: { recordId: number; prescriptions: Prescription[] }): Promise<Res> =>
    reqApi.post<string, Res>("record/prescription", data)
// 往已有病历追加套餐
export const reqAddRecordPackage = (data: { recordId: number; packageIds: number[] }): Promise<Res> =>
    reqApi.post<string, Res>("record/package", data)
// 完结就诊：有未完成事项时后端返回清单，确认后 force=true 强制完结
export const reqFinishRecord = (recordId: number, force = false): Promise<Res> =>
    reqApi.put<string, Res>("record/finish/" + recordId, {}, {params: {force}})
// 作废就诊（仅接诊中，需填原因）
export const reqCancelRecord = (recordId: number, reason: string): Promise<Res> =>
    reqApi.put<string, Res>("record/cancel/" + recordId, {}, {params: {reason}})
// 未完绪病历修改（内容字段：主诉/查体/诊断/治疗/医嘱/备注/复诊/分诊/体征）
export const reqUpdateRecord = (record: MedicalRecord): Promise<Res> =>
    reqApi.put<string, Res>("record", record)
