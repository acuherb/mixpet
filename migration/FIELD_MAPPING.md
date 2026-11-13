# PetSystem → mixpet 字段级映射表（step 0 交付物）

> 生成日期：2026-11-12。数据源：
> - `petsinit_schema.tsv`：PetSystem 模板库（`初始库.bak` 还原至本地 Docker SQL Server `mssql-pets`，库名 petsinit，2016-10 版）全量导出，**148 表 / 1982 字段**，含 MS_Description 中文注释。⚠️ 这是模板库 schema；生产库可能新增少量表/字段，切换日导入生产 .bak 后需重跑导出比对。
> - `pet_mix.sql`：mixpet 基线（43 表，MariaDB 11.8）。
> - mixpet 后端形态已确认：**Spring Boot 3 (Java) + MariaDB (pet_mix 库) + 本地磁盘文件存储**；迁移工具建议写成独立 Java/Python ETL：SQL Server(jTDS/pymssql) → MariaDB，走 mixpet Service 层校验或直接 SQL + 事后试算。

## 分类口径

- **A 直接迁**：目标表存在，数据可近乎原样搬（映射后 INSERT）。
- **B 需转换**：结构差异大，需逐字段映射 + 转换规则（金额/状态码/主键重排）。
- **C mixpet 缺失**：目标无对应表，需先补建表与功能（见 GAP_LIST.md）。
- **D 不迁**：系统内部表/历史遗留/已被 mixpet 机制替代。

## 一、表级映射总表（148 → 43）

### A/B：核心业务数据（迁移主战场）

| PetSystem 表 | 说明 | mixpet 目标 | 类别 | 备注 |
|---|---|---|---|---|
| emember | 客户/会员主档(36列) | client + member_level | B | 见字段映射 §2.1 |
| epet | 宠物(20列) | pet | A/B | 见 §2.2 |
| epetcase | 就诊/病历主档(32列) | medical_record + appointment | B | 见 §2.3 |
| epetcaseinvoice | 收费单(20列,4组支付方式+找零) | bill | B | 见 §2.4 |
| epetcaseinvoiceitem | 收费明细(19列) | bill 各 fee_* 分项 | B | mixpet 无明细表，按 LINK_TYPE 归入分项费用 |
| epetcaseprescription / +item | 处方(15/27列) | prescription | B | 见 §2.5 |
| epetcasecheck / +item / +itemresult | 化验(13/38/6列) | lab_order + lab_order_item | B | 参考值在 epbmaster，转 lab_ref_range |
| epetcaseoper / +item / opersleep* | 手术+麻醉记录(19/19/34/9/20列) | surgery + anesthesia_record | B | |
| epetcaseinhos / +item / +log | 住院(33/14/21列) | hospital_record (+advice/monitor) | B | |
| epetvaccination / +types | 免疫(13/3列) | immune_record | A/B | DATE_NEXT→next_time |
| epetcasesale / +item | 零售(13/14列) | bill(fee_drug) | B | 无病历的消费并入 bill |
| estocks | 库存余量(7列) | drug.stock | B | 见 §2.6 |
| estockin / +item / +paylist | 入库(13/7/6列) | drug_record(change_type=1) | B | 批号/效期/成本价可平移 |
| estockout / +item | 出库(15/7列) | drug_record(change_type=2/3) | B | |
| epbmaster | 商品主档(76列) | drug | B | 见 §2.7，品类宽于 drug |
| ecmvendor | 供应商(16列) | supplier | A | |
| ecmlevel | 会员等级(6列) | member_level | B | COST_ALL 阈值→等级 |
| ecmmemberdc + emember_dc(+_detail,+2) | 储值套餐+储值卡+充值流水 | client.client_balance | **C(流水缺)** | 见 §2.8，硬约束核心 |
| ecmmembersa + emember_sa(+_detail) | 疗程卡(套餐+实例+消费流水) | client_count_card | C(部分) | 见 GAP P1 |
| ecmmemberes + emember_es(+_detail) | 限次限额卡 | client_count_card | C(部分) | 同上 |
| ehairingcard / +details | 美容次卡+使用流水 | client_count_card | B | RES_TIMES→剩余次数 |
| emember_gift | 会员赠品(8列) | — | C | 量少可人工补录 |
| epetcasehair / +item | 美容工单(13/14列) | foster(美容口径) | B | |
| epetcasecare / +item | 洗护/护理(14/15列) | foster / bill | B | |
| epetfosterage | 寄养(11列) | foster | B | 笼位 CAGE_ID 见 C 类 |
| epetcasejiancha_eyes/pifu/tijian/yachi | 专科检查模板(15/40/24/10列) | — | C | 可转 record_template 或 sys_dict |
| epetcaseother1a/b/c(+_en) | 自定义检查模板(36/33/22列) | — | C | 同上，按门店实际使用量取舍 |
| epetcasetypex/typeb(+item,+itemresult) | 特检X/B(27/19列) | image_test/image_order | B | 影像类 |
| epetcasepathology / +item | 病理(17/14列) | lab_order(挂化验) | B | 或补独立模块 |
| epetcasemicrobial / +item | 微生物/药敏(104/14列) | lab_order | B | 104 列多为固定药敏项目 |
| epetcaseph | pH 检查(7列) | lab_order | B | |
| epetcasequicktemp | 快检(17列) | lab_order | B | |
| ecminitdiag / ediagnoseitems / epbdiagnosetype | 初诊/诊断字典(5/6/5列) | diagnosis_dict | B | |
| ecmcompany | 单位客户(12列) | client | B | 作为客户迁入 |
| ecmemployee | 员工(73列,含权限) | doctor | B | 只迁执业相关字段 |

