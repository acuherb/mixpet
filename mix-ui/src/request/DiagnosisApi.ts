import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {Page} from "@/model/DO/Page";

/** 诊断编码字典 */
export interface DiagnosisDict {
    dictId?: number;
    code: string;
    name: string;
    codeSystem?: string | null;
    category?: string | null;
    species?: string | null;
    active?: boolean | null;
}

// 启用中的编码（接诊向导下拉）
export const reqDiagnosisList = (keyword?: string): Promise<DiagnosisDict[]> =>
    reqApi<string, DiagnosisDict[]>("diagnosis", {params: {keyword: keyword ?? ''}})
// 字典维护分页
export const reqDiagnosisPage = (params: { numPage: number; pageSize: number; keyword?: string }): Promise<Page<DiagnosisDict>> =>
    reqApi<string, Page<DiagnosisDict>>("diagnosis/page", {params})
export const reqAddDiagnosis = (data: DiagnosisDict): Promise<Res> =>
    reqApi.post<string, Res>("diagnosis", data)
export const reqUpdateDiagnosis = (data: DiagnosisDict): Promise<Res> =>
    reqApi.put<string, Res>("diagnosis", data)
// 停用（不物理删除，历史病历编码仍可追溯）
export const reqDelDiagnosis = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("diagnosis/" + id)
