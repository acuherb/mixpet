# 项目上下文

## 0. 给 AI 的工作约定（最重要，先读）
1. 不要只分析，直接给可执行命令。除非明确说"先讨论方案"，否则每个任务产出可直接粘贴的代码/命令/patch。
2. 不要问"要不要我做"。有多个合理方案时一次列 2-3 个让用户一次选完。
3. 改代码优先用 git patch（保存 fix.patch 后 git apply）；简单改动可用 cat > 文件 <<'EOF' 整体覆盖；避免一堆 sed -i 单行。
4. 改前先读现状：grep/cat 看当前代码，不要凭记忆猜锚点。
5. 改完必须给验证方式（grep 或 npm run build 确认成功）。
6. 拿不准的决策一次问完，不要来回。
7. 用户是新手，命令要完整可粘贴，不给"把 XXX 换成你的值"这种占位。
8. **本项目是生产系统，不是毕设。** 所有功能按生产标准实现：不允许占位、不允许 TODO、不允许"先简单处理后面再说"。缺依赖/缺表/缺配置要一次性说清并给迁移脚本，不要偷偷降级实现。
9. **不许替我做取舍。** 如果觉得某需求"没必要"，先说明理由并给 2-3 个方案让我选，不许直接砍功能或简化。我认为必要的功能就是必要的。
10. **每次 commit 后必须 push。** 回复里给出远程 URL 和 commit hash 前 7 位，让我能在 GitHub 核对。没 push 不算完成。

## 1. 脚本避坑清单（踩过的坑，别再踩）
1. 不要用 rpartition("}") 给 Java 类追加方法——类末尾的 } 会被切走导致编译错误。改法：文件末尾 printf '}\n'。
2. 不要用 rfind("</template>") 定位 .vue 根标签——弹窗有多个嵌套 template 会插错。改法：只做纯文本替换（属性值、唯一位置），不动模板结构。
3. 不要把 const xxx = useXxx() 插到 import 之前——SFC 里 import 不提升会 ReferenceError。改法：插到所有 import 之后。
4. 不要给 el-table 用动态 :max-height——路由切换触发 parentNode is null。改法：固定 max-height="calc(100vh - Npx)"。
5. petMapper.updateById(pet) 默认跳过 null 字段——清 client_id 用 LambdaUpdateWrapper.set(xxx, null)。

## 2. 项目一页纸
- 名称：pet-hms（原名 MixPet，MixJade 的毕设重置版二次开发，私有仓库 acuherb/pet-hms）
- 用途：宠物医院管理系统，本地 Debian 13 + MariaDB 11 自建，局域网访问
- 技术栈：Java 17 + Spring Boot 3.1 + Spring Security 6 + MyBatis-Plus 3.5 | Vue 3 + Vite 4 + TS + Element Plus | WebSocket
- 端口：后端 8085 / 前端 3120
- 目录：shipPet/ 后端、ship-ui/ 前端、数据库备份/pet_ship.sql、images/ notice/ chatImg/ 上传文件
- 部署：见 DEPLOY.md；开发流程见 dev-tools/WORKFLOW.md

## 3. 功能总览

### ✅ 已有
- 预约挂号、病历（medical_record）、药品字典（drug）+ 处方（prescription）+ 账单（bill）收费发药、医生接诊
- 医疗扩展（2026-10-13）：化验/检验、住院、影像检查、免疫接种、经营统计看板（均开单/录入并计费入 bill，见「## 医疗扩展模块」）
- 顾客：资料/自建宠物/挂号/余额充值/就诊记录/退养/在线咨询
- 领养（adopt）、寄养（foster）、公告（notice）、图片上传（Img）
- 后台：客户/宠物/医生/科室/药品/账单/领养/寄养/公告/挂号列表管理
- 收银小票 + A4 发票打印（前端 window.print）
- 安全基线：账号=手机号、越权收口、密码哈希不返回、停用客户不能登录、接口被拦业务码 401/403

### ❌ 缺口（宠物医院常有、本项目尚未做）
| 项目 | 说明 |
|---|---|
| 手术管理 | 有外科/骨科科室，缺手术排程、麻醉记录 |
| 驱虫/体检套餐 | 常见项目，未覆盖 |
| 随访/复诊提醒 | 病历有"复诊日期"字段，缺提醒机制 |
| 科室项目缺口 | 暂无神经科/消化内科等细分；如要可后续扩 |

（住院/化验/影像/免疫/统计已于 2026-10-13 上线，见「## 医疗扩展模块」）

## 4. 医疗主链路（挂号→接诊→开方→收费→发药）
数据表：medical_record（appointment_id 唯一）、drug（10 条演示）、prescription、bill。金额 DECIMAL(10,2)/BigDecimal（ALTER 的，不在旧备份 SQL）。
流程：
1. 医生端「查看挂号」→「接诊」→ 病历建档弹窗（主诉=挂号描述/体格检查/诊断/治疗/医嘱/内部备注/复诊日期/挂号费+诊疗费/处方选药实时算价）→ POST /record/save（DOCTOR，事务：病历+处方+未付账单+挂号完成）
2. 后台收费列表：待收费高亮，余额收款/现金收款 → PUT /bill/pay/{id}（NURSE，事务：扣余额+校验库存+扣库存发药+已付）；余额不足可顺手「充」
3. 后台药品列表：CRUD+分类筛选+库存告警（≤5 黄/0 红）+快速入库；停用不可开方
4. 顾客端宠物「就诊记录」→ 就诊史（日期/诊断/医生/费用/缴费状态）→ 病历详情（内部备注不返回）
收费打印（y-billList.vue）：80mm 小票 + A4 发票，独立窗口 window.print，非税务联网，税率暂按免征，发票号按 billId 占位。
权限：record/save=DOCTOR、drug 管理=MANAGER、drug 下拉/bill 收款=NURSE、record 查询=登录即可（顾客只能查自己的，归属校验在 Controller）。
注意：已引入 Lombok；重复接诊被拦（appointment_id 唯一）；开方校验库存、发药再校验扣减；已收款不可重复收。

## 5. 科室与医生
科室由 5 个扩为 9 个（数据库动态读取，已迁移到运行库）：
内科 / 骨科 / 化验室 / 美容洗护室 / 外科 / 影像科 / 皮肤科 / 中兽医科 / 眼科与口腔科。保留原 id 与医生外键关联。
职位称谓：院长(8) / 主任(6) / 主治医师(4) / 护理技师(2) / 实习助理(0)（权限等级不变，仅显示称谓）。
医生主页（w-doctorOne.vue）：卡片式，工号 DOCTOR-id 派生、入职=createTime、工龄=入职派生、座右铭=自行编辑（doctor_motto），均前端不改库。
医生工作台（doctorPage/index.vue）：顶部仅「个人中心」，页头待办统计+真实姓名，处理挂号卡实时预览前 3 条，在线咨询卡，底部化验/住院占位。

## 6. 安全基线（2026-10-12 审计后）
已修越权：/client/one、/client/page、/appointment/page、/adopt/page、/foster/page → MANAGER；/client、/client/full、/pet/client → NURSE；DELETE /notice/delImg → MANAGER；GET /login/user 剔除密码哈希；PUT /doctorLog/appoint/deal → DOCTOR+归属校验；删除 /power/getPwd 调试接口；ClientDetails 登录查 active=1。
已知未修（待决策）：GET /doctor/page 匿名放行带医生敏感字段（需脱敏版，中风险）；GET /appointment/doctor 前台展示就诊记录（产品层决定）；CSRF 全局关；黑名单客户仍可登录；/common/download 可枚举（UUID 低风险）。

