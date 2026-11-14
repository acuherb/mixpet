#!/bin/bash
# 幂等补齐数据库增量迁移（P0/P1/P3/P4），供本地开发 / 旧库升级 / 从备份恢复后使用。
# 背景：mapper 已引用 P3/P4 临床列，旧库没跑迁移时 /flow/doctorPipeline 会抛
#       "Unknown column 'r.version'"，被兜底成「系统繁忙，请稍后再试！」，诊疗流只剩一条错误事件。
# 安全性：脚本只 ADD COLUMN / CREATE TABLE IF NOT EXISTS / INSERT IGNORE，不删改任何既有数据；
#         每个脚本自身用 information_schema 判存在，可重复执行。
# 用法：bash dev-tools/db-migrate.sh
set -e

PROJ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

for f in "$HOME/.env" "$PROJ/.env"; do
  if [ -f "$f" ]; then
    echo "加载环境变量: $f"
    set -a
    # shellcheck disable=SC1090
    source "$f"
    set +a
    break
  fi
done
if [ -z "${DB_USER:-}" ] || [ -z "${DB_PASSWORD:-}" ]; then
  echo "缺少 DB_USER / DB_PASSWORD：请复制 .env.example 为 .env 并填入真实值" >&2
  exit 1
fi
DB_NAME="${DB_NAME:-pet_mix}"

SCRIPTS=(
  feature_20261107_p0_payment_audit.sql
  feature_20261107_p1_appointment_slot.sql
  feature_20261107_p3_clinical.sql
  feature_20261107_p4_clinical.sql
  feature_20261113_balance_log.sql
  feature_20261113_service_items.sql
  feature_20261113_level_discount.sql
  feature_20261113_wechat.sql
  feature_20261113_lab_ref_range.sql
  feature_20261113_lab_panel.sql
  feature_20261113_prescription_days.sql
  feature_20261113_quarantine.sql
  feature_20261113_legacy_keys.sql
  feature_20261114_checkup.sql
)
for s in "${SCRIPTS[@]}"; do
  f="$PROJ/db-backup/$s"
  if [ ! -f "$f" ]; then
    echo "❌ 缺少迁移脚本：$f" >&2
    exit 1
  fi
  echo "→ 执行 $s"
  MYSQL_PWD="$DB_PASSWORD" mysql -u "$DB_USER" "$DB_NAME" < "$f"
done

echo "校验关键表与列..."
MISSING=$(MYSQL_PWD="$DB_PASSWORD" mysql -u "$DB_USER" -N "$DB_NAME" -e "
SELECT CONCAT('列:', c.tbl, '.', c.col) FROM (
  SELECT 'bill' tbl,'pay_time' col UNION ALL SELECT 'bill','refund_money' UNION ALL
  SELECT 'medical_record','version' UNION ALL SELECT 'medical_record','differential_diagnosis' UNION ALL
  SELECT 'medical_record','diagnosis_basis' UNION ALL SELECT 'medical_record','diagnosis_code' UNION ALL
  SELECT 'medical_record','diagnosis_code_system' UNION ALL SELECT 'medical_record','review_status' UNION ALL
  SELECT 'medical_record','review_note' UNION ALL SELECT 'medical_record','signed_by' UNION ALL
  SELECT 'lab_order','specimen_type' UNION ALL SELECT 'lab_order','critical_handled' UNION ALL
  SELECT 'lab_order_item','ref_low' UNION ALL SELECT 'image_order','findings') c
WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS x
                  WHERE x.TABLE_SCHEMA='$DB_NAME' AND x.TABLE_NAME=c.tbl AND x.COLUMN_NAME=c.col)
UNION ALL
SELECT CONCAT('表:', t.name) FROM (
  SELECT 'audit_log' name UNION ALL SELECT 'medical_record_revision' UNION ALL SELECT 'lab_ref_range'
  UNION ALL SELECT 'lab_result_revision' UNION ALL SELECT 'client_notification' UNION ALL SELECT 'diagnosis_dict') t
WHERE NOT EXISTS (SELECT 1 FROM information_schema.TABLES x
                  WHERE x.TABLE_SCHEMA='$DB_NAME' AND x.TABLE_NAME=t.name);") || {
  echo "❌ 迁移校验查询失败（数据库不可达或权限不足），无法确认 schema 完整性" >&2
  exit 1
}

if [ -n "$MISSING" ]; then
  echo "❌ 仍有缺失："
  echo "$MISSING"
  exit 1
fi
echo "✅ 数据库迁移齐全（$DB_NAME），诊疗流相关查询可正常执行"
