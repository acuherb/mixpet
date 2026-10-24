# MixPet 智慧宠物医疗管理系统

> 面向**小型宠物医院日常经营落地**的全流程管理系统，本地/局域网自建部署。
> 基于 [MixPet](https://github.com/MixJade/MixPet) 二次开发，已从毕设原型演进为可实际使用的生产系统。

---

## 系统定位

本系统覆盖宠物医院**前台接待 → 诊疗 → 收费发药 → 库存采购 → 客户运营 → 经营分析**的完整闭环，目标是在本地部署、局域网内供一家小型宠物医院的实际日常使用。

**核心设计原则（生产标准）**

- 账号即手机号；支持现场自助注册（局域网平板/手机，需管理员审核通过后登录），也可由前台录入
- 全程国标单据：兽医处方笺、检验/影像申请单、手术知情同意书（可打印）
- 金额计算防并发超扣、库存台账可追溯、退款冲正/单据作废
- 安全性按生产要求：越权收口、接口脱敏、路径穿越防护、密码盐化

---

## 技术栈

| 层 | 技术 |
| :--- | :--- |
| 后端 | Java 17 + Spring Boot 3.1 + Spring Security 6 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8 / MariaDB 11 |
| 前端 | Vue 3 + Vite 4 + TypeScript + Element Plus |
| 实时通信 | WebSocket（在线咨询） |
| 图片 | WebP（上传自动转码，省空间保清晰度） |
| 邮件 | Spring Boot Mail（QQ 邮箱 SMTP） |

---

## 功能总览

### 前台（顾客端）
- 首页展示：医院公告（排序权重置顶 + 创建时间 + 更多公告列表）/ 待领养宠物 / 医生团队 / 寄养信息
- 账号：现场自助注册（完整信息、手机号即账号、等待管理员审核）/ 找回密码
- 首页细节：顶栏头像昵称、未登录入口引导登录、医生回复实时提醒、回到顶部、图片懒加载
- 宠物档案（首字头像 / 体重曲线 / 一页式健康档案、就诊记录入口）
- 预约挂号（号源容量校验、可改期/取消、复诊关联）
- 在线咨询（文字 + 图片 WebP + 文件、留言，双向已读未读 + 新消息提醒）
- 就诊记录（病历 / 处方 / 检验 / 影像 / 住院 / 手术 / 套餐详情）
- 挂号**爽约可见**：医生标记爽约后记录保留（灰色「已爽约」），不再凭空消失，可重新预约
- 首页直达：登录顾客首屏「预约挂号 / 在线咨询」按钮（未登录引导登录注册）
- 余额充值、会员卡 / 次卡 / 积分兑换

### 医生端
- 处理台：诊疗待办看板（待出结果化验 / 待出报告影像 / 待手术 / 待随访 / 接诊中病历 / **待审核顾客**）/ 挂号预览 / 在线咨询（客户列表 + 新咨询实时提醒）
- **今日待办**：待就诊 / 待收费（收银从旁路跳转改为待办驱动）/ 全部挂号，工作流时间线混排待接诊与接诊中（明日/后续单有标识）
- 接诊向导（4 步闭环）：问诊检查 → 诊断医嘱 → 处置开单（处方/化验/影像/套餐/手术/住院，含补开）→ 完结结算
- 化验/影像：**统一「医技结果录入」入口**（页签切换检验/影像，常规开单只在向导内）、申请单打印
- 挂号管理：**列表/卡片双视图**（列表一屏看全貌，选择记忆）、行内**改期**（患者电话改时间不切后台）、标记爽约（记录保留可查）
- 住院监护、手术与麻醉记录、知情同意书
- 病例中心：病历多条件检索 + 全量病例档案（一页看全：病历/处方/套餐/化验含明细/影像/住院/手术/免疫/随访/复诊链）
- 国标兽医处方笺 / 检验申请单 / 影像申请单打印
- 复诊计划、随访记录（病历号下拉选择，杜绝手填串档）

### 管理后台
- 经营看板 + 数据中心（收入构成 / 医生 Top / 药品毛利 / 环比 / **待审核顾客卡**）
- 收费 / 发药核对 / 退款 / 质检作废 / 收银小票 + A4 发票打印（病历弹窗为完整病例档案）
- **退款支持按处方行部分退**（勾选行 + 实时应退金额，退哪些、剩哪些一目了然）
- 药品与库存：进销存、低库存 / 近效期预警、采购入库（供应商 / 批号）
- 客户运营：客档管理、会员等级、次卡模板、随访提醒、**注册顾客审核（工作台/看板待办提醒 + 客户管理审核状态筛选直达）**
- 诊疗管理：住院（**床位/护理/输液分项计价 + 押金收取累计 + 医嘱临时费用 + 结算明细可核对**）、手术、检验项 / 影像项字典、病历模板、疫苗驱虫、病例中心（检索 + 全量档案）
- **寄养管理**：按天计费（笼位/清洁/餐费分项 + 预估）、状态流转（待入托→寄养中→已接回）、接回结算按实际天数多退少补
- 组织管理：科室、医生（多科室 / 职称 / 权限）、医生排班
- 系统管理：统一设置中心（化验分组 / 药品分类 / 单位等字典）、聊天数据清理（保留天数可设 0 全删）、公告排序权重设置
- 自动提醒：复诊 / 疫苗到期、预约就诊前、疫苗提醒（每日定时生成系统公告）
- 资源清理：每日凌晨自动清理无引用头像 / 冗余公告 / 聊天图片，更换头像即时删旧文件

---

## 目录结构

```text
mixpet/
├── mixPet/              # 后端（Spring Boot 3 + Java 17）
│   └── src/main/
│       ├── java/com/mix/    # 代码（启动类 MixPetApplication）
│       └── resources/        # 配置、Mapper XML
├── mix-ui/              # 前端（Vue 3 + Vite + TS）
│   └── src/
│       ├── components/       # 组件
│       ├── views/            # 页面（reception 顾客端 / doctorPage 医生端 / backstage 管理端）
│       ├── request/          # API 封装
│       ├── router/           # 路由
│       └── model/            # 数据模型
├── avatar/               # 头像存储（WebP）
├── notice/               # 公告配图
├── chat/                 # 聊天文件（image/ 图片、file/ 文件）
├── report/               # 报告附件存储（image/ 与 file/ 子目录）
├── db-backup/             # SQL 脚本（基线 + 51 个增量迁移，索引见 INDEX.md）
├── dev-tools/             # 开发辅助（start.sh 一键启停 / stop.sh / check.sh 等，见 WORKFLOW.md）
├── deploy.sh             # 一键部署脚本（构建 + nginx 反代 + 启动）
├── .env                  # 本机环境变量（不入库；模板 .env.example）
├── CONTEXT.md            # 项目上下文与迭代日志（AI 协作约定）
├── DEPLOY.md             # 完整本地部署教程
└── README.md             # 本文件
```

---

## 快速开始

完整部署步骤见 [DEPLOY.md](./DEPLOY.md)。概要：

### 环境要求
- JDK 17（推荐 Temurin）、Maven 3.8+
- Node.js 18+
- MySQL 8 或 MariaDB 11

### 1. 创建数据库并导入

```bash
sudo mysql <<'EOF'
CREATE DATABASE pet_mix CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE USER 'mixpet'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON pet_mix.* TO 'mixpet'@'localhost';
FLUSH PRIVILEGES;
EOF

# 基线 + 全部 51 个增量脚本（按文件名顺序，缺一不可）
mysql -u mixpet -p pet_mix < db-backup/pet_mix.sql
for f in $(ls db-backup/*.sql | grep -v pet_mix.sql | sort); do
  mysql --default-character-set=utf8mb4 -u mixpet -p pet_mix < "$f"
done
```

### 2. 配置环境变量

```bash
cp .env.example .env    # 填入 DB_USER/DB_PASSWORD/APP_DEFAULT_PASSWORD/REMEMBER_ME_KEY/VERIFY_CODE_SECRET
```

### 3. 一键启动（推荐）

```bash
~/公共/mixpet/dev-tools/start.sh    # 后端 8085 就绪后自动起前端 3120；stop.sh 停止
```

或手动：`cd mixPet && set -a; source ../.env; set +a && mvn spring-boot:run` + `cd mix-ui && npm run dev`

### 4. 一键部署（生产环境）

仓库根目录提供 `deploy.sh`，自动完成**前端构建 → 后端打包 → 生成 nginx 反代配置 → 启动后端**：

```bash
# 先按 DEPLOY.md 建库并导入基线，再补齐 24 个增量迁移脚本
# 设置必要环境变量（至少 DB_PASSWORD / APP_DEFAULT_PASSWORD / REMEMBER_ME_KEY / VERIFY_CODE_SECRET）
export MIXPET_DIR=/opt/mixpet                 # 项目目录（建议纯英文路径）
export MIXPET_DOMAIN=pet.你的诊所域名.com
export DB_USER=your_db_user
export DB_PASSWORD=your_secure_password
export APP_DEFAULT_PASSWORD=your_secure_default_password
export REMEMBER_ME_KEY=your_32_char_secret_key_here!
export VERIFY_CODE_SECRET=your_32_char_jwt_key_here!

bash deploy.sh            # 构建 + 生成 nginx 配置 + 启动后端
bash deploy.sh --tls      # 额外申请 Let's Encrypt 证书并启用 HTTPS/WSS
bash deploy.sh --firewall # 额外配置 ufw（放行 22/443 并启用）
```

> 上线前仍须：① 改默认口令 ② 清测试数据（见 [DEPLOY.md](./DEPLOY.md) 10.2）。脚本不执行数据库迁移与清数据。

---

## 配置说明

后端配置采用**环境变量写法**，换机部署不改代码。所有密钥类变量**生产必须设置**，禁止使用默认值。

| 环境变量 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `DB_USER` | *（无）* | 数据库账号，**生产必须设置** |
| `DB_PASSWORD` | *（无）* | 数据库密码，**生产必须设置** |
| `MAIL_USER` | `your_qq@qq.com` | 发件 QQ 邮箱（开发可选） |
| `MAIL_PASS` | `your_auth_code` | QQ 邮箱 SMTP 授权码（开发可选） |
| `PET_AVATAR` | `/home/debian/公共/mixpet/avatar/` | 头像 / 报告附件存储路径 |
| `PET_NOTICE` | `/home/debian/公共/mixpet/notice/` | 公告配图存储路径 |
| `PET_CHATIMG` | `/home/debian/公共/mixpet/chat/` | 聊天文件存储路径 |
| `APP_DEFAULT_PASSWORD` | *（无）* | 新建/重置账号默认口令，**生产必须设置** |
| `REMEMBER_ME_KEY` | *（无）* | 记住我 Cookie 签名密钥（至少 32 字符），**生产必须设置** |
| `VERIFY_CODE_SECRET` | *（无）* | JWT 验证码令牌密钥（至少 32 字符），**生产必须设置** |

> ⚠️ **安全提示**：以上所有默认值为空，生产部署时必须通过环境变量设置。建议复制 [.env.example](./.env.example) 并填入真实值后，在启动脚本中通过 `source` 加载。

---

## 默认账号

> **初始登录流程**：系统初始只有管理员 `admin` 可登录；医生账号由管理员在后台创建，顾客账号有两种来源：① 管理员后台录入（默认 `123456`，**首次登录强制改密**，前端 `/changePwd` 守卫）；② 顾客现场自助注册（手机号即账号、密码自设、无需改密），注册后为**待审核**状态，管理员在后台「顾客列表」审核通过后才能登录（拒绝则无法登录并提示原因）。
>
> 三类演示账号密码均为 `123456`，上线前必须修改。顾客端**账号即手机号**（录入/注册均如此）；基线演示库预置的顾客账号 `ying`、医生账号 `yun` 便于直接体验，上线前请改为真实信息或重置。

| 角色 | 登录标识 | 密码 | 入口 |
| :--- | :--- | :--- | :--- |
| 管理员 | `admin` | `123456` | 登录时点击**猫猫头**切换（初始唯一入口） |
| 顾客 | 手机号（演示库预置 `ying`） | `123456` | 顾客端「手机号 + 密码」登录，**首次须改密** |
| 医生 | `yun` | `123456` | 登录时点击**猫猫头**切换（由管理员创建） |

> ⚠️ 正式投入使用前，务必：
> - 修改所有默认密码：代码侧默认口令已改为环境变量 `APP_DEFAULT_PASSWORD`（部署时 `export APP_DEFAULT_PASSWORD=你的强口令`）；存量演示账号 `admin`/`yun`/`ying` 须到后台「用户管理 / 医生管理」逐个重置
> - 清除测试数据
> - 配置 HTTPS / 为 WebSocket 配 `wss` 反向代理（完整步骤见 [DEPLOY.md](./DEPLOY.md) 10.2）

---

## 数据库迁移说明

`db-backup/` 下除原始基线 `pet_mix.sql` 外，还有按功能/日期命名的**增量迁移脚本**（51 个，完整索引见 [`db-backup/INDEX.md`](./db-backup/INDEX.md)）。开发过程中新增的字段/表都以独立脚本保存并执行到运行库，便于留档与追查。近期新增：

| 脚本 | 内容 |
| :--- | :--- |
| `feature_20261020_package_item.sql` | 套餐结构化：可选检查项目子表（检验/影像快照组合定价） |
| `feature_20261020_bill_fee_immune.sql` | bill 补免疫费分项 |
| `feature_20261020_bill_pay_by.sql` | bill 收款人/收款时间留痕 |
| `feature_20261020_drug_inbound_cost.sql` | 入库成本链路（药品毛利） |
| `feature_20261020_hospital_advice.sql` | 住院医嘱单表 |
| `feature_20261020_is_del_default_fix.sql` | 存量表逻辑删除默认值一致性 |
| `feature_20261021_bill_refund_reason.sql` | 退款原因独立列 |
| `feature_20261022_query_indexes.sql` | 高频查询索引补齐（幂等） |
| `feature_20261022_hospital_foster_billing.sql` | 住院分项计价+押金+医嘱临时费；寄养按天计费+状态流转（幂等） |
| `feature_20261023_appointment_miss.sql` | 挂号爽约状态位（记录保留可查，幂等） |

重导全新环境时，请按文件名顺序补齐全部增量；运行 `deploy.sh` 前也须先建库并补齐全部增量脚本——脚本本身不执行数据库迁移。

---

## 致谢与许可

- 原始项目：[MixJade/MixPet](https://github.com/MixJade/MixPet)
- 原始项目许可证见 [LICENSE](./LICENSE)
- 二次开发部分版权归本仓库作者所有