### D：不迁（被 mixpet 机制替代 / 系统内部）

| PetSystem 表 | 处理 |
|---|---|
| esysuser / esysauthorize / esysmenu / esyssetting / esystemlog / elogmessage | mixpet 自有 doctor/audit_log/权限体系 |
| epbprefix / +classify / +classify2 / +range | 单据号规则：mixpet 自增+业务前缀，老单号作为原号字段保留 |
| esms / esmslib / esmsmodule / esmstemp / esmsweixin | 短信/微信通道：切换窗口重配（见 GAP P0-3），历史不迁 |
| emessage / emessagelog / emessagetype / emembercallcenter(+log) | 站内消息/呼叫中心：mixpet 有 msg；历史消息不迁 |
| eknowledge | 知识库：不迁（可选人工搬运） |
| euploadimages / euploadpubpath / esyscopypic | 图片文件：随就诊历史打包迁移，指向老系统只读挂接 |
| erptdayfinish(123列) / erptaccm / erptaccmdetail / esyscash / ereqcost | 历史财务报表：老系统只读查，不迁结构 |
| estockcheck / +item, estockmove / +item, estockintype, estockouttype, intermanu*, muti*order* | 盘点/调拨/加工/调拨单：迁移时点只需期末余量（estocks），过程单据留老系统 |
| hairworkflow / eselectgroup* / epbmaster_rela / epbmaster_weight / epbmastercheckvalue / epbmasteritem / ecmcaseclose / epetcasereturnmember / epetcasecheckvalue / ecmpartaddress / ecmstocktype / ecmstock / ecmuom / ecmspec / ecmpackage / ecmdepartment / ecmteam / ecmmemberdc2 / epetbreed / epetspecies / epetsex / ecmlevel 上层冗余 | 字典/辅助：按需转 sys_dict，或迁移工具内置常量 |
| epetjianyi / +item / +msgitem | 建议模板：可转 record_template（P2 可选） |
| ememberbank | 银行账户(10列)：量少，人工处理 |

## 二、核心表字段级映射

### 2.1 emember → client

| emember | → client | 转换 |
|---|---|---|
| MEMBER_ID (nvarchar, 老会员号) | — (新自增 id) | 保留到新字段 `legacy_member_no`（建议加列，用于对账与追溯） |
| MEMBER_ID_SHOW | — | 同上，可并入 legacy_member_no |
| FULLNAME | client_name | 直接 |
| SEX | — | client 无性别字段，可入 remark 或加列 |
| MOBILE / TEL_HOME / TEL_OFFICE | client_phone | 优先 MOBILE；其余入 remark |
| IDNO / ADDRESS / EMAIL / QQ / WEIXIN | — | 建议加列 id_no / address / wechat，否则 remark |
| PASSWORD | — | 不可逆：置空，走注册/重置流程 |
| COST_ALL | — | 历史累计消费：迁移时写入 remark 或新列（报表基线） |
| M_LEVEL | member_level | 映射到 mixpet 等级（见 GAP P1-1） |
| IS_A/B/C/D + DISCOUNT_A/B/C/D | member_level.discount | **多身份多折扣 → 单一等级折扣**，需业务决策（取医院会员为准 or 最高权益） |
| REGDATE / MDF_DATE | create_time / update_time | 直接 |
| MEMO | remark | 直接 |

