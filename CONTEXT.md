# 项目上下文

## 给 AI 的工作约定（最重要，先读）

1. **不要只分析，直接给可执行命令**。除非用户明确说"先讨论方案"，否则每个任务都要产出可以直接复制粘贴的代码/命令/patch。
2. **不要问"要不要我做"**。用户说想做，就做。有多个合理方案时，一次列 2-3 个让用户一次选完，不要一个个问。
3. **改代码优先用 git patch**：
   - 让用户保存为 fix.patch，然后 git apply fix.patch
   - 简单改动可以直接用 cat > 文件 <<'EOF' ... EOF 整体覆盖
   - 避免一堆 sed -i 单行命令，容易出错
4. **改前先读现状**：用 grep/cat 看当前代码，不要凭记忆猜锚点。
5. **改完必须给验证方式**：一段 grep 或 npm run build 命令让用户确认成功。
6. **拿不准的决策**：一次问完，不要来回。
7. **用户是新手**，命令要完整可粘贴，不要写"把 XXX 换成你的值"却不给例子。

## 脚本避坑清单（踩过的坑，别再踩）

1. **不要用 `rpartition("}")` 给 Java 类追加方法**——类末尾的 `}` 会被切走，导致"到达文件结尾"编译错误。改法：直接在文件末尾 `printf '}\n' >> 文件`。
2. **不要用 `rfind("</template>")` 定位 .vue 根标签**——弹窗里有多个嵌套 `<template>`，会插错。改法：只做纯文本替换（如属性值、`</BackOpCol>` 唯一位置），不动模板结构。
3. **不要把 `const xxx = useXxx()` 插到 import 之前**——Vue SFC 里 import 不提升，会 ReferenceError。改法：插到所有 import 之后。
4. **不要给 el-table 用动态 `:max-height`**——路由切换会触发 `parentNode is null`。改法：用固定 `max-height="calc(100vh - Npx)"`。
5. **`petMapper.updateById(pet)` 默认跳过 null 字段**——清 `client_id` 要用 `LambdaUpdateWrapper.set(xxx, null)`。

## 用户的开场白模板

项目在 ~/公共/pet-hms，先读 CONTEXT.md，然后我说今天做什么。
读完后，按 CONTEXT.md 顶部的「给 AI 的工作约定」跟我配合。

---

## 项目背景

- 原名 MixPet（作者 MixJade 的毕设），GitHub: https://github.com/MixJade/MixPet
- 现名 pet-hms，GitHub: https://github.com/acuherb/pet-hms（私有）
- 用途：自建宠物医院管理系统，自用
- 部署：本地 Debian 13 + MariaDB，局域网访问

## 技术栈

- 后端：Java 17 + Spring Boot 3.1 + Spring Security 6 + MyBatis-Plus 3.5
- 前端：Vue 3 + Vite 4 + TypeScript + Element Plus
- 数据库：MariaDB 11（兼容 MySQL 8）
- 后端端口 8085，前端端口 3120

## 目录结构

- shipPet/ — 后端
- ship-ui/ — 前端
- 数据库备份/pet_ship.sql — 数据库脚本
- images/ notice/ chatImg/ — 上传文件目录
- DEPLOY.md — 部署教程
- CONTEXT.md — 本文件

## 已完成（截至 2026-10-11）

### 数据库
- pet 表：+pet_birthday_estimated、pet_color、pet_weight、pet_sterilization、pet_chip、pet_allergy、pet_history、pet_alive、pet_note、active
- client 表：+client_email、client_pwd_changed、client_tel2、client_birthday_estimated、client_address、client_id_card、client_level、client_points、client_balance、client_source、client_blacklist
- adopt 表：+refuse_reason
- pet_transfer 表：to_client_id 改为可空

