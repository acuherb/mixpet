# db-backup 目录说明

本目录存放**特性 SQL 脚本**（建表/加列/索引/数据修复），按日期/功能命名，**已全部在开发库执行过**。

## 使用约定

1. **新库部署**：先导基线 `pet_mix.sql`，再按**文件名字典序**执行全部特性脚本（幂等脚本可重复执行）；
2. **执行方式**：
   ```bash
   MYSQL_PWD='<密码>' mysql -h127.0.0.1 -P3306 --default-character-set=utf8mb4 -u<用户> <库名> < <脚本>
   ```
3. **字符集**：务必带 `--default-character-set=utf8mb4`（含中文的脚本在错误 charset 下会写入乱码——历史上 sys_dict 就这样乱过）；
4. 生产库迁移前先 `mysqldump` 备份；
5. 脚本命名：`feature_<日期>_<功能>.sql` 新功能 / `fix_<日期>_<问题>.sql` 数据修复 / `*_module.sql` 早期模块建表。

## 脚本索引（按功能，共 51 个特性脚本 + 1 基线）

### 基线
| 脚本 | 内容 |
|---|---|
| `pet_mix.sql` | 全库基线（建库后第一个执行；旧名 `pet_ship.sql` 为 ship→mix 替换前基线，仅存档） |

### 一、基础模块建表（早期 module 脚本）
| 脚本 | 内容 |
|---|---|
| `lab_module.sql` | 化验/检验：`lab_test`/`lab_order`/`lab_order_item` + bill 加 `fee_lab` |
| `hospital_module.sql` | 住院：`hospital_record` + bill 加 `fee_hospital`（须先于 image 执行） |
| `image_module.sql` | 影像：`image_test`/`image_order` + bill 加 `fee_image` |
| `immune_module.sql` | 免疫接种：`immune_record` + bill 加 `fee_immune` |
| `surgery_module.sql` | 手术麻醉：`surgery`/`anesthesia_record` + bill 加 `fee_surgery` |
| `package_module.sql` | 服务套餐：`service_package`/`record_package` + bill 加 `fee_package` |
| `rx_prescription_module.sql` | 国标处方笺：doctor 加 `doctor_cert`（执业资格证号） |
| `job_module.sql` | 职务/职称字典：`job` 表 + doctor 加 `job_id` + 按 auth_lv 播种 |
| `vaccine_merge_into_drug.sql` | 疫苗字典合并进药品：drug 加 3 字段、数据迁移、删 vaccine 表 |
| `doctor_department.sql` | 医生多科室：`doctor_department` 中间表 |

### 二、工作流/功能增强（2026-10-14 ~ 16）
| 脚本 | 内容 |
|---|---|
| `feature_20261014_workflow.sql` | 接诊分诊结构化：`triage_level`/体重/体温/心率/呼吸 |
| `feature_20261014_hospital_monitor.sql` | 住院每日监护：`hospital_monitor` 表 |
| `feature_20261014_refund_inventory.sql` | 退款冲正+库存台账：notice_type、drug 两列、`drug_record` 表、bill 三列、三表 `is_cancel` |
| `feature_20261014_revisit_followup.sql` | 复诊关联+随访：两表加 `origin_record_id`、建 `follow_up` |
| `feature_20261014_schedule.sql` | 医生排班：`doctor_schedule` 表 |
| `feature_20261014_surgery_consent.sql` | 手术知情同意书：`surgery_consent` 表 |
| `feature_20261014_sys_dict.sql` | 统一设置中心：`sys_dict` 表 + 6 类字典种子 |
| `feature_20261015_bill_discount.sql` | 会员折扣：bill 加 `discount` + 两个 MariaDB 生成列（优惠/实收） |
| `feature_20261015_finance_report.sql` | 财务报表：drug 加 `cost_price`（进货价） |
| `feature_20261015_member_card.sql` | 会员/次卡/积分：`member_level`/`count_card_template`/`client_count_card` |
| `feature_20261015_record_template.sql` | 电子病历模板：`record_template` 表 |
| `feature_20261015_supplier_inventory.sql` | 供应商+采购入库：`supplier` 表 + drug_record 加 3 列 |
| `feature_20261015_chat_file_webp.sql` | 聊天文件化：msg 加 `msg_type`/`msg_name` + 历史图片回填 |
| `feature_20261016_image_report_tpl.sql` | 影像报告模板：image_test 加模板字段 |
| `feature_20261016_chat_read.sql` | 聊天已读：`is_read`/`read_time` + msg_content 扩 1000 |
| `feature_20261016_client_audit.sql` | 顾客注册审核：client 加 `audit_status`（历史置通过） |
| `feature_20261016_notice_sort.sql` | 公告排序权重：notice 加 `sort` |
| `feature_20261017_ai_advice.sql` | AI 辅助建议预留字段 |
| `feature_20261017_brand_color.sql` | 品牌主题色：hospital_setting 加 `brand_color` |
| `feature_20261017_client_audit_reason.sql` | 审核拒绝原因：client 加 `audit_reason` |
| `feature_20261017_drug_dose_rule.sql` | 处方剂量规则：drug 加剂量相关字段 |
| `feature_20261017_followup_isdel.sql` | follow_up 补 `is_del` 逻辑删除 |
| `feature_20261017_record_status.sql` | 就诊状态：medical_record 加 `status`/`finish_time`/`cancel_reason` |

### 三、套餐结构化 + 计费细化（2026-10-20 ~ 22）
| 脚本 | 内容 |
|---|---|
| `feature_20261020_package_item.sql` | 套餐可选项目：`package_item` 子表（检验/影像快照） |
| `feature_20261020_bill_fee_immune.sql` | bill 补 `fee_immune` 分项（免疫计费） |
| `feature_20261020_bill_pay_by.sql` | bill 加收款人 `pay_by`/`pay_time`（操作人留痕） |
| `feature_20261020_drug_inbound_cost.sql` | 入库成本链路：drug_record 加成本字段 |
| `feature_20261020_hospital_advice.sql` | 住院医嘱单：`hospital_advice` 表 |
| `feature_20261020_is_del_default_fix.sql` | 存量表补 `is_del` 默认值（逻辑删除一致性） |
| `feature_20261021_bill_refund_reason.sql` | bill 加 `refund_reason`（退款原因独立留痕） |
| `feature_20261022_query_indexes.sql` | 高频查询索引：bill(record_id+pay_status)/client(client_tel)/medical_record(doctor_id)（幂等） |
| `feature_20261022_hospital_foster_billing.sql` | 住院分项计价+押金、医嘱临时费用；寄养按天计费+状态流转（幂等） |
| `feature_20261023_appointment_miss.sql` | 挂号爽约改状态位：appointment 加 `miss`（记录保留不删，幂等） |

### 四、数据修复
| 脚本 | 内容 |
|---|---|
| `fix_entity_update_time_20261013.sql` | 补 `doctor_department.update_time` 列（实体缺字段导致保存崩） |
| `fix_20261021_double_encoded_text.sql` | 修复 member_level/record_template 的中文双重编码乱码（latin1→utf8mb4 回转） |

## 与运行库的关系

开发中新增字段/表都以独立脚本保存并**立即执行到运行库**（`pet_mix`）；`deploy.sh` 不执行数据库迁移——新环境部署须按本索引补齐全部脚本。
