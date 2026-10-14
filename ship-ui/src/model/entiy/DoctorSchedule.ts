/**
 * 医生排班
 */
export interface DoctorSchedule {
    scheduleId?: number;
    doctorId: number;
    doctorName?: string | null;
    departmentId?: number | null;
    /** 星期：1周一 .. 7周日 */
    weekday: number;
    /** 时段：1上午 2下午 */
    period: number;
    /** 可约号源数 */
    maxCount?: number | null;
    active?: boolean | null;
}

export const weekdays: { v: number; label: string }[] = [
    {v: 1, label: '周一'}, {v: 2, label: '周二'}, {v: 3, label: '周三'},
    {v: 4, label: '周四'}, {v: 5, label: '周五'}, {v: 6, label: '周六'}, {v: 7, label: '周日'}
]

export const periodText = (p: number): string => (p === 1 ? '上午' : '下午')
