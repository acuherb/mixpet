package com.mix.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 打印预检出参（不动通用 Result）：处方笺全量数据 + 补充字段 + 编号
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RxPrintResultVo {
    private Integer code;
    private String msg;
    /** 处方笺打印数据（前记/正文/后记全字段，失败为 null） */
    private RxSheetVo sheet;
    /** 补充字段（编号/毛色/种类/休药期/发药人/联次/状态/打印时间） */
    private RxSupplementVo supplement;

    public static RxPrintResultVo ok(RxSheetVo sheet, RxSupplementVo supplement) {
        return new RxPrintResultVo(1, "已生成处方笺编号", sheet, supplement);
    }

    public static RxPrintResultVo fail(String msg) {
        return new RxPrintResultVo(0, msg, null, null);
    }
}
