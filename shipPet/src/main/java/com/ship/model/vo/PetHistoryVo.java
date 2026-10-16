package com.ship.model.vo;

import java.time.LocalDate;
import java.util.List;

/**
 * 宠物病史速览（医生接诊时右侧边栏）：
 * 宠物档案（含过敏史/既往病史）+ 最近就诊 + 近期用药 + 近期检验异常项
 */
public record PetHistoryVo(
        String petName,
        String petVariety,
        Boolean petSex,
        LocalDate petBirthday,
        Boolean petSterilization,
        String petChip,
        /** 过敏史 */
        String petAllergy,
        /** 既往病史 */
        String petHistory,
        /** 最近几次就诊（时间倒序） */
        List<PetHistoryRecord> recentRecords,
        /** 近期用药（时间倒序） */
        List<PetHistoryRx> recentRx,
        /** 近期检验异常项（时间倒序） */
        List<PetHistoryLab> abnormalLabs
) {
}
