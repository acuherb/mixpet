package com.mix.model.vo;

import lombok.Data;

/**
 * 影像报告录入请求
 */
@Data
public class ImageReportVo {
    /** 申请单id */
    private Integer orderId;
    /** 影像报告内容 */
    private String result;
    /** 报告文件(影像图/PDF)路径，可选 */
    private String reportFile;
    /** 结构化报告：影像所见 / 影像诊断 */
    private String findings;
    private String impression;
    /** PACS / DICOM 元数据 */
    private String dicomUid;
    private String dicomSeries;
    /** 原图附件文件名 JSON 数组 */
    private String imageFiles;
}