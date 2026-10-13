import {AnesthesiaRecord} from "@/model/entiy/AnesthesiaRecord";

/**
 * 手术单（联查）
 */
export interface Surgery {
    surgeryId: number;
    recordId: number;
    petId: number;
    clientId: number;
    doctorId: number | null;
    surgeryName: string;
    surgeryType: string | null;
    anesthesiaType: string | null;
    scheduledTime: string | null;
    startTime: string | null;
    endTime: string | null;
    surgeon: string | null;
    assistant: string | null;
    status: number;
    preopNote: string | null;
    intraopNote: string | null;
    postopNote: string | null;
    fee: number;
    petName?: string;
    clientName?: string;
    doctorName?: string;
    anesthesia?: AnesthesiaRecord | null;
}

/** 新建手术单请求 */
export interface SurgeryVo {
    recordId: number;
    surgeryName: string;
    surgeryType: string | null;
    anesthesiaType: string | null;
    scheduledTime: string | null;
    surgeon: string | null;
    assistant: string | null;
    fee: number;
}

/** 手术状态：0待手术 1手术中 2已完成 3已取消 */
export const surgeryStatusText = (s: number): string =>
    ({0: '待手术', 1: '手术中', 2: '已完成', 3: '已取消'} as Record<number, string>)[s] ?? '未知'

export const surgeryStatusType = (s: number): string =>
    ({0: 'warning', 1: 'primary', 2: 'success', 3: 'info'} as Record<number, string>)[s] ?? 'info'

export const surgeryTypes = ['绝育', '骨科', '肿瘤', '外伤', '其他']
export const anesthesiaTypes = ['全身麻醉', '局部麻醉', '镇静']