## 7. 已完成日志
### 数据库
pet：+pet_birthday_estimated/pet_color/pet_weight/pet_sterilization/pet_chip/pet_allergy/pet_history/pet_alive/pet_note/active/pet_source
client：+client_email/client_pwd_changed/client_tel2/client_birthday_estimated/client_address/client_id_card/client_level/client_points/client_balance/client_source/client_blacklist
adopt：+refuse_reason；pet_transfer：to_client_id 可空
### 后端
手机号=登录账号唯一校验；绑定/改邮箱 PUT /client/email；改密 PUT /client/password；管理员重置 PUT /client/reset/{id}；updateSelfById 只更新可改字段
领养链路：审核通过写 transfer、撤回 PUT /adopt/undo/{id}、拒绝带理由 PUT /adopt/adopt、拒绝后重启 PUT /adopt/restart/{id}、编辑精确更新、宠物智能删除（有引用停用）、启用/停用 PUT /pet/activate|deactivate/{id}、入栏 PUT /pet/intake/{id}?reason=、顾客退养 PUT /adopt/returnPet/{petId}、顾客自建宠物 POST /pet/self
顾客预约挂号 POST /appointment/self（只能挂自己名下宠物）；余额充值 PUT /client/recharge（NURSE，>0 且 ≤10 万，setSql 安全累加）
修 pet_age 残留列：FosterMapper/AppointmentMapper 改 pet_birthday，DTO 字段名统一
### 前端
后台侧边栏可折叠；宠物/主人详情表单分组；生日年龄双向联动+「约」；DetailTable 窄屏适配；绑定邮箱弹窗自适应；列表每页 30 条（可切 10-100）；表格高度自适应；全选提示「已选 N 项」；分页「共 N 条」蓝色加粗；列表列宽收紧+title 提示
### 医生端改造
职位称谓/专科归类贴合行业；医生个人页科技玻璃拟态卡片式；工作台重构；修复 admin 改资料失败（updateRole 补 form.doctorId）
### 医生多科室与资料卡（2026-10-12 二次迭代）
- 医生多科室归属：新增 doctor_department 中间表（DoctorDepartMapper），医生可勾选多个科室+「全科」一键全选，删除医生/改科室同步维护关系表；保留 department_id 作为主科室兼容旧逻辑；科室列表人数按关系表统计
- 科室列表重命名「吉祥物」→「负责人」；默认每页 100 条（全量+滚动），下拉与列表来源同一 department 表
- 简介长度修复：doctor_info varchar(64)→varchar(1000)，前端 maxlength=1000，简介卡固定高度内部滚动（不再撑高挤卡）
- 新增座右铭字段 doctor_motto（数据库列+实体+SQL+前端实体/DO+编辑弹窗+展示披）
- 医生资料卡加宽（940→1120px）、信息网格固定 3 列；工龄=createTime 派生前端实时计算，入职日期从创建医生起算
- 顾客端医生展示：卡片放大、信息重排（头像→名字→基本信息→座右铭→简介最后省略）、详情页全屏(92vw)+挂号前端分页（每页5）+挂号状态列、账号不再对顾客展示、多科室名称展示
- 卡片宽度隔离：医生卡独立 440px 网格（DoctorCard 自建 grid），入口/宠物/寄养卡沿 MyRow 360px 基准，首页三卡保持一行互不干扰
### 回测通过（2026-10-12）
管理员/顾客登录、顾客自建宠物、编辑资料写身份证、预约挂号、充值闭环、收费结算、打印小票发票

### 职务/职称与权限解耦（2026-10-13）
- 背景：原"职位"把权限(auth_lv)与职称混在一根轴上、名称写死（JobUtil.getJob + 表单 option + DoctorDetails switch），顾客端还误把权限当职称显示（有管理权限的医生对顾客显示成"主任"）。
- 改法：权限轴仍是固定 auth_lv(0/2/4/6/8→ADMIN/MANAGER/DOCTOR/NURSE/LOSER，DoctorDetails 贯穿 switch 不动)；新增职称轴 job 字典(job_id)，doctor 加 job_id，可增删改、纯展示，与权限解耦。
- 新增：job 表 + 实体/Mapper/Service/Controller（MANAGER 增删改，删前校验名下是否有医生）、后台「职务/职称」页 z-job（路由 /backstage/z-job，菜单在个人中心组）。
- 前端：PermissionUtil.ts 定义 5 档权限名 + "能干什么"说明(permName/permDesc)，医生/个人页权限下拉带说明；医生列表拆「职称 + 权限」两列；表单拆「权限(仅院长可改，保留 isAdmin 守卫) + 职称(全员可改，自助 updateSelf 已加 job_id)」；顾客端医生卡/详情由 getJob(authLv) 改显真实 jobName。TagJob.vue 现显示权限名；科室详情"职位"列改名"权限"。
- SQL：`数据库备份/job_module.sql`（建 job 表 + doctor 加 job_id + 预置 10 职称 + 按 auth_lv 给老医生播种），已应用。

### 疫苗字典合并进药品字典（2026-10-13）
- 背景：疫苗与药品概念重叠（药品表本就预留 drug_type=疫苗 分类），且疫苗 price 是死字段（接种不计费）、疫苗无库存，单独一套字典重复维护。
- 改法：疫苗 = 药品分类 `drug_type=疫苗`。drug 加 `brand/prevent/interval_days`（可空，仅疫苗用）；删除独立 vaccine 字典（表+实体+Mapper+Service+Controller+前端 z-vaccine 页/模型/API/菜单/路由）。
- 免疫模块：`immune_record` 外键 `vaccine_id→drug_id`、`vaccine_name→drug_name`；登记时按 drug_id 取药、校验 drug_type=疫苗+启用+未删，按 interval_days 算下一针；下拉用 `GET /drug/vaccine`（启用中的疫苗类药品）。
- 药品表单：选"疫苗"分类时才显示 品牌/预防疾病/接种间隔；列表加"接种间隔"列。开方下拉 `GET /drug` 改为**排除疫苗**（疫苗不走处方，走免疫登记）。
- SQL：`数据库备份/vaccine_merge_into_drug.sql`（drug 加 3 字段 → vaccine 数据迁入 drug（临时列记来源 id）→ immune_record 改列名并回填 drug_id → 删临时列和 vaccine 表），已应用。迁移后 drug_type=疫苗 共 8 条（6 个原疫苗 + 2 个演示数据）。
- 后续可优化药品列表：疫苗库存/批号/保质期、统一计费（接种也收钱）等。

### 就诊流程连通 + 国标兽医处方笺（2026-10-13）
- 背景：原接诊只能加药品；化验/影像/住院/免疫各自单独建、未与本次就诊连通；缺国标处方笺。
- 执业资格证号：doctor 加 `doctor_cert`（国标处方笺必填），医生表单/个人页可维护。
- 国标兽医处方笺：`GET /record/rx/{recordId}` 聚合 病历+宠物+主人+医生+处方（`RxSheetVo`）；前端 `d-rx.vue` 可打印页（标题/编号/畜主/宠物(种名·品种·性别·年龄·体重·芯片)/临床诊断/Rp 药品表/金额大小写/开方兽医+资格证号/兽药房调配/日期），`@media print` 打印样式；路由 `/doctorPage/d-rx?recordId=`。
- 流程连通（轻量，未重写）：接诊成功后按挂号单取 recordId（`GET /record/appointment/{appointmentId}`）弹"就诊汇总"，可 打印处方笺 / 开检验单 / 开影像单（跳医生端 d-lab/d-image）/ 入院登记 / 免疫接种（跳后台 z-hospital/z-immune），均带 recordId 预填本次病历。
- 顾客端闭环：病历详情(clientPet)在处方/化验外，补显 影像检查报告、住院信息（后端接口原有，前端接上）。
- SQL：`数据库备份/rx_prescription_module.sql`（doctor 加 doctor_cert），已应用。
- 后续可做：随访复诊提醒（已完成，见下）。

### 随访/复诊提醒（2026-10-13）
- 数据来源：`medical_record.revisit_date`（复诊）、`immune_record.next_time`（下一针）。
- 后端 `GET /remind`（NURSE/MANAGER/DOCTOR）返回 `RemindVo`：`selectDueRevisits`（复诊日期≤今+30天，含逾期）+ `selectDueVaccines`（下一针≤今+30天），联查宠物/客户/医生。
- 前端 `z-remind.vue` 提醒中心：应复诊、应接种两张表，逾期红/临近黄高亮；路由 `/backstage/z-remind`，菜单加「随访提醒」（经营看板旁）。
- 说明：目前是"查询型"提醒（员工进页面查看）；如需主动推送（站内消息/短信）可后续接消息推送。

### 检验/影像申请单国标样式（2026-10-13）
- 复用处方笺模式：`GET /lab/requisition/{orderId}`、`GET /imageOrder/requisition/{orderId}` 返回统一 `RequisitionVo`（头部=畜主/宠物(品种·性别·年龄·体重·芯片)/申请医生+资格证号/临床诊断；body：检验为多项表(名称/分组/参考范围/单位)，影像为单项目+检查部位/目的+费用）。
- 前端 `z-requisition.vue` 可打印页（`/backstage/z-requisition?type=lab|image&orderId=`），`@media print` 打印样式。
- 入口：化验结果(z-labResult)、影像报告(z-imageResult)列表每行加「申请单」按钮。至此国标单据三件套：处方笺 + 检验申请单 + 影像申请单。

