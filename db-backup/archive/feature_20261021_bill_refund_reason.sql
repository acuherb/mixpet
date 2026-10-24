-- 退款原因独立留痕（原实现把「操作人｜原因」拼在一个字段，粗糙且无法分开检索）
-- 同时配合「发药/退款操作人改从登录态取」的安全修复：refund_by=当前登录人，refund_reason=前端必填原因

ALTER TABLE `bill`
    ADD COLUMN `refund_reason` varchar(255) NULL COMMENT '退款原因（退款时必填）' AFTER `refund_by`;
