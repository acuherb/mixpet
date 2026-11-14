package com.mix.model.vo;

/**
 * 保存一次体检的入参。schemaJson/metaJson 由前端带模板快照与表头信息；
 * clientId 一律由后端按 petId 从库里查，不接受前端传入，防止越权挂到别人名下。
 */
public record CheckupSaveVo(
        /** 有值=修改已有体检，空=新增 */
        Integer checkupId,
        Integer templateId,
        Integer petId,
        Integer recordId,
        String schemaJson,
        String resultsJson,
        String metaJson,
        String conclusion
) {
}
