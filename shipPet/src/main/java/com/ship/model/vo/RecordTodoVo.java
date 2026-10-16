package com.ship.model.vo;

/**
 * 诊疗待办统计（医生工作台/后台看板）：
 * 一次就诊从接诊到完结之间可能卡壳的环节
 */
public record RecordTodoVo(
        /** 接诊中的病历数（未完结） */
        long treating,
        /** 待出结果的化验单数 */
        long labPending,
        /** 待出报告的影像单数 */
        long imagePending,
        /** 待进行/待记录的手术数 */
        long surgeryPending,
        /** 待随访数 */
        long followPending
) {
}
