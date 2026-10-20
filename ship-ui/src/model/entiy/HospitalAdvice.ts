/**
 * 住院医嘱单：住院期间的每日用药/治疗/护理计划
 * status：1待执行 2执行中 3已停嘱 4已完成
 */
export interface HospitalAdvice {
    adviceId?: number;
    hospitalId: number;
    recordId: number;
    doctorId?: number | null;
    doctorName?: string | null;
    /** 类型：1药品 2治疗 3护理 */
    itemType: number;
    drugId?: number | null;
    itemName: string;
    dose?: string | null;
    doseUnit?: string | null;
    /** 频次：QD/BID/TID/QID/持续 */
    frequency?: string | null;
    /** 途径：口服/注射/外用/静脉 */
    route?: string | null;
    days?: number | null;
    /** 开始日期 YYYY-MM-DD */
    startDate?: string | null;
    remark?: string | null;
    status?: number;
    stopReason?: string | null;
    stopTime?: string | null;
    stopBy?: string | null;
    createTime?: string | null;
}