### 2.2 epet → pet

| epet | → pet | 转换 |
|---|---|---|
| PET_ID / PET_ID_SHOW | 新自增 id + legacy_pet_no | 同会员号策略 |
| FULLNAME | pet_name | 直接 |
| SPECIES / BREED | pet_species / pet_breed | 需对照 mixpet 字典（猫/犬/其他） |
| SEX | pet_sex | 老系统字符值→mixpet 编码 |
| AGE / BIRTHDAY / BIRTHDAY_TRUE | pet_birthday | 优先 BIRTHDAY_TRUE；无则按 AGE 倒推 |
| WEIGHT | pet_weight | 直接 |
| EATDRINK / LIVINGSPACE / REASON / DESCRIPTION / MEMO / CUTDATE(绝育) | 健康档案字段 | 并入 pet 备注/健康描述；CUTDATE 若 mixpet 无字段入健康描述 |
| MEMBER_ID | client_id | 按 §2.1 新旧映射表关联 |

### 2.3 epetcase → medical_record / appointment

| epetcase | → 目标 | 转换 |
|---|---|---|
| CASE_ID (就诊号) | 新 record_id + legacy_case_no | |
| PET_ID / DOCT_ID | pet_id / doctor_id | 映射表关联 |
| DATETIME | create_time（appointment_date 同值） | 一条 epetcase 生成 1 病历 + 1 历史挂号单(is_suc=1) |
| TYPE_ID(挂号种类,指向epbmaster) / REG_COST / REG_CHARGE / PAY_TYPE | appointment/bill.fee_register | 挂号费并入对应 bill |
| STATUS (P挂号→A进行→C完成) | record_status | C→已完成；P/A 未完成单据切换日极少，人工处理 |
| SYMPTOM / DETAILS / NOTICE / DOCT_ADVICE / MEMO / MEMO_DOCT | 主诉/检查/医嘱/备注 | 长文本直接；注意 mixpet record_template 风格 |
| TEMP_ZS/ZD/YZ(体温) | medical_record 体温字段(bp 类) | 按现有列名对齐 |
| WEIGHT / EATDRINK / LIVINGSPACE / REASON | 病历体重等 | |
| INIT_DIAG(初诊) | diagnosis | 对照 diagnosis_dict |
| PH / PH_TYPE / KE / FROM_DOCT_ID / CLOSE_MEMO / ACC_STATUS | 转诊/会诊/结案 | 保留原文入 remark（低频） |

### 2.4 epetcaseinvoice(+item) → bill

| epetcaseinvoice | → bill | 转换 |
|---|---|---|
| INVOICE_NUM | legacy_bill_no | mixpet 单号策略重排 |
| LINK_ID(就诊号) | record_id | |
| MEMBER_ID / PET_ID / CREATE_EMP_ID / COST_EMP_ID(钱算谁的) | client_id / pet_id / pay_by | COST_EMP_ID≠MEMBER_ID 的代付场景入 remark |
| TYPEKEY(医疗/美容/销售) | — | 决定 fee_* 归项 |
| REV_COST(应收) / CHANGE_COST(找零) | total_money / discount_money | 注意实收=PAY_COST1..4-CHANGE_COST |
| PAY_TYPE1..4 + PAY_COST1..4 | pay_type(0余额/1现金) | **mixpet 只有 2 种支付**：现金/余额按主支付映射；微信/刷卡历史单统一记"现金"或补 pay_by 备注（见 GAP P0-3） |
| COST_STATUS / STATUS / INHOSACCSTATUS | pay_status | 已收→1；未收→0（对账"未结账单数"来源） |
| item: PART_NUM / QTY_ORDER / ORDER_PRICE / STD_COST | fee_drug / fee_lab / fee_surgery… | 按 LINK_TYPE + 商品 IS_STOCK/GROUP_ID 归项；REFUND_PRICE 生成退款记录口径 |

### 2.5 epetcaseprescription(+item) → prescription

| 老字段 | → 新字段 | 转换 |
|---|---|---|
| PRIS_ID / LINK_ID(CASE_ID) | 新 prescription_id + record_id | |
| item: PART_NUM / 用法剂量字段(27列) | prescription 明细 | mixpet 有 prescription_dose_fields；用法文本直迁 |
| 发药状态 | bill.dispense_status | 老系统发药即出库，迁移时点前已发的不补库存流水 |

### 2.6 库存（切换时点：日结后）

