package com.ship.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ship.model.vo.RxArchiveVo;
import com.ship.model.vo.RxPrintPrepareVo;
import com.ship.model.vo.RxPrintResultVo;

import java.time.LocalDate;

/**
 * 兽用处方笺打印服务（734号国标：编号/联次/补充字段/留痕/作废）
 */
public interface IRxPrintService {
    /**
     * 打印预检：定位处方 → 已有 VALID 复用编号 / 无则生成 RX-YYYYMMDD-XXXX（按天自增、并发防重号）
     * → 补录字段合并（补充表已有 > 本次入参 > pet/bill 既有值）→ 返回全量打印数据
     */
    RxPrintResultVo prepare(RxPrintPrepareVo vo);

    /** 处方笺档案分页（管理端） */
    Page<RxArchiveVo> archive(LocalDate startDate, LocalDate endDate, Integer doctorId,
                              String keyword, Boolean printed, String status,
                              int numPage, int pageSize);

    /** 作废处方笺编号（占号不回收） */
    boolean voidBy(String printNo, String reason);
}
