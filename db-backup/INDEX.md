# db-backup 目录说明

本目录存放**数据库基线**与**历史迁移脚本存档**。2026-10-24 起改为「单基线 + 归档」结构，新环境部署**只需一条命令**。

## 结构

```
db-backup/
├── pet_mix.sql      # 当前基线：全库 43 表 + 演示数据（2026-10-24 重压）
├── runtime/         # 管理后台「数据备份」页落盘处（gitignore，勿入库勿外传）
└── archive/         # 历史增量脚本存档（51 个，按日期/功能命名）
```

## 新环境部署（一条命令）

```bash
# 建库（utf8mb4）后：
MYSQL_PWD='<密码>' mysql -h127.0.0.1 -P3306 --default-character-set=utf8mb4 -u<用户> <库名> < pet_mix.sql
```

- 基线**含演示数据**（14 客户 / 16 药品 / 会员等级 / 供应商等），开箱即用；
- **正式上线前**按 DEPLOY.md 10.2 的 TRUNCATE 清单清场，并到后台重置演示账号密码；
- 基线为 2026-10-24 运行库全量 dump（`mysqldump --single-transaction --routines --triggers --hex-blob`），已经隔离库还原验证（43 表一致、中文完好）。

## 发版压基线约定（重要，防目录再乱）

1. **平时**：新增字段/表仍写独立脚本放**本目录根下**（命名 `feature_<日期>_<功能>.sql`），立即执行到运行库；
2. **每次发版**（或存量脚本攒到 10 个左右）：用管理后台「数据备份」或 `mysqldump` 重压一份 `pet_mix.sql`，把此前根下的脚本**整体移入 `archive/`**；
3. `archive/` 只增不移出（git 历史可追溯每一列的来历），主目录永远保持「1 基线 + 最近一批增量」；
4. `fix_*` 类一次性数据修复脚本随归档一起进 `archive/`（对新库无意义，留档备查）。

## archive/ 存档索引（51 个，2026-10-24 归档时快照）

| 分组 | 脚本 |
|---|---|
| 基础模块建表 | `lab_module` / `hospital_module` / `image_module` / `immune_module` / `surgery_module` / `package_module` / `rx_prescription_module` / `job_module` / `vaccine_merge_into_drug` / `doctor_department` |
| 工作流增强 10-14~17 | `feature_20261014_workflow` / `_hospital_monitor` / `_refund_inventory` / `_revisit_followup` / `_schedule` / `_surgery_consent` / `_sys_dict` / `feature_20261015_bill_discount` / `_finance_report` / `_member_card` / `_record_template` / `_supplier_inventory` / `_chat_file_webp` / `feature_20261016_image_report_tpl` / `_chat_read` / `_client_audit` / `_notice_sort` / `feature_20261017_ai_advice` / `_brand_color` / `_client_audit_reason` / `_drug_dose_rule` / `_followup_isdel` / `_hospital_setting` / `_medical_record_bp` / `_prescription_dose_fields` / `_record_present_illness` / `_record_status` / `feature_20261018_rx_print` |
| 计费细化 10-19~21 | `feature_20261019_bill_pay_by` / `feature_20261020_bill_fee_immune` / `_drug_inbound_cost` / `_hospital_advice` / `_is_del_default_fix` / `_package_item` / `feature_20261021_bill_refund_reason` |
| 一次性数据修复 | `fix_entity_update_time_20261013`（实体缺 update_time 列）/ `fix_20261021_double_encoded_text`（会员等级与病历模板中文双重编码回转） |

> `pet_ship.sql`（ship→mix 替换前的旧基线快照，仅 9 表）同日在 archive/ 内留档，新环境**不要**使用。