### 后端
- 手机号即登录账号，唯一性校验
- 绑定/修改邮箱接口 PUT /client/email
- 修改密码接口 PUT /client/password
- 管理员重置密码接口 PUT /client/reset/{id}
- updateSelfById 只更新用户可改字段
- 领养审核通过 → 写 pet_transfer 记录（reason 带申请理由）
- 领养撤回（PUT /adopt/undo/{id}）→ 宠物变无主、单子回待审核、写 transfer
- 领养拒绝带理由（PUT /adopt/adopt，AdoptVo.refuseReason）
- 拒绝后可重启（PUT /adopt/restart/{id}）→ 状态回待审核、理由清空
- 领养编辑精确更新（只改 money/info，不动 in_adopt）
- 宠物智能删除：无引用 → 逻辑删；有引用 → active=0 停用
- 宠物启用/停用接口（PUT /pet/activate|deactivate/{id}）
- 宠物入栏（PUT /pet/intake/{id}?reason=）：有主→无主（遗弃/托管），清 client_id 用 LambdaUpdateWrapper.set，写 transfer 记录（reason=`入栏：xxx`）
- 顾客退养（PUT /adopt/returnPet/{petId}）：领养人退回名下宠物→无主，领养单标记 in_adopt=3（已退养），写 transfer 记录「顾客退养」；个人中心宠物列表有退养按钮
- 顾客自己添加宠物（POST /pet/self）：主人强制为登录人、pet_source=1；pet 表已加 pet_source 字段（0后台录入 1顾客自建）；个人中心宠物列表有添加弹窗（姓名/品种/性别/生日或年龄/毛色/照片）
- 顾客自己预约挂号（POST /appointment/self）：挂号人强制为登录人，只能挂自己名下宠物；个人中心挂号页有预约弹窗（宠物/科室/医生联动/时间/描述）
- 顾客余额充值接口（PUT /client/recharge，NURSE）：金额>0 且 ≤10 万校验，insert 方式用 setSql 安全累加余额
- 修 pet_age 残留列：FosterMapper.xml / AppointmentMapper.xml 改 pet_birthday，DTO 字段名统一

### 前端
- 后台侧边栏可折叠
- 宠物/主人详情和表单分组显示
- 生日与年龄双向联动，估算加「约」
- DetailTable 窄屏适配
- 绑定邮箱弹窗宽度自适应
- 后台所有列表每页 30 条（可切 10/20/30/50/100）
- 表格高度自适应（max-height="calc(100vh - 130px)"）
- 全选提示「已选 N 项」在操作栏左上方
- 分页条「共 N 条」蓝色加粗
- 宠物/用户列表列宽收紧 + 操作按钮 title 提示

## 安全基线（2026-10-12 审计后）

**已修复的越权**（顾客登录后原本可拉全量客户资料/所有挂号/领养寄养记录）：
- `GET /client/one`、`/client/page`、`/appointment/page`、`/adopt/page`、`/foster/page` → MANAGER
- `GET /client`、`/client/full`、`/pet/client` → NURSE（医生端 d-appoint 依赖，医生 auth_lv≥2 均有 NURSE）
- `DELETE /notice/delImg` → MANAGER
- `GET /login/user` 响应剔除密码哈希（setClientPassword(null)）
- `PUT /doctorLog/appoint/deal` → DOCTOR + 只能处理自己的挂号单（原 LOSER 无归属校验）
- 删除 `GET /power/getPwd/{password}` 调试接口（匿名可调且明文记日志）
- ClientDetails 登录查询加 active=1：停用客户不能再登录
- 接口被拦时 HTTP 仍 200、业务码 401/403（/power/error 统一转发，属项目风格）

**已知未修（记录待决策）**：
- `GET /doctor/page` 匿名放行且 SQL 带 doctor_tel/email/username/auth_lv——前台首页和后台医生列表共用此接口，需做脱敏版公开接口（中风险）
- `GET /appointment/doctor` 前台 doctorOne.vue 用（展示医生接诊记录给顾客看，患者姓名/病情暴露），产品层面决定是否保留
- CSRF 全局关闭（session+rememberMe 场景）
- 黑名单客户（client_blacklist=1）仍可登录（只有停用 active 挡了）
- /common/download 按文件名可枚举（UUID 文件名，风险低）

## 医疗主链路（2026-10-12 新增，挂号→接诊→开方→收费→发药）

**数据表**：medical_record（病历，appointment_id 唯一）、drug（药品字典，10 条演示数据）、prescription（处方明细，名称/规格/单价开方时快照）、bill（账单）。金额统一 DECIMAL(10,2)/BigDecimal（ALTER 的，不在旧备份 SQL 里）。

**流程**：
1. 医生端「查看挂号」卡片点「接诊」→ 病历建档弹窗（主诉自动带入挂号描述、体格检查/诊断*/治疗方案/医嘱/内部备注/复诊日期、挂号费+诊疗费、处方表格选药品+数量+用法实时算价）→ `POST /record/save`（DOCTOR，事务：插病历+处方+生成未付账单+挂号标记完成）
2. 后台「收费列表」：待收费高亮，余额收款（校验并扣 client_balance）/现金收款 → `PUT /bill/pay/{id}`（NURSE，事务：扣余额+校验库存+扣库存发药+标记已付）；限余额不足时可顺手「充」余额
3. 后台「药品列表」：CRUD+分类筛选+库存告警（≤5 黄/0 红）+快速入库；停用药品不可再开方
4. 顾客端宠物卡片「就诊记录」→ 就诊史（日期/诊断/医生/费用/缴费状态）→ 病历详情（含处方，内部备注不返回、体格检查可见）

