package com.ship.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ship.model.vo.RxArchiveVo;
import com.ship.model.vo.RxPrintPrepareVo;
import com.ship.model.vo.RxPrintResultVo;
import com.ship.security.model.RoleConst;
import com.ship.service.IRxPrintService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 兽用处方笺打印（农业农村部第734号《兽医处方格式及应用规范》）
 * - 打印/补录：医生、护士、管理员；
 * - 档案查询/作废：管理员（档案调阅与留痕管理）；
 * - 处方正文数据仍走 GET /record/rx/{recordId}（含顾客归属校验，顾客端可复用）。
 */
@RestController
@RequestMapping("/record/rx")
public class RxPrintController {
    private final IRxPrintService rxPrintService;

    public RxPrintController(IRxPrintService rxPrintService) {
        this.rxPrintService = rxPrintService;
    }

    /**
     * 打印预检：生成/复用编号 + 补录字段合并，返回处方笺全量数据
     * copies=1 仅第三联(畜主留存)，copies=3 三联全打
     */
    @PostMapping("/print-prepare")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public RxPrintResultVo printPrepare(@RequestBody RxPrintPrepareVo vo) {
        return rxPrintService.prepare(vo);
    }

    /** 处方笺档案分页（管理端调档/留档） */
    @GetMapping("/archive")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Page<RxArchiveVo> archive(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean printed,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int numPage,
            @RequestParam(defaultValue = "10") int pageSize) {
        return rxPrintService.archive(startDate, endDate, doctorId, keyword, printed, status, numPage, pageSize);
    }

    /** 作废处方笺编号（占号不回收，填原因） */
    @PostMapping("/archive/void/{printNo}")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public com.ship.common.Result voidBy(@PathVariable String printNo, @RequestParam String voidReason) {
        return rxPrintService.voidBy(printNo, voidReason)
                ? com.ship.common.Result.success("已作废，编号占位不回收")
                : com.ship.common.Result.error("编号不存在或已作废");
    }
}
