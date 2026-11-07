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

// 诊疗流（闭环状态机）脊柱：单一接口取回全部环节快照
export const reqDoctorPipeline = (): Promise<DoctorFlowVo> =>
  reqApi<string, DoctorFlowVo>('flow/doctorPipeline')
