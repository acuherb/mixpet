-- ============================================================
-- 修复：实体缺 updateTime 字段导致 insert 抛反射异常（2026-10-13）
-- 适用：MySQL 8 / MariaDB
-- 背景：FieldFill.insertFill 会无条件给所有实体 set createTime/updateTime，
--       实体若缺 updateTime 字段 → MyBatis 抛
--       "There is no setter for property named 'updateTime' in 'class ...'"
--       → 整个事务回滚 → 前端看到全局兜底"系统繁忙，请稍后再试！"
-- 涉及：RecordPackage（接诊保存套餐时触发）、DoctorDepart（保存医生科室关系时触发）
-- 修复：①两实体补 updateTime 字段（代码） ②FieldFill 加 hasSetter 守卫（代码，杜绝今后再踩）
--       ③两表补 update_time 列（本脚本）
-- ============================================================
SET NAMES utf8mb4;

ALTER TABLE `record_package`
  ADD COLUMN IF NOT EXISTS `update_time` datetime DEFAULT NULL COMMENT '修改时间' AFTER `create_time`;

ALTER TABLE `doctor_department`
  ADD COLUMN IF NOT EXISTS `update_time` datetime DEFAULT NULL COMMENT '修改时间' AFTER `create_time`;