| 老表 | → 新 | 规则 |
|---|---|---|
| estocks(QTY_ON_HAND, QTY_MIN, QTY_MAX) | drug.stock / warning_stock | **只迁期末余量**，不迁过程单；明细对账用 estocks vs drug 汇总 |
| estockin(+item 批号/效期/成本) | drug_record(1入库) | 切换前最后一次入库留档；mixpet 单品无批次维度 → 批号/效期入 note（效期预警口径见 GAP P1-2） |
| estockout(+item COST_PRICE) | drug_record(2出库) | 历史单不迁，只留老系统查询 |

### 2.7 epbmaster → drug（76 列，重点）

| 老字段 | → drug | 说明 |
|---|---|---|
| PART_NUM | legacy_part_no | 老料号必须保留（收费明细/化验结果都指向它） |
| DESCRIPT / SPEC / UOM / PACKAGE | drug_name / spec / unit | |
| COMPONENT / APPLICATION / ATTENTION / PROP / UNEFFECT / TOBOO / EACH_OTHER | 成分/用法/注意事项 | drug 字典字段按现有列对齐，放不下入 remark |
| MANUFACTURER / BARCODE / ISBN | 厂家/条码 | |
| QTY_ON_HAND / QTY_MIN | stock / warning_stock | 与 estocks 核对（两处都有余量，以 estocks 为准，epbmaster.QTY_ON_HAND 做第二口径） |
| STD_COST / COST_PRICE / STD_COST_A/B/C/SEL(ABC售价) | price / cost_price | **价格体系转换重点**：ABC 三档会员价 vs mixpet 单价+折扣 |
| IS_STOCK(Y/N) | — | IS_STOCK=N 为非库存项目（挂号费/化验项/手术项目等）→ 不入 drug，转服务项（fee_lab/surgery 口径或 sys_dict） |
| IS_HAIRING_CARD / CARD_TIMES | count_card_template | 美容次卡商品转次卡模板 |
| DOG/CAT/DOG_Y/CAT_Y…(参考值) | lab_ref_range | **化验参考值藏在这里**，转换到 mixpet 独立参考范围表 |
| CHANGE_PRICE / IS_USE_DISCOUNT / COMMISSION_RATE | — | 折扣开关见 Web.config 口径；提成见 GAP P1-4 |
| RX_OTC / ALERT_MSG / ALERT_DAYS / WARR_MN / DATE_EXPIRATION | 库存预警/效期 | mixpet 有 warningStock + 近效期预警 |

#### 2.7.1 品类迁移映射规则（P0-4 定案 2026-11-13）

PetSystem 的"项目"并非都要落 drug——mixpet 已有专属项目字典，映射按 IS_STOCK 分流：

| epbmaster 料号类型 | 判定依据 | → mixpet 落点 |
|---|---|---|
| 药品/疫苗/驱虫/保健品 | IS_STOCK=Y 且 TYPE2/TYPE3/PROP 含关键词 | drug（drug_type=疫苗/驱虫/保健，其余药品按 西药/中成药/外用） |
| 用品/零食/玩具等零售商品 | IS_STOCK=Y 且非药 | drug（drug_type=**用品**，字典值已落库）；判不了 → **其他** |
| 化验项目 | IS_STOCK=N，GROUP_ID/TYPE2 属检验 | lab_test（test_name/test_price 平移） |
| 影像/特检项目 | IS_STOCK=N，属 X光/B超/CT | image_test（group_name 对应分组） |
| 手术项目 | IS_STOCK=N，TYPE2 属手术 | 不迁为商品，手术费走 fee_surgery 手工录入 |
| 美容/护理服务项 | IS_STOCK=N，TYPE2 属美容 | 不迁为商品，走 foster/bill 口径 |
| 挂号种类 | epetcase.TYPE_ID 指向的料号 | 不迁，挂号费 = bill.fee_register（金额并入 epetcase.REG_COST 映射） |

- 收费**逐项明细**不再强求：对账按 bill 金额口径（§3），明细留老系统只读查。统一服务项字典降级为 P2 可选。

#### 2.7.2 化验参考值转换规则（P0-5 定案 2026-11-13，实现归 step 2 ETL）

老参考值藏在 epbmaster 列里，目标表 `lab_ref_range`（species/sex/age_min_month/age_max_month/ref_low/ref_high/critical_low/critical_high/ref_text/active）：