## 8. 待办
### 手术/麻醉模块（2026-10-13，③）
- 表：`surgery`(手术单：名称/类型/麻醉方式/预约与实际时间/主刀助手/状态0待1中2完成3取消/术前术中术后记录/手术费)、`anesthesia_record`(麻醉记录：方式/麻醉师/时间/用药/生命体征/备注，一手术一条)、bill 加 `fee_surgery`。脚本 `数据库备份/surgery_module.sql`，已应用。
- 后端 `/surgery`：POST 新建排程(DOCTOR/NURSE/MANAGER，含计费累加 fee_surgery 进未付账单或新建)、GET /page、GET /record/{id}(顾客归属校验)、GET /{id} 详情(含麻醉)、PUT 更新术前/术中/术后/状态、PUT /anesthesia 保存麻醉记录(upsert)。
- 前端 `z-surgery.vue`：手术列表(状态筛选)+新建排程弹窗(病历下拉/名称/类型/麻醉/预约时间/主刀助手/费用)+手术·麻醉记录弹窗(状态/实际时间/术前术中术后/麻醉全套)，路由 /backstage/z-surgery，菜单「手术麻醉」，支持路由 recordId 预填。
- 连通：接诊成功"就诊汇总"加「手术安排」(带 recordId 跳 z-surgery)；顾客端病历详情加"手术/麻醉"展示；账单三处(列表/小票/A4)加"手术费"。

## 8. 待办
### 服务套餐（2026-10-13，②-a 字典已建）
- 表：`service_package`(套餐字典：名称/分类/价格/包含内容/启停)、`record_package`(就诊选购记录)、bill 加 `fee_package` 分项。脚本 `数据库备份/package_module.sql`，预置 5 个套餐（体检/驱虫/免疫）。
- 计费决策：套餐走固定分项 `fee_package`（与 fee_lab/hospital/image 一致，不做通用费用明细重构）。
- 后端 `/package`：GET(启用列表，接诊下拉)、GET /page、POST/PUT/DELETE/{id}（MANAGER）。
- 前端 `z-package.vue` 套餐字典管理页（CRUD+分类筛选+搜索），路由 /backstage/z-package，菜单「套餐管理」。
- ②-b 已完成：接诊建档加"套餐"选择（d-appoint）→ saveRecord 写 record_package + 计 fee_package；账单列表/小票/A4 发票加"套餐费"；顾客端病历详情展示"套餐服务"。
### 回测（待做）
- [ ] 顾客添加宠物（完整表单+待审核）→ 后台审核通过/驳回 → 挂号可选性
- [ ] 顾客端资料编辑（身份证/年龄推算）
- [ ] 头像/聊天图片压缩（手机 3MB 大图）+ 随机头像模板
- [ ] 首改密流程（管理员重置→强改密；注册流程不再被误踢）
- [ ] 后台用户管理手机号唯一性校验
- [ ] 顾客改邮箱/改密码（改密码已实现，邮箱入口待补）
- [ ] 手机端按钮错位修复效果
### 开发
- [ ] 忘记密码改为邮箱找回（现 findMail 逻辑重构）——需先配 QQ 邮箱授权码 MAIL_USER/MAIL_PASS
- [x] 住院管理、化验/检验、影像检查、免疫接种、经营统计看板（详见「## 医疗扩展模块」）
### 上线前
- [ ] 删除测试数据
- [ ] 改默认密码 123456
- [ ] 配置 HTTPS+域名（聊天 WebSocket 现按 location.hostname:8085 直连，上线需给 8085 配 wss 反代）
- [ ] 重导数据库时带 ALTER 过的字段（pet_source 等，不在旧备份 SQL）

## 医疗扩展模块（2026-10-13 上线：化验/住院/影像/免疫/统计）

**通用套路（5 个模块一致）**：医生/医护开单 → 费用按"字典价"计入 bill（优先累加进该病历未付账单的分项 `fee_*`+`total_money`，否则新建一张只含该费用的未付账单）→ 后台/化验室录入结果或报告 → 顾客端按 client_id 归属校验查看。归属以病历为准、价格以字典为准（前端传值不作数）；均 `@Transactional` 回滚。bill 分项：`fee_register/fee_treat/fee_drug/fee_lab/fee_hospital/fee_image`。建表脚本 `数据库备份/{lab,hospital,image,immune}_module.sql`（DROP+CREATE+预置+ALTER，已对线上库执行；image 的 ALTER 依赖 hospital 先建 fee_hospital，故应用顺序 hospital→image→immune）。

1. **化验/检验**：`lab_test`(字典,10条,有 active 标志)/`lab_order`/`lab_order_item`。d-lab 开单 `POST /lab/order`(DOCTOR)、z-labResult 录结果 `PUT /lab/result`(NURSE/MANAGER)、z-labTest 字典(MANAGER，被引用则停用不删)。顾客端 clientPet 展示 `GET /lab/record/{id}`。
2. **住院**：`hospital_record`。z-hospital 入院登记 `POST /hospital`(DOCTOR/NURSE，防重复未出院)、出院结算 `PUT /hospital/discharge/{id}`(DOCTOR/NURSE/MANAGER，按 天数×日均费 计 fee_hospital，至少1天)。
3. **影像**：`image_test`(字典,7条,无 active 用 isDel 软删)/`image_order`。d-image 开单 `POST /imageOrder`(DOCTOR，单项目)、z-imageResult 录报告 `PUT /imageOrder/report`(NURSE/MANAGER)、z-imageTest 字典(MANAGER)。状态 boolean：false 待检查 / true 已出报告。
4. **免疫**：`immune_record`（疫苗字典已合并进药品，见下方"疫苗合并"）。z-immune 接种登记 `POST /immune`(DOCTOR/NURSE/MANAGER，需选病历推导宠物；按疫苗 interval_days 自动算 next_time 下一针、dose_cnt 第几针)。`GET /immune/pet/{petId}` 顾客查自己宠物。疫苗下拉取药品中 `drug_type=疫苗` 的启用项（`GET /drug/vaccine`）。
5. **统计看板**：z-dashboard `GET /statistics/overview`(MANAGER/NURSE/DOCTOR)。今日/本月实收、待收费数、在院数、待检验/待影像数、近30天应接种数、近7日营收趋势(revenueTrend)、医生接诊Top(doctorTop)。SQL 在 StatisticsMapper.xml，实收按 bill.update_time+pay_status=1 计。

**踩坑修复（2026-10-13）**：ImageTestServiceImpl 误给静态工厂 `Wrappers.lambdaUpdate()` 加 `new`（语法错）；ILabOrderService 漏 `import ...entity.LabOrder`；前端 BillDto 缺 feeHospital/feeImage；z-imageResult 状态筛选误用 number(0/1) 应为 boolean；DB 需跑 hospital/image/immune 脚本补表+补 bill 两列；`dev-tools/check.sh` 后端编译需 `LANG=C.UTF-8`（中文路径），已修。

## 9. 关键决策
1. 账号=手机号，不开放自主注册，前台录入客户自动生成
2. 密码统一 123456，顾客可自己改；忘记密码三种方式（后台重置/手机号/邮箱），手机号找回因短信成本暂缓
3. 只存生日（真相源），年龄前端实时计算；反推生日标记为估算
4. pet_source 已加（0后台录入 1顾客自建），自建走 POST /pet/self
5. 邮件功能延后，上线前处理
6. 权限(auth_lv)与职称(job)解耦：权限固定 5 档驱动角色、名称带"能干什么"说明；职称走 job 字典可维护。允许自助改职称，改权限仅院长(ADMIN)

### 接诊一次性开全套单（2026-10-13，流程重构）
- 背景：原接诊只能录药品+套餐，化验/影像/手术要等"完成接诊"后跳转到 5 个页面单独开（子单都外键 record_id，而 recordId 保存后才生成，属绕过非根治）。
- 改法：接诊弹窗内嵌「检验申请（勾选，字典实时价）+ 影像申请（项目+部位/目的）+ 手术申请（开关+名称/类型/麻醉/费用）」；后端 `RecordVo` 加 `labTestIds/imageItemId/imageExamPart/surgery`，`saveRecord` 在同一事务内：建 record → 处方 → 套餐 → 化验单主表+明细（快照 name/group/unit/ref_range，计 fee_lab）→ 影像申请单（计 fee_image）→ 手术单（计 fee_surgery）→ 未付账单（total = 挂号+诊疗+药+套餐+检验+影像+手术）。任一子单校验失败整体回滚。
- 就诊汇总：展示检验/影像/手术费用（仅 >0 显示）；按钮按本次是否已开切换"去录结果"↔"补开"。
- 前端 `SurgeryVo.recordId` 改可空（后端填）；接诊弹窗加 `.lab-pick/.img-pick` 样式。
- 住院（出院才计费）/免疫（不计费）仍走单独入口，未嵌入接诊弹窗。
- 后续可优化：流程顺手度（弹窗分区/分步、批量开单交互）待实际使用后再调。

