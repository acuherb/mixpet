import reqApi from '@/request/reqAPI'

export interface FlowChip {
  key: string
  petName: string | null
  sub: string
  flag?: string | null
  cls?: string | null
  stageTitle?: string
  nextLabel?: string
  actionPath?: string
  clientName?: string | null
}
export interface FlowStage {
  key: string
  title: string
  desc: string
  actionPath: string
  actionLabel: string
  count: number
  items: FlowChip[]
}
export interface DoctorFlowVo {
  stages: FlowStage[]
  serverTime: string
}

/**
 * reqApi 响应拦截器返回的是整个业务体 {code,msg,data}，而后端流接口是
 * Result.data(vo) 包了一层。这里统一解包，否则三个流页面拿到的是 Result、
 * vo.stages 永远是 undefined —— 流水线会静默显示为空（曾长期漏检）。
 * 仅当 data 确实是流快照（含 stages）才解包，异常/401 时原样返回，调用方按空数组兜底。
 */
const unwrapFlow = (res: any): DoctorFlowVo =>
  (res?.data && typeof res.data === 'object' && Array.isArray(res.data.stages) ? res.data : res) as DoctorFlowVo

// 诊疗流（闭环状态机）脊柱：单一接口取回全部环节快照
export const reqDoctorPipeline = (): Promise<DoctorFlowVo> =>
  reqApi<string, any>('flow/doctorPipeline').then(unwrapFlow)
// 前台接待流（后端限 NURSE 及以上）
export const reqReceptionPipeline = (): Promise<DoctorFlowVo> =>
  reqApi<string, any>('flow/receptionPipeline').then(unwrapFlow)
// 管理驾驶舱（后端限 MANAGER 及以上）
export const reqManagerPipeline = (): Promise<DoctorFlowVo> =>
  reqApi<string, any>('flow/managerPipeline').then(unwrapFlow)