**收费打印（2026-10-12 新增，前端 y-billList.vue）**：
- 收银小票：80mm 单栏式（独立窗口 window.print，不起服务端），收款成功自动弹出 + 已结算账单「票」按钮重打
- 正规发票：A4 增值税普通发票版式（代码/号码/购销双方/明细/价税合计/大写金额 moneyCN/收款复核开票人三栏/备注），已结算账单「发」按钮打印
- 说明：非税务联网，仅内部记账/报销凭证；税率暂按免征（★/0.00）；发票号暂按 billId 占位；抬头暂固定「宠物医院管理系统」

**权限**：record/save=DOCTOR、drug 管理=MANAGER、drug 下拉/bill 收款=NURSE、record 查询=登录即可（顾客只能查自己宠物，归属校验在 Controller）
**注意**：后端已引入 Lombok（pom）；重复接诊被拦（appointment_id 唯一）；开方时校验库存，发药（收款）时再校验+扣减；已收款的账单不可重复收款


## 医生端改造（2026-10-12，后端 + 前端）

### 职位与科室分类（贴合宠物医院行业）
- 职位称谓：院长(8) / 主任(6) / 主治医师(4) / 护理技师(2) / 实习助理(0)，权限等级不变，仅优化显示称谓（JobUtil.ts、TagJob.vue、w-doctorOne.vue、x-doctorList.vue 已同步）
- 科室由 5 个扩为 9 个（数据库动态读取，已执行迁移到运行库）：内科 / 骨科 / 化验室 / 美容洗护室 / 外科 / 影像科 / 皮肤科 / 中兽医科 / 眼科与口腔科；保留原 id 与医生外键关联

### 医生个人资料页（w-doctorOne.vue）
- 重构为卡片式主页：封面渐变横幅 + 大头像 + 职级徽章 + 姓名/账号 + 信息网格（性别/年龄/电话/邮箱/工号/入职日期/星座）+ 个人简介 + 修改资料/重置密码
- 工号（DOCTOR-xxxx，由 doctorId 派生）、入职日期（createTime）、星座（生日派生），均纯前端、不改库
- 视觉升级「科技玻璃拟态」：深色蓝紫渐变横幅、玻璃卡片 + 蓝紫描边、信息卡悬浮发光、职级改为渐变标签跟在姓名旁（不再叠在头像上）

### 医生工作台（doctorPage/index.vue）
- 顶部只保留「个人中心」，去掉与工作台卡片重复的菜单入口
- 页头统计：待办挂号数、已处理数；问候显示真实医生姓名
- 处理挂号卡：实时预览前 3 条待办（宠物/主人/状态/描述/时间），超 3 条提示
- 在线咨询卡：展示最近在线客户 + 查看消息入口
- 底部预留：化验管理 / 住院管理 / 更多功能 占位卡

### 修复
- admin 修改资料报「修改失败」：updateRole 补全 `form.doctorId = doctor.doctorId`（ADMIN 走 reqUpdateDoctor，updateById 依赖主键，缺 doctorId 时 update 0 行）

## 待办

### 已回测（2026-10-12，脚本 dev-tools 已理顺）
- 管理员登录 + 顾客登录 ✅
- 顾客自建宠物（POST /pet/self，pet_source=1）✅
- 顾客编辑资料（写身份证）✅
- 预约挂号可选性 ✅
- 充值闭环（正负/超限校验）✅
- 首改密流程（重置→强制改密）✅
- 收费结算（admin 账号可正常收款）✅
- 打印小票/发票 ✅

### 脚本优化（2026-10-12，dev-tools/）
- stop.sh：改为按端口精准停止（ss/fuser/lsof 降级+优雅杀+超时强杀），解决旧 pkill 杀不干净导致端口占用卡死
- start.sh：先停干净→统一日志 /tmp/pet-backend.log→等「Started ShipPetApplication」真就绪
- rb.sh：加回滚确认提示（防误丢未提交改动）
- check.sh：清理重复命令，单次编译检查

### 明天计划（2026-10-12，剩余）
1. **回测今天的功能**（后端需先重启）：
   - 顾客添加宠物（完整表单+待审核）→ 后台审核通过/驳回 → 挂号可选性
   - 顾客端资料编辑（身份证/年龄推算）
   - 头像/聊天图片压缩（手机拍 3MB 大图）+ 随机头像模板
   - 首改密流程（管理员重置密码 → 顾客登录被强制改密；注册流程不再被误踢）
