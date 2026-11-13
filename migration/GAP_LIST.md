# PetSystem → mixpet 模块差距清单（step 0 交付物，配套 FIELD_MAPPING.md）

> 生成日期：2026-11-12。基于：PetSystem 模板库 148 表 schema + Web.config 口径 + mixpet 43 表/后端模块盘点。
> 结论先行：**数据结构层面 mixpet 缺 6 块（P0），功能层面缺 8 块（P0/P1）**；病历/化验/影像/处方/免疫/手术/住院/挂号等临床主线 mixpet 已覆盖且体验更好。

## P0 —— 迁移硬阻塞（不补无法切换）

### P0-1 会员储值流水与对账能力
- PetSystem：emember_dc_detail（充值/消费逐笔流水，含 PAY_TYPE、INVOICE_NUM 回溯）。
- mixpet：只有 `client.client_balance` 一个余额数字，无流水表；充值/扣款无台账，**无法出对账单**（简报硬约束①）。
- 动作：建 `client_balance_log`（change_type/amount/balance_before/balance_after/ref_type/ref_id/operator/create_time），改造 recharge 与 BillServiceImpl 扣减处同步写流水；迁移工具把历史流水压缩为期初一条（明细留老系统可查）。

### P0-2 欠款/挂账
- PetSystem：epetcaseinvoice.COST_STATUS 未收单据 + InHosYfkLimit/InHosZkkLimit 住院记账上限，欠款是常态业务。
- mixpet：bill 只有 pay_status 0/1，无挂账台账、无信用额度、无欠款催收视图。
- 动作：最小方案 = bill 增加"挂账"支付形态 + 仪表盘未收款列表（已有 pay_status=0 口径可复用）；住院记账上限用 hospital_setting 配置。

### P0-3 微信支付 + 微信模板消息 —— ✅ 骨架完成（2026-11-13），参数留配置位待接入
- PetSystem：EPayWx*/EPayZfb* 支付通道 + WX_NOTICE_TEMPLATE_ID_*（消费 XF/余额充值 YCKCZ/提醒 ALERT/检查反馈 JCFK）+ esmsweixin。
- 已落地（全链路旁路设计，`mix.wx.enabled=false` 时零影响）：
  - **支付**：WeChatPayService（JSAPI 预下单/查单/退款，官方 SDK wechatpay-java 0.2.14，懒加载证书配置，占位参数不影响启动）；WxPayController（`POST /wx/pay/prepay/{billId}` 顾客自助支付 + `POST /wx/pay/notify` 微信回调 SDK 验签解密）；BillServiceImpl.payByWeChat CAS 幂等收款（payType=现金口径、落 wx_transaction_id、重复通知安全、积分照发）；商户单号落 bill.wx_out_trade_no（查单/退款/对账用）。
  - **模板消息**：WeChatTemplateService（access_token 内存缓存 + 4 类模板：挂号成功/就诊提醒/缴费提醒/免疫到期提醒；数据 key 用通用模板字段，接入真实模板后微调）；4 个业务挂点全部 try-catch 旁路：AppointmentServiceImpl.reserve（挂号成功）、MedicalRecordServiceImpl（接诊提醒+缴费提醒）、ImmuneRecordServiceImpl.saveImmune（免疫提醒）。
  - **openid 绑定**：client.client_openid 列 + WxAuthController（`GET /wx/auth/url` 登录态取授权跳转、`GET /wx/auth/callback` 回调落库，state 带 HMAC 签名防伪造绑定）；SecurityConfig 放行两个微信服务器回调。
  - **配置**：application.yml `mix.wx` 块全参数占位（支持 WX_* 环境变量），含四步接入清单注释。
- 接入时补：①真实商户参数+4 个模板 ID+enabled=true；②H5 支付页（WeixinJSBridge 调起 prepay 返回参数，公众号网页授权域名配置）；③bill 退款联动 createRefund（仅微信支付账单走微信原路退，退款回调事件接线）；④模板消息数据 key 按实际模板调整。

### P0-4 商品主档品类远宽于 drug（epbmaster 76 列）——✅ 已基本解除（2026-11-13）
- 复核结论：mixpet 已有 lab_test（含单价）/image_test 专属项目字典，PetSystem 非库存服务项大都有家；真缺口只是"非药品零售商品"的分类承接。
- 已完成：sys_dict.drug_type 新增 **用品/其他** 两个值（feature_20261113_service_items.sql，已入 db-migrate.sh 并落运行库；y-drugList 下拉自动读取字典，无需改 UI）。
- 完整映射规则定案：见 FIELD_MAPPING §2.7.1。
- 收费逐项明细不再强求（对账按 bill 金额口径）；统一服务项字典降级 P2。