| epbmaster 列 | → lab_ref_range 行 |
|---|---|
| DOG（正常狗参考值） | species=犬，成年段 |
| DOG_Y（小狗） | species=犬，幼年段（age_max_month=12） |
| DOG2/DOG3/DOG4 | 犬，扩展年龄段（按列序拆段，值非空才生成行） |
| CAT / CAT_Y / CAT2..CAT4 | 同上，species=猫 |
| RABBIT / HAMSTER / TOTORO | species=兔/仓鼠/龙猫 |

- 值格式 `"low-high"`（如 3.5-12.5）拆 ref_low/ref_high（decimal 12,4）；纯文本/单值写 ref_text；
- epbmastercheckvalue.DESC（料号附注）追加到 ref_text；
- lab_test.ref_range（文本汇总）同步生成一行兜底值，保证旧项目列表显示不空。

### 2.8 会员储值线（硬约束：三步试算）

```
ecmmemberdc(储值套餐定义: DC_A/B/C/D 各身份折扣) 
  → emember_dc(会员持有的储值卡实例) 
    → emember_dc_detail(充值/消费流水: COST_TYPE, COST, PAY_TYPE, INVOICE_NUM)  ★对账依据
ecmmemberdc2(套餐按商品前缀折扣) → emember_dc2(实例前缀折扣)
```

| 迁移动作 | 说明 |
|---|---|
| 余额 | emember_dc 各卡 DC 值/余额合计 → client.client_balance（**需从 detail 逐笔重算校验 = 卡面余额**） |
| 流水 | mixpet **无储值流水表** → 必须先建 `client_balance_log`（change_type, amount, before/after, ref_bill, operator, time），历史流水压缩为一条期初充值记录 + 明细留老系统（见 GAP P0-1） |
| 折扣 | ecmmemberdc2 前缀折扣 → mixpet 无此机制，取舍见 GAP P1-1 |
| 对账 | 会员数、余额合计、近 N 月充值/消费合计，新旧一致才放行 |

## 三、对账单口径（正式迁移放行条件）

1. **会员数**：emember 有效行（排除测试号 MDF_ID='del' 类）= client 行数；
2. **储值余额合计**：Σ(emember_dc 卡余额) = Σ(client_balance)（分毫不差）；
3. **库存 SKU 数与金额**：Σ(estocks.QTY_ON_HAND, IS_STOCK=Y) 与 Σ(drug.stock)，金额=Σ(数量×COST_PRICE)；
4. **未结账单数**：epetcaseinvoice.COST_STATUS≠已收 = bill.pay_status=0；
5. **宠物/病历行数**：epet / epetcase 与 pet / medical_record 行数一致（抽样 10 份病历字段级比对）。

## 四、Web.config 业务规则口径搬运清单（约 80 开关，重点 12 个）

| 开关 | 含义 | mixpet 落点 |
|---|---|---|
| AccRcvStatus / InHosAccStatus / RegisterStatus | 应收/住院记账/挂号收费模式 | bill 生成时机 |
| AccMode / CheckOutRound / CheckCostItemSum / CostPriceCalcMode / CostAllMode | 收费模式/四舍五入/成本合计/成本核算 | BillServiceImpl 计价逻辑 |
| UseAccDiagDiscount / UseAccSaleDiscount / UseAccHairDiscount / UseAccCareDiscount | 各业务会员折扣开关 | member_level 折扣应用范围 |
| InHosYfkLimit / InHosZkkLimit / YfkYePayLimit | 住院预收款/折扣款/余额支付上限 | hospital_record 记账（mixpet 住院收费待补） |
| CheckOutPaperMode / PrisPrintMode / PrisUseNorName | 单据打印模式 | 打印模板（处方笺已有） |
| PetMemberSameID / AutoMemberPetIdMode / PetSpeciesLinkBreed | 编号/宠物品种联动 | 编号策略 |
| ActualLinkQty / ActualLinkWeight | 实际数量/体重联动 | 录入联动 |
| StockWarrExpired / ALERT_DAYS | 库存效期预警 | drug 近效期预警 |
| YZFJ_Rate / YckAutoSms / HairAutoSms | 住院附加率/自动短信 | mixpet 暂无短信通道 |
| EPayWx* / EPayZfb* | 微信/支付宝通道参数 | 切换窗口配置（GAP P0-3） |
| WX_NOTICE_TEMPLATE_ID_XF/YCKCZ/ALERT/JCFK | 消费/余额充值/提醒/检查反馈模板 | 消费/余额充值/提醒/检查反馈模板 | 切换窗口配置（GAP P0-3） |
| HairWorkFlow | 美容工作流开关 | foster 流程 |