2. **邮件功能**（需用户先拿 QQ 邮箱授权码）：配 MAIL_USER/MAIL_PASS → 测邮件发送 → 重构忘记密码邮箱找回
3. 时间富余的话：主页/列表页细节打磨

### 待测试
- [ ] 邮件发送（需先配 QQ 邮箱授权码到 MAIL_USER/MAIL_PASS 环境变量）
- [ ] 手机端按钮错位修复效果
- [ ] 后台用户管理手机号唯一性校验
- [ ] 顾客改邮箱 / 改密码（改密码已实现，邮箱入口待补）

### 待开发（建议顺序）
- [ ] 忘记密码改成邮箱找回（现 findMail 逻辑需重构）

### 上线前
- [ ] 删除测试数据
- [ ] 改默认密码 123456
- [ ] 配置 HTTPS 和域名（注意：聊天 WebSocket 现按 `location.hostname:8085` 直连，上 HTTPS 后需给 8085 配 wss 反向代理）
- [ ] 上线重导数据库时记得带 pet_source 字段（ALTER 的，不在旧备份 SQL 里）

## 关键决策

1. 账号 = 手机号，不开放自主注册，前台录入客户时自动生成
2. 密码统一 123456，顾客可自己改；忘记密码三种方式（后台重置 / 手机号 / 邮箱），手机号找回因短信服务成本暂缓
3. 只存生日（真相源），年龄前端实时计算；反推的生日标记为估算
4. pet_source 字段已加（0后台录入 1顾客自建），顾客自建宠物走 POST /pet/self
5. **`pet_alive`（是否活着，兽医判断）和 `active`（系统启用/停用）严格分开**。删除宠物时：无任何引用记录才真删，有记录改 `active=0` 停用（保留病历/领养/过户历史）
6. **宠物归属状态机**：
   - 有主 ──(遗弃/托管)──→ 无主 ──(申请领养)──→ 待审核 ──(通过)──→ 新主人
   - 新主人 ──(撤回/退养)──→ 无主
   - 已实现：审核通过、撤回、拒绝、重启、入栏（PUT /pet/intake，有主→无主）、顾客退养（PUT /adopt/returnPet，新主人→无主，in_adopt=3）
   - 待实现：无

## 数据库

- 库名：pet_ship
- 账号：mixpet / MixPet2024
- sudo socket: sudo mysql

## 常用命令

后端启动：
    cd ~/公共/pet-hms/shipPet && mvn spring-boot:run

前端启动：
    cd ~/公共/pet-hms/ship-ui && npm run dev

编译检查：
    cd ~/公共/pet-hms/shipPet && mvn clean compile -q
    cd ~/公共/pet-hms/ship-ui && npm run build

数据库备份：
    mysqldump -u mixpet -pMixPet2024 pet_ship > ~/backup/pet_ship_$(date +%Y%m%d).sql

## 给下一个 AI

先读 CONTEXT.md，然后看我这次想做什么。
改动前先确认代码现状，给 git patch 比逐条命令更高效。
特别看「脚本避坑清单」，那些是真实踩过的坑。

---

## 与 AI 协作的工作流（每次开新会话读这段）

**用户开场白**：「读 ~/公共/pet-hms/CONTEXT.md」

**协作模式**：
1. 用户说想法（一句话描述想改什么）
2. AI 直接给一条完整命令（含：打快照 + 改代码 + 编译 + diff 输出）
3. 用户复制粘贴，跑完贴结果
4. AI 看效果，给下一步或修正

**不用来回问"这段代码长什么样"** —— AI 自己在脚本开头加诊断段。

**用户可用的三个工具脚本**（放在 dev-tools/，不进 Git）：
- `~/公共/pet-hms/dev-tools/snap.sh` — 打快照（可省略）
- `~/公共/pet-hms/dev-tools/rb.sh` — 回滚到最近快照（不满意时用）
- `~/公共/pet-hms/dev-tools/check.sh` — 编译前后端

**AI 给脚本的约定**：
- 一次动完，不留半成品
- 用 `cat > 文件 <<'EOF'` 整体覆盖，不用 sed 拼
- 脚本末尾必须带 `npm run build` 或 `mvn compile` 验证
- 脚本末尾带 `git --no-pager diff --stat` 让用户看改动范围
- 简单改动一条命令，复杂改动分步但一次给全
- **改 `.vue` 模板结构风险高，优先做纯文本替换**
- **改 Java 类追加方法，直接 printf '}\n' >> 文件**