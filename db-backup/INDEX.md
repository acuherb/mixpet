# db-backup 目录说明

本目录存放**特性 SQL 脚本**（建表/加列/索引/数据修复），按日期命名，**已全部在开发库执行过**。

## 使用约定

1. **新库部署**：按文件日期顺序全部执行（幂等脚本可重复执行）；
2. **执行方式**：
   ```bash
   MYSQL_PWD='<密码>' mysql -h127.0.0.1 -P3306 --default-character-set=utf8mb4 -u<用户> <库名> < <脚本>
   ```
3. **字符集**：务必带 `--default-character-set=utf8mb4`（含中文的脚本在错误 charset 下会写入乱码）；
4. 生产库迁移前先 `mysqldump` 备份。

## 脚本索引（按功能）

### 建表 / 结构新增
| 脚本 | 内容 |
|---|---|
| `feature_20261020_hospital_advice.sql` | 住院医嘱单 `hospital_advice` |
| `feature_20261020_package_item.sql` | 服务套餐项目关联 `package_item` |
| `feature_20261020_drug_inbound_cost.sql` | 入库台账 `drug_record.cost_price` |
| `feature_20261020_bill_fee_immune.sql` | 账单疫苗费列 `bill.fee_immune` |
| `feature_20261020_bill_pay_by.sql` | 账单收款人 `bill.pay_by` |
| `feature_20261021_bill_refund_reason.sql` | 退款原因 `bill.refund_reason` |
| `feature_20261022_query_indexes.sql` | 高频查询索引补齐（幂等） |

### 数据修复
| 脚本 | 内容 |
|---|---|
| `fix_20261021_double_encoded_text.sql` | 会员等级/病历模板中文双重编码回转 |
| `feature_20261020_is_del_default_fix.sql` | 4 张表 `is_del` 默认值 0 + 存量 NULL 找回 |
| `feature_20261015_chat_file_webp.sql` | 聊天文件 webp 支持 |
| `feature_20261016_chat_read.sql` | 聊天已读 |
| `feature_20261016_client_audit.sql` | 顾客注册审核 |

### 其他
- 其余为 2026-10-15 之前的初始化/历史特性脚本（医生科室、排班、寄养、会员、次卡等）。

> 提示：本目录不进 docker 镜像、不参与运行时；`git log --oneline -- db-backup/` 可查每个脚本的业务背景。