### P0-5 化验参考值在商品主档里——✅ 规则定案（2026-11-13），实现归 step 2 ETL
- lab_ref_range 表结构确认（species/年龄段/ref_low/ref_high/ref_text），与 epbmaster 参考值列完全对得上。
- 转换规则已写死：FIELD_MAPPING §2.7.2（DOG/CAT 系列 → 按物种+年龄段拆行，"low-high"拆上下限，非数值写 ref_text）。
- 迁移工具开发时按此规格实现转换器，无遗留决策。

### P0-6 客户身份折扣体系（IS_A/B/C/D 四身份 × 三档价）——✅ 已落地（2026-11-13）
- 拍板方案落地：等级折扣（member_level.discount）之外，新增 **member_level_discount** 表按商品类别(drug_type)配置类别折扣（feature_20261113_level_discount.sql，已入 db-migrate.sh 并落运行库）。
- 应用点：开方时按 drug.drug_type 查类别折扣率**折进处方净单价**（MedicalRecordServiceImpl 初始/追加开方两处），bill.discount 仍只放等级折扣 → real_money 生成列/退款/报表零改动，两类折扣自然叠加。
- 管理：z-memberLevel 新增「类别折扣」编辑弹窗；PetSystem 多身份按拍板取最高权益迁入，原身份记 remark。

## P1 —— 切换后 3 个月内需补齐（期间走老系统只读兜底）

| # | 差距 | PetSystem 依据 | 说明 |
|---|---|---|---|
| 1 | 疗程卡/套餐卡（SA）与限次限额卡（ES） | ecmmembersa/es + 实例 + 流水 | mixpet count_card 只有"次数"概念，无按商品范围(CLASSIFY_RANGE)限定的套餐卡；可用 count_card 粗代 |
| 2 | 多仓库/调拨/盘点 | ecmstock, estockmove, estockcheck | mixpet 单仓库。门店就一个仓则永久不补 |
| 3 | 效期/批次管理 | estockinitem.DATE_EXPIRATION, WARR_MN | mixpet drug_record 有 batch_no 但无效期预警维度 |
| 4 | 员工提成 | epbmaster.COMMISSION_RATE, ecmemployee 提成列, COMMISSION_LEVEL1-5 | mixpet 无提成体系；若需要则建提成报表（只读统计即可起步） |
| 5 | 专科检查模板 | epetcasejiancha_eyes/pifu/tijian/yachi, epetcaseother1a/b/c(_en), epetcasequicktemp | 眼科/皮肤/体检/牙科结构化模板 → 转 record_template；按门店实际使用率决定搬哪几个 |
| 6 | 病理/微生物(药敏)专项 | epetcasepathology, epetcasemicrobial(104 列药敏矩阵) | 可挂 lab_order 凑合；若门店病理量大再补独立模块 |
| 7 | 日结单 | erptdayfinish(123 列) | mixpet /report/finance 已覆盖口径，补"日结打印单"即可 |
| 8 | 美容工作流 | hairworkflow(14列), ehairingcard | mixpet foster 偏寄养；美容工单流转若复杂需扩展 |

## P2 —— 可放弃或人工搬运

- emembercallcenter 呼叫中心、esms 短信模块（mixpet 有邮件+站内信）、eknowledge 知识库、上传总部(edatabase/上传总部.exe 总部对接)、epetjianyi 建议模板（可转 record_template）、ememberbank、ecmcage 笼位管理（foster 可加笼位字段）。
- 历史财务/报表库（erptdayfinish 等 123 列宽表）：**不迁**，老系统转只读后直接查。

## mixpet 侧已知缺口（此前走查结论，与迁移叠加的优先级提醒）

简报已列的动线 P0（化验/影像医生端入口、注册审核闭环、挂号凭证卡、发药逐项勾选等）**不属于数据迁移阻塞项**，但属于"切换前提=mixpet 覆盖 90% 日常操作"的判断依据，仍按走查顺序推进，勿因迁移工作搁置。

## 用户拍板结论（2026-11-13）

1. **欠款不是常态业务** → P0-2 降级为轻方案：复用 bill.pay_status=0 做"未收款列表"，不做挂账台账/信用额度。
2. **折扣要灵活** → P0-6/P0-4 按灵活方案处理：member_level 保留等级折扣，并支持按商品类别/服务项配置折扣率（等价老系统 ecmmemberdc2 前缀折扣的现代化版本）；ABC 三档价不迁，用"单价+类别折扣"替代。
3. **微信商户参数待开发时补齐** → P0-3 维持开发计划，参数在切换窗口配置。
4. **生产 .bak 手动导出；门店只有一个仓库** → P1-2 多仓库/调拨/盘点**永久豁免**；试算阶段输入由用户手动导出提供。
5. **美容/寄养业务保留** → P1-8 美容工作流列入开发计划（foster 扩展工单流转），老系统 ehairingcard 次卡数据迁入 count_card。

## 迁移工具既定参数（依据上述拍板）

- 老系统 `estockmove/estockcheck` 等过程单据确认不迁，只迁 estocks 期末余量。
- 老会员多身份：取最高权益等级迁入，原身份记 remark（灵活折扣上线后可再细分）。