### 接诊报"系统繁忙"实体字段缺失修复（2026-10-13）
- 现象：接诊加套餐（或保存医生科室关系）点完成/保存，前端提示"系统繁忙，请稍后再试！"。
- 真正根因：`FieldFill.insertFill` 会无条件给所有实体 `set createTime + updateTime`；`RecordPackage`(接诊保存套餐)与 `DoctorDepart`(保存医生科室关系) 缺 `updateTime` 字段 → MyBatis 抛 `ReflectionException: There is no setter for property named 'updateTime'` → 事务回滚 → `ExceptionAdvice` 兜底成"系统繁忙"。
- 首判曾误以为只缺 `record_package.update_time` 库列（commit 22a1802 补列，属不完整修复），实际必须补**实体字段**才根治。
- 修复（commit e2986b2）：① `RecordPackage`/`DoctorDepart` 补 `updateTime` 字段(+getter/setter/@TableField)；② **加固 `FieldFill`：仅当 `metaObject.hasSetter` 存在才赋值**，杜绝今后任何实体再踩坑；③ `doctor_department` 补 `update_time` 列 + 迁移脚本 `fix_entity_update_time_20261013.sql`。
- 排查：扫描全部实体，`createTime`/`updateTime` 现已齐全；重启后端验证 `Started ShipPetApplication` + `/department/name` 返回 200。

