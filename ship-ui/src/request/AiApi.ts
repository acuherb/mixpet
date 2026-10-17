import {AiAssistResult} from "@/model/VO/AiAdvice";
import reqApi from "@/request/reqAPI";

// AI 辅助诊疗：基于当前病历生成结构化建议（留痕+防重复）
export const reqAiAssist = (recordId: number): Promise<AiAssistResult> =>
    reqApi.post<string, AiAssistResult>("ai/assist", {}, {params: {recordId}})
