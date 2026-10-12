# 项目上下文

## 0. 给 AI 的工作约定（最重要，先读）
1. 不要只分析，直接给可执行命令。除非明确说"先讨论方案"，否则每个任务产出可直接粘贴的代码/命令/patch。
2. 不要问"要不要我做"。有多个合理方案时一次列 2-3 个让用户一次选完。
3. 改代码优先用 git patch（保存 fix.patch 后 git apply）；简单改动可用 cat > 文件 <<'EOF' 整体覆盖；避免一堆 sed -i 单行。
4. 改前先读现状：grep/cat 看当前代码，不要凭记忆猜锚点。
5. 改完必须给验证方式（grep 或 npm run build 确认成功）。
6. 拿不准的决策一次问完，不要来回。
7. 用户是新手，命令要完整可粘贴，不给"把 XXX 换成你的值"这种占位。

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
- 顾客：资料/自建宠物/挂号/余额充值/就诊记录/退养/在线咨询
- 领养（adopt）、寄养（foster）、公告（notice）、图片上传（Img）
- 后台：客户/宠物/医生/科室/药品/账单/领养/寄养/公告/挂号列表管理
- 收银小票 + A4 发票打印（前端 window.print）
- 安全基线：账号=手机号、越权收口、密码哈希不返回、停用客户不能登录、接口被拦业务码 401/403

### ❌ 缺口（宠物医院常有、本项目尚未做）
| 项目 | 说明 |
|---|---|
| 住院管理 | 医生工作台已留占位卡，未实现 |
| 化验/检验管理 | 已留「化验管理」占位卡；现仅"化验室"科室名，无检验单/结果录入 |
| 手术管理 | 有外科/骨科科室，缺手术排程、麻醉记录 |
| 疫苗接种/免疫档案 | 病历无疫苗记录 |
| 驱虫/体检套餐 | 常见项目，未覆盖 |
| 随访/复诊提醒 | 病历有"复诊日期"字段，缺提醒机制 |
| 科室项目缺口 | 暂无神经科/消化内科等细分；如要可后续扩 |

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
医生主页（w-doctorOne.vue）：卡片式，工号 DOCTOR-id 派生、入职=createTime、星座=生日派生，均前端不改库。
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
### 回测通过（2026-10-12）
管理员/顾客登录、顾客自建宠物、编辑资料写身份证、预约挂号、充值闭环、收费结算、打印小票发票

## 8. 待办
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
- [ ] 住院管理
- [ ] 化验/检验管理
- [ ] 疫苗接种/免疫档案
### 上线前
- [ ] 删除测试数据
- [ ] 改默认密码 123456
- [ ] 配置 HTTPS+域名（聊天 WebSocket 现按 location.hostname:8085 直连，上线需给 8085 配 wss 反代）
- [ ] 重导数据库时带 ALTER 过的字段（pet_source 等，不在旧备份 SQL）

## 9. 关键决策
1. 账号=手机号，不开放自主注册，前台录入客户自动生成
2. 密码统一 123456，顾客可自己改；忘记密码三种方式（后台重置/手机号/邮箱），手机号找回因短信成本暂缓
3. 只存生日（真相源），年龄前端实时计算；反推生日标记为估算
4. pet_source 已加（0后台录入 1顾客自建），自建走 POST /pet/self
5. 邮件功能延后，上线前处理