### 安全与质量修复（2026-10-14，BUG 审查）
- 背景：收到一份 29 项 BUG 审查报告。逐项核对真实代码后，发现**全局 `SecurityConfig` 是 `anyRequest().authenticated()`，仅 `rearEnd` 数组里的接口匿名放行**；报告里多数"无 @Secured 即匿名可访问"属**误判**（本就要求登录）。只有 `rearEnd` 中真正匿名暴露的是 `/doctor/page`（返回医生全字段敏感数据）。按"真实存在才修"处理。
- 严重(匿名越权)：`/doctor/page` 从 `rearEnd` 移除 + 加 `@Secured({ADMIN,MANAGER})`（commit 本轮）；其余需登录但缺角色约束的接口补 `@Secured`：`/doctor`/`/doctor/department`({ADMIN,MANAGER,NURSE})、`/doctor/department/detail`({ADMIN,MANAGER})、`/pet/page`({MANAGER,ADMIN})、`/pet/noClient`({NURSE,MANAGER,ADMIN})、`/notice/page`({MANAGER,ADMIN})、`/appointment/doctor`({DOCTOR,NURSE,MANAGER,ADMIN})。`/pet/one` 加**顾客归属校验**（CLIENT 仅能看自己宠物，员工看全部）。`/doctor/one` 本就脱敏+需登录，保持。
- 并发/数据一致性：`BillServiceImpl.pay()` 余额扣减与发药扣库存均加 `WHERE ... >= 金额/数量` 原子条件并校验影响行数，防并发超扣成负数/负库存（BUG-8/10）。充值 `ClientController.recharge` 用 `client_balance = client_balance + x` 原子 SQL，本身安全，未改。
- 账单缺字段：各模块新建账单补齐全 fee 列（BUG-21/22/23）。`HospitalServiceImpl.discharge` 补 feePackage/feeSurgery；`LabOrderServiceImpl.createOrder` 补 feeHospital/feePackage/feeSurgery；`ImageOrderServiceImpl.createOrder` 补 feePackage/feeSurgery。`SurgeryServiceImpl` 经核已全，未改。
- 代码缺陷：①`ImgController.getFileName` 去 `assert` 改显式 null 判空 + **图片扩展名白名单**；`upload`/`upChat` 失败时返回 null（不再谎报成功文件名，BUG-13/14/15/26）；`sendImg` 加 **`..`/`/`/`\` 路径遍历防护**（BUG-28）。②`DoctorServiceImpl.addDoctor` 删等价 if/else 死代码（BUG-11）。③`DoctorLogController.update` 未登录返回 `Result.error` 而非 null（BUG-12）。④`ExceptionAdvice` 去 `printStackTrace` 改 `log.error("系统未预期异常", ex)`（BUG-27）。⑤`LoginController.regMail` 验证码 30s 间隔改为用 session attribute 记录发送时间（`getCreationTime()` 是会话创建时间，误判，BUG-18；`sendFindMail` 复用故一并修复）。
- 经核实**不存在/无需改**：BUG-24（手术单账单已含 feePackage/feeSurgery）、BUG-25（Doctor 实体无 pwdChanged 字段，强制改密未实现，属未来项）、BUG-29（`deleteByIds` 早已正常，报告读的是过期版本）、BUG-7（saveRecord 设计无实际缺陷）。
- 验证：`mvn clean compile` 通过；重启后端；curl 验证匿名访问 `/doctor/page`、`/pet/page`、`/notice/page`、`/appointment/doctor`、`/doctor/one` 均返回 `{"code":401,"msg":"当前未登录"}`，而 `/doctor/view` 正常返回脱敏列表。注意本系统用**响应体 code** 而非 HTTP 状态码表示鉴权失败。
- 后续自查可关注：部分顾客/医生端接口在非对应角色时返回 null 而非 `Result.error`（如 `/pet/clientOne`、`/appointment/client`、`/doctorLog/*`、`/login/user`），属一致性小问题，本轮未改以免扩大范围。
- 教训：本项目约定所有实体含 createTime/updateTime；新实体务必两字段都加，否则相关 insert 必崩。

### 工作流增强（2026-10-14 下午，按科学就诊流程推进）
- 背景：以"院长/医生"视角按 分诊→挂号→接诊→诊断→结果回写→治疗/发药→收费→离院/出院→随访复诊/免疫提醒 闭环审视，指出四大断点并推进。
- **T1 接诊分诊结构化**：`MedicalRecord` 补 `triageLevel`(1急诊/2普通/3复诊)、`weight`、`temperature`、`heartRate`、`respiratoryRate`；`RecordVo` 经 `record` 字段自动带入；`d-appoint.vue` 病历区新增"分诊与体征"分组（下拉选分诊级别 + 体重/体温/心率/呼吸数字框）。DB 列见 `数据库备份/feature_20261014_workflow.sql`。
- **T2 检验/影像结果回写与报告挂接**：后端 `LabOrder`/`ImageOrder` 增 `reportFile` 字段；`LabResultVo`/`ImageReportVo` 增 `reportFile`，`saveResult`/`saveReport` 一并持久化；`LabOrderDto`/`ImageOrderDto` 透传，XML 分页/按病历查询补 `report_file` 列。前端结果录入页 `z-labResult.vue`/`z-imageResult.vue` 用 `UpImg` 组件上传报告附件并随结果保存、已录入可查看/移除。顾客端查询接口原本已存在（`/lab/record/{recordId}`、`/imageOrder/record/{recordId}`，带归属校验），结果+报告现已可回写。
  - 注：`UpImg` 组件 `photoNm` 为**必填** prop，使用时务必传 `:photo-nm`（本轮回填 `''`）。
- **T3 发药核对流程**：`Bill` 补 `dispenseStatus`/`dispenseBy`/`dispenseTime`；`IBillService.dispense` + `BillServiceImpl.dispense`（未收款/已发药均拦截，记录核对人与时间）；`BillController.POST /bill/dispense` 加 `@Secured({NURSE,MANAGER,ADMIN})`。前端 `y-billList.vue` 操作列加"药"按钮（仅已收款未发药可见），状态列显示 待发药/已发药，弹窗输核对人姓名后确认。
- **T4 免疫计划与到期提醒**：项目原本**无任何定时任务基础设施**，新增 `@EnableScheduling`（ShipPetApplication）+ `VaccineReminderScheduler`（`@Scheduled(cron="0 0 8 * * ?")` 每日 08:00）。扫描 `immune_record` 中 `next_time` 落在未来 7 天内的记录（去已删），按 客户/宠物 汇总，调用新增的 `INoticeService.createSystemNotice` 程序化生成系统公告（creatId=0，不经登录校验）。每日仅生成一条（按日期标题去重），异常被 catch 仅记日志不中断。
- 验证：`mvn clean compile` 通过；前端 `npm run build`（vue-tsc + vite）通过（仅 chunk 体积警告）；后端重启后 `/bill/dispense` 端点注册并正确拦截匿名；新 DB 列已 `ALTER` 生效。四项均后端编译/运行 + 前端类型检查双重验证。
- 已知边界：①T2 报告附件沿用 `/common/upload`（图片类型校验已在昨日加固），文件服务经 `/api/common/download?name=`；②T4 提醒依赖 `immune_record.next_time` 已被填写，若前台录入免疫时未填下一针时间则不会触发提醒（属前台录入规范问题）；③发药核对与收费解耦——收费即扣库存，发药核对为护士线下发药后的系统确认记录，二者不互斥。

### 隐私加固 + 库存台账 + 退款冲正/单据作废（2026-10-14 下午续）
- 起因：全项目体检发现 T4 引入的**疫苗提醒公告隐私回归**——系统公告含全院宠物名+客户名，却因 `is_disable=0` 默认出现在顾客端 `/notice/four`（匿名放行），顾客可点开正文看到他人信息。
- **公告分流（修隐私）**：`notice` 加 `notice_type`（0顾客公告/1系统提醒仅员工）。`createSystemNotice` 改走新 `addSystemNotice`（`notice_type=1, creat_id=0`）；`selectFour`（顾客端）加 `AND notice_type=0` 过滤；员工端分页补 `notice_type` 并在 `y-noticeList` 标题加"系统"标记（顺手修了该页 `prop="clientName"` 错配）。
- **库存台账**：`Drug` 加 `warning_stock`（预警阈值）+ `expire_date`（有效期）；新建 `drug_record` 出入库台账表（change_type 1入库/2发药出库/3退款回库/4盘亏）。`IDrugService` 增 `inbound`（入库+记台账）、`stockRecordPage`、`listWarning`、`logStockChange`；`DrugController` 增 `POST /drug/inbound`、`GET /drug/warning`、`GET /drug/record/page`（均 MANAGER）。`BillServiceImpl.pay()` 发药扣减后记"出库"台账。前端 `y-drugList`：低库存改用 `warningStock`（未设默认5，取代硬编码）、加有效期列、加"入库"弹窗（数量/有效期/备注）与"出入库台账"抽屉，表单补预警阈值/有效期。
- **退款冲正**：`Bill` 加 `refund_status/refund_by/refund_time`；`IBillService.refund` + 实现（需已收款且未退：回滚库存并记"退款回库"台账、余额支付则退还余额、现金支付提示线下退现金、标记已退）；`BillController PUT /bill/refund/{id}`（NURSE/MANAGER/ADMIN）。前端 `y-billList` 加"退"按钮+`已退`状态+退款二次确认（输操作人）。
- **单据作废**：`Prescription/LabOrder/ImageOrder` 加 `is_cancel`；`PrescriptionMapper.selectByRecord` 加 `AND is_cancel=0`（作废处方不再参与发药/计费）。作废接口均**冲减未付账单**对应费用列（处方→feeDrug=价×量；化验→feeLab；影像→feeImage）与 total，已收款则拒绝并提示走退款：`PUT /lab/cancel/{id}`、`PUT /imageOrder/cancel/{id}`、`PUT /record/prescription/cancel/{id}`。前端 `z-labResult`/`z-imageResult` 加"作废"按钮（仅未出结果/报告可见）。
- 迁移脚本：`数据库备份/feature_20261014_refund_inventory.sql`（notice_type、drug 两列、drug_record 表、bill 三列、三表 is_cancel）。
- 验证：`mvn clean compile` 通过；前端 `npm run build` 通过；DB 新列/表已生效；重启后端 `/bill/refund`、`/drug/warning`、`/lab/cancel` 匿名均 `code:401`，`/notice/four` 200，后端无 ERROR。
- 后续待办（体检发现、未在本轮做）：住院每日监护记录、手术知情同意书、顾客取消/改期预约+号源容量校验、复诊关联与随访记录、remember-me 过期/签名校验、黑名单客户记住我拦截、默认口令可配置、补全顾客端检验/影像报告查看页。

### 顾客报告查看收尾 + 记住我安全加固（2026-10-14 傍晚）
- **顾客端报告查看（补完 T2）**：顾客端 `clientPet.vue` 的就诊详情弹窗本就展示检验/影像**文字结果**，仅缺报告**附件链接**；为化验单/影像单各补一个"查看报告附件/影像附件"外链（`/api/common/download?name=` + `reportFile`）。至此"顾客端可查报告(含附件)"闭环。
- **记住我加固**：`RememberService.processAutoLoginCookie` 原本既不验过期也不验签名（TODO 自认）。修复：①解析 cookie 内 `expiryTime`，过期即抛 `InvalidCookieException`；②先 `loadUserByUsername` 加载用户（`ClientDetails`/`DoctorDetails` 均过滤 `active=1`，停用/黑名单账号在此抛 `UsernameNotFoundException` 被拦截）；③用加载到的密码重算 `makeTokenSignature` 与 cookie 签名比对，防伪造/篡改，密码变更后自动失效需重登。
- 说明：审计里"黑名单客户 remember-me 仍可登录"经核实不准确——自动登录本就调用 `loadUserByUsername`（含 active=1 过滤）；本轮补齐过期+签名后，停用/黑名单/改密均可被正确拦截。
- 验证：`mvn clean compile` 通过；`npm run build` 通过；重启后端 `Started ShipPetApplication` + `/department/name` 200。
- 仍待办（未做）：住院每日监护、手术知情同意书、顾客取消/改期预约+号源容量、复诊关联与随访、默认口令 `123456` 可配置化（`DoctorServiceImpl`/`ClientServiceImpl`/`ClientController`/`application.yml`）。

### 号源容量 + 顾客取消/改期预约 + 默认口令可配置（2026-10-14 夜）
- **号源容量校验**：`AppointmentController.checkAppoint`（私有，新增/改期都会走）末尾加"同一医生同一自然日预约数 ≤ 上限"校验，超限返回"当日号源已满"。上限走配置 `appointment.max-per-day`（默认 20）。改期排除自身（`.ne(appointmentId)`）；`mybatis-plus` 全局 `logic-delete-field: isDel` 已自动排除已删除/已取消。
- **顾客取消/改期**：`AppointmentController` 新增 `PUT /appointment/self/{id}`（改期：锁定客户/宠物/科室/医生/描述等不可改字段，仅采纳新就诊时间，再跑容量校验）与 `PUT /appointment/self/cancel/{id}`（取消：归属校验 + 仅待就诊 + 逻辑删除 `isDel`）。均需登录顾客且只能操作自己的预约、已完成不可操作。前端 `clientAppoint.vue` 每张待就诊卡片加"改期/取消预约"按钮 + 改期弹窗（datetime 选择）；`AppointApi` 加 `reqRescheduleSelfAppoint`/`reqCancelSelfAppoint`。
- **默认口令可配置**：`application.yml` 加 `app.default-password`（默认 123456）；`DoctorServiceImpl`（新建医生）、`ClientServiceImpl`（新建用户）、`ClientController`（管理员重置密码）三处 `@Value` 注入替代硬编码 `123456`。
- 验证：`mvn clean compile` 通过；`npm run build` 通过；重启后端 `/appointment/self/{id}`、`/appointment/self/cancel/{id}` 匿名均 `code:401`，端口 200。
- 仍待办（未做）：住院每日监护记录、手术知情同意书、复诊关联与随访记录。

### 住院每日监护记录（2026-10-14 夜续）
- 背景：住院原先只有"入院登记 + 出院结算"两点，中间数日的看护过程无记录。补齐每日监护。
- 新建 `hospital_monitor` 表 + `HospitalMonitor` 实体/Mapper；`IHospitalMonitorService`/Impl：`addMonitor`（校验住院单存在且"住院中"，自动带 petId、记录人=当前登录医生/顾客名，monitorTime 默认现在）、`listByHospital`（按记录时间倒序）。
- `HospitalMonitorController` `/hospital/monitor`：`POST`（DOCTOR/NURSE 添加）、`GET /{hospitalId}`（列表；顾客端做归属校验：住院单→病历→clientId 仅本人可见）。
- 前端 `z-hospital.vue`：住院列表操作列加"监护"按钮 → 抽屉内含**新增表单**（记录时间/体温/心率/呼吸/体重/精神/食欲/排便排尿/当日用药/处置/备注，仅"住院中"显示新增）+ **监护历史时间线**（已出院仅看历史）。`HospitalApi` 加 `reqAddHospitalMonitor`/`reqHospitalMonitorList`；新建前端模型 `HospitalMonitor.ts`。
- 迁移脚本：`数据库备份/feature_20261014_hospital_monitor.sql`。
- 验证：`mvn clean compile` 通过；`npm run build` 通过（修过一处闭包空指针窄化）；DB 表已建；重启后端 `Started`+200，`/hospital/monitor/{id}` 匿名 `code:401`。
- 仍待办（未做）：手术知情同意书、复诊关联与随访记录。

### 手术/麻醉知情同意书（2026-10-14 夜·收官）
- 背景：手术单已有术前/术中/术后记录 + 麻醉记录，但缺**知情同意书**（合规刚需，国标单据三件套缺一件）。
- 新建 `surgery_consent` 表 + `SurgeryConsent` 实体/Mapper；`ISurgeryConsentService`：`sign`（一手术一条 upsert，需"同意+签字人"，已取消手术拒签）、`getBySurgery`、`consentSheet`（聚合 手术+宠物+主人+主刀兽医+病历诊断+同意书，供打印）。
- `SurgeryConsentController` `/surgery/consent`：`POST`（DOCTOR/NURSE 签署）、`GET /{surgeryId}`（同意书）、`GET /sheet/{surgeryId}`（可打印数据）；后两者顾客端做归属校验（仅本人宠物）。
- 前端 `z-surgery.vue`：操作列加"同意书"按钮 → 弹窗（风险告知预置模板可改、畜主同意开关、签字人、备注；已签自动回显；可重复签署更新）+ "打印同意书"跳转。新增打印页 `z-consent.vue`（对齐 z-requisition 打印风格，`@media print` 隐藏工具栏）并注册路由 `/backstage/z-consent`。`SurgeryApi` 加 `reqSignConsent`/`reqConsentOne`/`reqConsentSheet`。
- 迁移脚本：`数据库备份/feature_20261014_surgery_consent.sql`。
- 验证：`mvn clean compile` 通过；`npm run build` 通过；`surgery_consent` 表已建；重启后端 `/surgery/consent/sheet/{id}` 匿名 `code:401`、端口 200。
- 至此体检清单仅剩最后一项：**复诊关联 + 随访记录**（复诊单关联原病历、术后/出院随访）。

### 复诊关联 + 随访记录（2026-10-14 终）
- **复诊关联**：`Appointment` 与 `MedicalRecord` 各加 `origin_record_id`（复诊挂号时填初诊病历id）。`saveRecord` 接诊时把挂号单的 `originRecordId` 写入新病历；`selectRecordOne` 回填 `originDiagnosis`（初诊/上次诊断）；新增 `GET /record/revisits/{recordId}` 查某病历的复诊链（顾客端归属校验）。前端：顾客病历详情显示"关联就诊/复诊记录"，加"预约复诊"按钮跳挂号页并带 `originRecordId`；`clientAppoint.vue` 读取 URL `originRecordId` 自动打开预约弹窗并提示"复诊挂号（关联病历 #X）"，提交时一并带上（`saveSelf` 保存到挂号单）。
- **随访记录**：新建 `follow_up` 表 + `FollowUp` 实体/Mapper + `FollowUpDto`（联查宠物/客户/诊断）+ `FollowUpMapper.xml` 分页。`IFollowUpService`：`add`（自动带宠物/客户、状态待随访、随访人=当前登录人）、`listByRecord`、`selectPage`（待随访/已随访 + 宠物或客户名搜）、`done`（记结果/时间/随访人，已完成拒重）。`FollowUpController /follow`：`POST` 建计划（DOCTOR/NURSE）、`GET /page` 管理分页、`GET /record/{recordId}`（顾客归属校验）、`PUT /done/{id}` 完成。前端新增管理页 `z-follow.vue`（状态筛选/搜索/新建/完成随访弹窗/查看详情）+ 路由 `/backstage/z-follow` + 后台菜单"随访记录"。
- 迁移脚本：`数据库备份/feature_20261014_revisit_followup.sql`（appointment/medical_record 加 origin_record_id、建 follow_up 表）。
- 验证：`mvn clean compile` 通过（修过 IMedicalRecordService 缺 List 导入）；`npm run build` 通过（修过 clientPet 重复 useRouter 导入）；DB 列/表已建；重启后端 `/record/revisits/{id}`、`/follow/page` 匿名 `code:401`、端口 200。
- **至此，全项目体检清单（29 项 BUG + 临床/安全/库存/财务工作流）全部完成。**

### 接诊流程重构：分门别类 + 分区保存 + 可追加（2026-10-14 夜·体验优化）
- 背景（用户反馈）：原接诊是一个大弹窗，病历/处方/套餐/检验/影像/手术全在一页需上下滚动，且只有"完成接诊"一次性提交；医生想分门别类逐个填写、分区保存，且提交后还能继续加药/加检查。
- **前端 `d-appoint.vue` 重写为两阶段**：
  - 阶段一「病历建档」：只填 主诉/诊断/治疗/医嘱/体征/费用 → "保存病历并开单"（`saveRecord` 仅带病历，生成病历+初始账单）。
  - 阶段二「开单调」：`el-tabs` 分 处方/检验/影像/手术/套餐 五个标签，**每个标签独立保存/开单**（保存处方、开检验单、开影像单、安排手术、保存套餐），并展示"已开"列表与实时合计；底部"完成接诊"出就诊汇总。已建过病历的挂号再次点接诊会**直接进入开单阶段**（支持继续追加）。
- **后端补两个"往已有病历追加"端点**（检验/影像/手术本就有各自开单端点，复用之）：
  - `POST /record/prescription`：`addPrescriptions`（校验药品/库存、快照、插入，未付款账单 `fee_drug/total_money` 累加）。
  - `POST /record/package`：`addPackages`（快照 RecordPackage、插入，未付款账单 `fee_package/total_money` 累加）。
  - 两者均要求存在**未付款账单**，已收款则提示先退款（与检验/影像作废策略一致）。
- 验证：`mvn clean compile` 通过；`npm run build` 通过；重启后端 `/record/prescription`、`/record/package` 匿名 `code:401`、端口 200。

### 化验/影像独立开单 + 统一设置中心（2026-10-14 夜·体验/架构）
- 背景（用户反馈）：①化验/影像应"单独去开单"，且接诊里"化验不能选择项目"；②建议把化验分类/项目、药品分类/单位等各模块配置**统一放到一个设置中心分门别类**管理。
- **化验/影像独立开单 + 修复选择bug**：根因是 Element Plus 2.3.6 的 `el-checkbox` 用 `label` 绑定值，而我上轮在接诊里用 `:value`（该版本不识别）→ 选不中。现将**检验/影像从接诊弹窗移除**，接诊只保留 病历/处方/手术/套餐；检验、影像走各自专用开单页 `d-lab`/`d-image`（本就正常，`:label`），从就诊汇总"去开检验单/影像单"进入。
- **统一设置中心（通用字典 sys_dict）**：新建 `sys_dict` 表 + `SysDict` 实体/Mapper/Service/Controller `/dict`（`GET /dict/type/{type}` 登录可用；增删改/启停 MANAGER）。前端新增设置页 `z-setting.vue`（左侧 6 类菜单 + 右侧字典值增删改/启停/排序）+ 路由 `/backstage/z-setting` + 后台菜单"系统设置"。字典类型：`lab_group` 化验分组 / `img_group` 影像分组 / `drug_type` 药品分类 / `drug_unit` 药品单位 / `surgery_type` 手术类型 / `anesthesia_type` 麻醉方式。
- **各模块表单改接字典**（去掉 allow-create 自由输入，统一在设置中心维护）：药品页 `y-drugList`（分类/单位）、检验项 `z-labTest`（分组）、影像项 `z-imageTest`（分组）、手术 `z-surgery`（类型/麻醉，含麻醉记录）、接诊 `d-appoint` 手术标签（类型/麻醉）。种子数据已预置常用值。
- 迁移脚本：`数据库备份/feature_20261014_sys_dict.sql`（sys_dict 表 + 6 类种子）。
- 验证：`mvn clean compile` 通过；`npm run build` 通过；DB 6 类种子已入；重启后端 `/dict/type/{type}` 匿名 `code:401`、端口 200。

### 菜单科学化重组 + 医生排班 + 预约就诊提醒（2026-10-14 夜·院长视角）
- 背景：以"院长"视角参考主流宠物 SaaS 菜单，提出优化菜单排列 + 补齐 医生排班/预约提醒/在线支付/采购供应商/数据备份 等缺口。今天按"高价值+自洽+可验证"推进三项，其余给可行性结论。
- **菜单科学化重组**（前端 `backstage/index.vue`）：扁平菜单 → 分组子菜单：经营看板 / 接待管理(挂号) / 店面业务(收银台·寄养·疫苗驱虫·随访提醒·随访记录) / 诊疗管理(化验结果+检验项·影像报告+影像项·手术麻醉·住院) / 商品与服务(药品·套餐·活体领养) / 客户中心(客户·宠物) / 基础配置(系统设置·公告) / 组织管理(仅管理员：科室·职务·医生·医生排班)。图标导入随之精简；无死链接（全部指向既有路由）。
- **医生排班**：新建 `doctor_schedule` 表 + `DoctorSchedule` 实体/Mapper + `IScheduleService`/Impl + `ScheduleController /schedule`（`GET /doctor/{id}` 回显、`GET /all` 总览、`POST` 覆盖式保存）。`isOnDuty(doctorId,date)`：未配置排班的医生不限制（向后兼容，避免影响既有挂号）；已配置则按星期校验。挂号 `AppointmentController.checkAppoint` 加入班校验（新增/改期/员工/顾客端均走此校验），不出诊则拒。前端 `z-schedule.vue`：选医生→周一~周日 × 上午/下午 开关+号源编辑→保存；下方排班总览。路由 `/backstage/z-schedule`，菜单挂在"组织管理"。
- **预约就诊前提醒**：新增 `AppointmentReminderScheduler`（复用已有 `@EnableScheduling`，每日 08:00）：扫描未来 36 小时内、待就诊(isSuc=false)的挂号，按 宠物/客户/医生/时间/备注 汇总成一条系统公告（`createSystemNotice`，仅员工可见、每日去重），供医护提前准备。
- 迁移脚本：`数据库备份/feature_20261014_schedule.sql`（doctor_schedule 表）。
- 验证：`mvn clean compile` 通过；`npm run build` 通过（修过一处图标未用导入）；`doctor_schedule` 表已建；重启后端 `/schedule/all` 匿名 `code:401`、端口 200。
- **本轮明确暂缓（附原因，非遗忘）**：①在线支付(微信/支付宝)——外部强依赖(商户资质/密钥/异步回调/对账)，今天只能留抽象；②数据备份/恢复界面化——恢复有毁库风险，不做一键恢复，导出可另行排期；③会员卡/次卡/积分闭环、经营财务报表、AI 诊断、电子发票、顾客小程序、多院区、药品批次效期、投诉工单——均为中大型功能，需各排半天~一天。

### 供应商管理 + 采购入库(供应商/批号) + 近效期预警/补货建议（2026-10-15）
- 背景：接上轮"院长视角"待办，推进库存采购/供应商缺口（在已有 `drug_record` 台账上扩展，自洽、无外部依赖）。
- **供应商管理**：新建 `supplier` 表 + `Supplier` 实体/Mapper + `ISupplierService`/Impl + `SupplierController /supplier`（`GET /` 启用列表、`GET /page` 分页、增/改/删 MANAGER）。前端新增 `z-supplier.vue`（名称/联系人/电话/地址/备注 CRUD）+ 路由 `/backstage/z-supplier`。
- **采购入库带供应商/批号**：`DrugRecord` 加 `supplierId`/`supplierName`(冗余)/`batchNo`；`IDrugService.inbound` 增 supplierId/batchNo 参数，入库时查供应商名一并写入台账；`DrugController /drug/inbound` 同步扩参。前端 `y-drugList.vue` 入库弹窗加"供应商下拉 + 批号输入"，出入库台账抽屉加"供应商/批号"列。
- **近效期预警 + 补货建议**：`IDrugService.listExpiring(days)`（expireDate 在 [今天, 今天+days] 的启用药品，按效期升序）+ `GET /drug/expiring`。前端药品页加"库存预警"按钮（带角标计数）→ 抽屉分两区：**低库存（含"建议补货量"=阈值×2−当前，下限10）** + **近效期（默认90天）**。
- 菜单：后台新增"库存管理"分组（药品库存 + 供应商管理），从"商品与服务"移出药品。
- 迁移脚本：`数据库备份/feature_20261015_supplier_inventory.sql`（supplier 表 + drug_record 加 supplier_id/supplier_name/batch_no）。
- 验证：`mvn clean compile` 通过；`npm run build` 通过；supplier 表与 drug_record 新列已建；重启后端 `/supplier`、`/drug/expiring` 匿名 `code:401`、端口 200。

### 经营财务报表 + 一页式宠物健康档案（2026-10-15）
- 背景：接"院长视角"待办，按用户指定顺序先做经营财务报表、后做一页式宠物健康档案。
- **经营财务报表**：`Drug` 加 `costPrice`(进货价，用于药品毛利)；新建 `FinanceReport` VO + `IReportService`/Impl + `ReportController GET /report/finance?start=&end=`（MANAGER/ADMIN）。按**接诊日期(createTime)**统计已收款、未退款账单：总收入/订单数/客单价/退款额、按 8 个收费项目(挂号/诊疗/药/化验/影像/套餐/手术/住院)构成、每日收入趋势、按科室/按医生(关联病历)、药品成本/毛利。前端 `z-report.vue`：日期区间+近7天/本月/上月快捷、指标卡、收入构成条、每日趋势 CSS 柱状、按科室/医生表、药品毛利卡；路由 `/backstage/z-report`；菜单"经营看板"升级为"数据统计"分组(经营看板+财务报表)；药品表单加"进货价"。
  - 边界：账单无 payTime，按接诊日期统计；药品成本按**当前进货价估算**(未快照历史进货价)；服务类(检验/影像/手术/住院/套餐)不计成本，故毛利仅覆盖药品。
- **一页式宠物健康档案**：新建 `PetHealth` VO + `IPetHealthService`/Impl + `PetHealthController GET /pet/health/{petId}`（顾客仅本人宠物，员工任意）。聚合：宠物基本信息(过敏史/既往病史/绝育/芯片)、概要(就诊次数/最近就诊/最近诊断/下次免疫)、体重曲线(取自理诊体重)、就诊史(近50)、免疫记录、住院记录。前端可复用组件 `PetHealthDialog.vue`，接入后台宠物列表(`y-petList` 操作列"康"按钮)与顾客端(`clientPet` 宠物卡片"健康档案"按钮)。
- 迁移脚本：`数据库备份/feature_20261015_finance_report.sql`（drug 加 cost_price）。
- 验证：`mvn clean compile` 通过；`npm run build` 通过（修过 PetHealthDialog 一处 update:modelValue 参数类型）；cost_price 列已建；重启后端 `/report/finance`、`/pet/health/{id}` 匿名 `code:401`、端口 200。

### 电子病历模板化 + 数据导出(CSV) + 会员卡/次卡/积分闭环（2026-10-15）
- **电子病历模板化**：新建 `record_template` 表 + 实体/Mapper + `IRecordTemplateService`/Impl + `RecordTemplateController /recordTemplate`（`GET /` 启用列表供医生、`/page` 增改删 MANAGER/ADMIN）。预置主诉/体格检查/诊断/治疗/医嘱/内部备注。前端模板管理页 `z-template.vue` + 路由 + 菜单（诊疗管理）；接诊 `d-appoint.vue` 病历区加"病历模板"下拉，选中即一键填充表单（可再改）。
- **数据导出(CSV)**：前端工具 `utils/ExportUtil.ts`（`exportCsv(文件名, 列, 数据)`，BOM+转义，Excel 中文不乱码，Blob 下载，无需后端/无新依赖）。接入关键列表页"导出"按钮：药品 `y-drugList`、账单 `y-billList`、客户 `y-clientList`（导出当前筛选/分页数据）。
- **会员卡/次卡/积分闭环(MVP)**：
  - `member_level`（等级键0-3对应 clientLevel、折扣率、充值返赠比例、消费积分率）+ 种子(普通/银卡/金卡/钻石)；`count_card_template`（次卡模板）；`client_count_card`（客户次卡）。三表 + 实体/Mapper。
  - `IMemberService`/Impl + `MemberController /member`：等级 CRUD、次卡模板 CRUD、`POST /buyCard`(扣余额生成次卡)、`POST /useCard/{id}`(核销扣次)、`GET /cards/{clientId}`、`POST /exchange`(积分兑余额 100分=1元)。
  - **闭环接入**：`ClientController.recharge` 充值按等级返赠（到账=本金+返赠）；`BillServiceImpl.pay` 收款后按等级积分率累积积分。
  - 前端：`z-memberLevel.vue`、`z-countCard.vue` + 路由 + 菜单（客户中心）；客户列表 `y-clientList` 操作列加"员"按钮 → 会员中心弹窗（余额/积分/等级 + 积分兑换 + 购卡 + 持有次卡核销）。
  - 边界：折扣率字段已预留但**尚未接入收费抵扣**（避免改动收款主流程，留待后续）；积分率默认1元=1分；次卡核销为独立动作，未与具体服务单联动。
- 迁移脚本：`数据库备份/feature_20261015_member_card.sql`（member_level/count_card_template/client_count_card + 等级种子）；record_template 表见 `数据库备份/feature_20261015_record_template.sql`。
- 验证：`mvn clean compile` 通过（修过 MemberServiceImpl 两处 `BaseMapper.updateById/deleteById` 返回 int 需 `>0` 转 boolean）；`npm run build` 通过（修过 y-drugList `drugList.value.records`）；三表已建；重启后端 `/member/level`、`/recordTemplate` 匿名 `code:401`、端口 200。

### 会员折扣接入收费（2026-10-15）
- 背景：上轮会员/次卡/积分闭环预留了"等级折扣率"字段但未接入收费。本轮打通，让折扣真正影响应收。
- **设计取舍（关键）**：`bill` 加 `discount`（普通列，**建单时按客户等级快照**，此后不随等级变动而变）；`discount_money`/`real_money` 设为 **MariaDB 生成列**（`PERSISTENT`，由 `total_money × discount` 自动算）。
  - 好处：全项目有 **7 处**会改账单金额（追加处方/套餐、开化验单/影像单/手术、住院、作废冲减），生成列方案下这些地方**只需照旧改 `total_money`，折后应付与优惠永远自动一致**，杜绝"追加了费用但实收没算进去"的账实不符。
- **接入点**：
  - `MedicalRecordServiceImpl.saveRecord` 建单时 `bill.setDiscount(memberService.discountOf(clientId))`（新增 `IMemberService.discountOf`，带 (0,1] 区间防御）。
  - `BillServiceImpl` 加 `payable(bill)` 助手（`realMoney ?: totalMoney`，兼容旧单）；`pay` 余额扣减、`refund` 退还、`earnPoints` 积分累积**全部改按折后实收**。
  - `ReportServiceImpl.finance` 收入/趋势改按 `real_money` 汇总，新增"优惠合计 `discountMoney`"。
- 前端：`BillDto` 补 `discount/discountMoney/realMoney`；`y-billList` 把"合计"列拆为**原价 / 优惠 / 应付**三列（`realMoneyOf` 助手）；`FinanceReport`/`z-report` 补"优惠合计"展示。
- 迁移脚本：`数据库备份/feature_20261015_bill_discount.sql`（discount 普通列 + 两生成列 + 存量 `discount=1` 回填）。
- 验证：`mvn clean compile` 通过（修过 BillServiceImpl 缺 BigDecimal 导入）；`npm run build` 通过；DB 生成列实测：total 300 × 0.9 → 优惠 30、应付 270，且 `total_money+100` 后自动重算为 300/30/270（追加联动正确，测试行已清理）；存量账单实收=原价（不变）；重启后端 `Started`+200、`/member/level`、`/bill/page` 匿名响应体 `code:401`、无 ERROR。
- 说明：项目鉴权统一以**响应体 `code:401`** 返回（HTTP 状态为 200），排查时要看响应体而非状态码。

### 后台菜单重组 v2（2026-10-15）
- 背景（用户反馈+自提方案）：上一轮"数据统计/接待管理/店面业务/诊疗管理/商品与服务/库存管理/客户中心/基础配置/组织管理"的分组仍不够顺手；用户自拟一版（接待与收费/店面服务/诊疗中心/商品与字典/客户中心/组织管理/系统配置）。评估后采纳其主干，做三处修正后实施。
- **最终结构**（`views/backstage/index.vue`，纯前端、无死链接）：
  - 顶层：个人中心 · 经营看板 · **财务报表**（用户方案漏了此项，与看板同属"一眼看经营"，故并列顶层而非塞进子菜单）
  - **接待与收费**：挂号列表 · 收银台（挂号→收费一条线）
  - **店面服务**：寄养管理（域名独立，预留洗护/美容；只有 1 项是正常的）
  - **诊疗中心**：化验结果录入 · 影像报告录入 · 手术麻醉 · 住院管理 · 疫苗驱虫 · 随访提醒 · 随访记录（疫苗/随访从店面业务移入——它们是医疗行为）
  - **商品与库存**：药品管理 · 服务套餐 · 供应商管理（原"商品与服务"+"库存管理"合并）
  - **客户中心**：客户管理 · 宠物管理 · 活体领养 · 会员等级 · 次卡模板
  - **系统配置**：系统设置 · 检验项字典 · 影像项字典 · 病历模板 · 公告管理
  - **组织管理**（仅管理员）：科室列表 · 医生列表 · 医生排班 · 职务/职称
- **三处修正及理由**：①用户方案漏了 财务报表/会员等级/次卡模板/供应商管理/病历模板 五个已有入口，已补齐；②把"商品与字典"拆开——检验项/影像项字典与病历模板归入"系统配置"，与既有"系统设置"（化验分组/影像分组/药品分类/单位…）凑成**唯一一站式配置中心**，确立原则"系统配置=所有定义/字典类，其他分组=纯业务操作"；③疫苗驱虫/随访三项按用户意见移入诊疗中心。
- 验证：`npm run build` 通过；29 个菜单项全部指向已注册路由，无死链接。

### 后台菜单重组 v3（精简版，2026-10-15）
- 背景：v2 版（11 个一级项）用户反馈"不够合理"。自审发现三个真实问题：①一级项过多（3 顶层 + 8 分组）稀释查找效率；②「店面服务」只有 1 项（寄养管理）——单项目组是信息架构坏味道，说明分组维度选错；③「系统配置」名不副实，里面塞了"公告管理"（内容是运营不是配置）。
- **修正原则**：一级项 ≤ 8；不出现单项目组（只 1 项时并入作业流最近的邻组）；按"作业流"分组而非"资源类型"；组名不与组内项重名；命名风格统一（XX管理 / XX运营 / XX与库存）。
- **最终结构**（一级项 11 → 8）：
  - 顶层：个人中心 · 经营看板 · 财务报表（看板按用户要求保持顶层）
  - **接待管理**：挂号列表 · 收银台 · **寄养管理**（寄养从单项目组"店面服务"并入——挂号→收费→寄养接待本就是前台同一条作业线）
  - **诊疗管理**：化验结果录入 · 影像报告录入 · 手术麻醉 · 住院管理 · 疫苗驱虫 · 随访提醒 · 随访记录
  - **商品与库存**：药品管理 · 服务套餐 · 供应商管理
  - **客户运营**：客户管理 · 宠物管理 · 活体领养 · 会员等级 · 次卡模板（原"客户中心"改名，避免与组内项"客户管理"重名）
  - **系统管理**：系统设置 · 检验项字典 · 影像项字典 · 病历模板 · 公告管理（原"系统配置"改名——"配置"容不下公告，"管理"可容纳设置+字典+模板+公告）
  - **组织管理**（仅管理员）：科室列表 · 医生列表 · 医生排班 · 职务/职称
- 验证：`npm run build` 通过（移除了不再使用的 ShoppingBag 图标导入）；菜单项全部指向已注册路由，无死链接。

### 数据中心合并 + 看板/报表视觉升级（2026-10-15）
- **菜单**：经营看板 + 财务报表 两个顶层项合并为「**数据中心**」子菜单（一级项 8 → 7）；分组图标用 `DataBoard`。
- **看板指标扩充**（`StatOverviewVo` + `StatisticsServiceImpl`，注入 AppointmentMapper/MedicalRecordMapper/ClientMapper/DrugMapper）：新增 今日挂号数、今日接诊数、客户总数、待发药账单数、低库存药品数（`warning_stock>0 AND drug_stock<=warning_stock`）、近效期药品数（90 天内）。
- **看板视觉升级**（`z-dashboard.vue` 重写）：8 张指标卡（渐变彩色图标 + hover 上浮），分"营收/运营"两行；待办卡与库存预警卡都加图标，低库存/近效期卡加左侧色条并可点击跳药品页；趋势图与医生 Top 的卡片头加图标，Top1/2/3 名次用金/银/铜渐变圆标。
- **报表视觉升级**（`z-report.vue` 重写）：筛选栏/指标卡/各分区头全部加渐变图标；**新增"环比"**——自动取等长且紧邻当前区间之前的周期再查一次 `/report/finance`，在"总收入"卡下显示 `el-tag` 涨跌幅（绿涨红跌，上期无数据则不显示）。
- 验证：`mvn clean compile` 通过；`npm run build` 通过；SQL 复核新指标查询正确（待发药 2、客户总数 12 等）；重启后端 `Started`+200、`/statistics/overview` 匿名 `code:401`、无 ERROR。