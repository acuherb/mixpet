import reqApi from "@/request/reqAPI";
import {QuarantineCert} from "@/model/entiy/QuarantineCert";

// 开具草稿：病历+宠物+客户+医生+处方明细预填；已有单据则直接返回该单
export const reqQuarantineDraft = (recordId: number): Promise<QuarantineCert> =>
    reqApi.get<string, QuarantineCert>("quarantine/draft/" + recordId)
// 开具并保存（生成 JY-YYYYMMDD-XXXX 单号）
export const reqQuarantineIssue = (vo: QuarantineCert): Promise<QuarantineCert> =>
    reqApi.post<string, QuarantineCert>("quarantine/issue", vo)
// 按单号补打取数
export const reqQuarantineByNo = (certNo: string): Promise<QuarantineCert> =>
    reqApi.get<string, QuarantineCert>("quarantine/" + encodeURIComponent(certNo))
