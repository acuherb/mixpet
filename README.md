# MixPet 智慧宠物医疗管理系统

> 面向**小型宠物医院日常经营落地**的全流程管理系统，本地/局域网自建部署。
> 基于 [MixPet](https://github.com/MixJade/MixPet) 二次开发，已从毕设原型演进为可实际使用的生产系统。

---

## 系统定位

本系统覆盖宠物医院**前台接待 → 诊疗 → 收费发药 → 库存采购 → 客户运营 → 经营分析**的完整闭环，目标是在本地部署、局域网内供一家小型宠物医院的实际日常使用。

**核心设计原则（生产标准）**

- 账号即手机号，不开放自主注册，客档由前台录入
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
- 首页展示：医院公告 / 待领养宠物 / 医生团队 / 寄养信息
- 宠物档案（首字头像 / 体重曲线 / 一页式健康档案）
- 预约挂号（号源容量校验、可改期/取消、复诊关联）
- 在线咨询（文字 + 图片 WebP + 文件、留言）
- 就诊记录（病历 / 处方 / 检验 / 影像 / 住院 / 手术 / 套餐详情）
- 余额充值、会员卡 / 次卡 / 积分兑换

### 医生端
- 处理台：待办统计 / 挂号预览 / 在线咨询
- 接诊：病历建档（含分诊体征）、开处方、开检验 / 影像单、安排手术 / 套餐
- 化验 / 影像结果录入、住院监护、手术与麻醉记录、知情同意书
- 国标兽医处方笺 / 检验申请单 / 影像申请单打印
- 复诊计划、随访记录

### 管理后台
- 经营看板 + 财务报表（收入构成 / 医生 Top / 药品毛利 / 环比）
- 收费 / 发药核对 / 退款 / 质检作废 / 收银小票 + A4 发票打印
- 药品与库存：进销存、低库存 / 近效期预警、采购入库（供应商 / 批号）
- 客户运营：客档管理、会员等级、次卡模板、随访提醒
- 诊疗管理：住院、手术、检验项 / 影像项字典、病历模板、疫苗驱虫
- 组织管理：科室、医生（多科室 / 职称 / 权限）、医生排班
- 系统管理：统一设置中心（化验分组 / 药品分类 / 单位等字典）、聊天数据清理
- 自动提醒：复诊 / 疫苗到期、预约就诊前、疫苗提醒（每日定时生成系统公告）

---

## 目录结构

```text
mixpet/
├── shipPet/              # 后端（Spring Boot 3 + Java 17）
│   └── src/main/
│       ├── java/com/ship/    # 代码
│       └── resources/        # 配置、Mapper XML
├── ship-ui/              # 前端（Vue 3 + Vite + TS）
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
├── db-backup/             # SQL 脚本（含各功能增量迁移脚本）
├── deploy.sh             # 一键部署脚本（构建 + nginx 反代 + 启动）
└── DEPLOY.md             # 完整本地部署教程
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
CREATE DATABASE pet_ship CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE USER 'mixpet'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON pet_ship.* TO 'mixpet'@'localhost';
FLUSH PRIVILEGES;
EOF

mysql -u mixpet -p pet_ship < db-backup/pet_ship.sql
```

### 2. 后端

```bash
cd shipPet
mvn spring-boot:run        # 监听 http://localhost:8085
```

### 3. 前端

```bash
cd ship-ui
npm install
npm run dev                # 访问 http://localhost:3120
```

### 4. 一键部署（生产环境）

仓库根目录提供 `deploy.sh`，自动完成**前端构建 → 后端打包 → 生成 nginx 反代配置 → 启动后端**：

```bash
# 先按 DEPLOY.md 建库并导入基线，再补齐 24 个增量迁移脚本
# 设置必要环境变量（至少 DB_PASSWORD / APP_DEFAULT_PASSWORD）
export MIXPET_DIR=/opt/mixpet                 # 项目目录（建议纯英文路径）
export MIXPET_DOMAIN=pet.你的诊所域名.com
export DB_PASSWORD=你的强口令
export APP_DEFAULT_PASSWORD=你的强口令

bash deploy.sh            # 构建 + 生成 nginx 配置 + 启动后端
bash deploy.sh --tls      # 额外申请 Let's Encrypt 证书并启用 HTTPS/WSS
bash deploy.sh --firewall # 额外配置 ufw（放行 22/443 并启用）
```

> 上线前仍须：① 改默认口令 ② 清测试数据（见 [DEPLOY.md](./DEPLOY.md) 10.2）。脚本不执行数据库迁移与清数据。

---

## 配置说明

后端配置采用**环境变量写法**，换机部署不改代码：

| 环境变量 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `DB_USER` | `mixpet` | 数据库账号 |
| `DB_PASSWORD` | `MixPet2024` | 数据库密码 |
| `MAIL_USER` | `your_qq@qq.com` | 发件 QQ 邮箱 |
| `MAIL_PASS` | `your_auth_code` | QQ 邮箱 SMTP 授权码 |
| `PET_AVATAR` | `~/公共/mixpet/avatar/` | 头像 / 报告附件存储路径 |
| `PET_NOTICE` | `~/公共/mixpet/notice/` | 公告配图存储路径 |
| `PET_CHATIMG` | `~/公共/mixpet/chat/` | 聊天文件存储路径 |
| `APP_DEFAULT_PASSWORD` | `MixPet@Prod2026!` | 新建/重置账号默认口令，**生产务必用环境变量覆盖** |

---

## 默认账号

> **初始登录流程**：系统初始只有管理员 `admin` 可登录；顾客、医生账号均由管理员在后台录入/创建（本项目不开放自主注册，客档由前台录入）。顾客新建后默认密码 `123456`，**首次登录会被强制跳转改密**（前端 `/changePwd` 守卫），改密后才能正常使用账号。
>
> 三类演示账号密码均为 `123456`，上线前必须修改。顾客端**账号即手机号**——新客档录入时登录账号自动取手机号（`ClientServiceImpl.addClient`）；基线演示库预置的顾客账号 `ying`、医生账号 `yun` 便于直接体验，上线前请改为真实信息或重置。

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

`db-backup/` 下除原始基线 `pet_ship.sql` 外，还有按功能/日期命名的**增量迁移脚本**（如 `feature_20261015_chat_file_webp.sql`）。开发过程中新增的字段/表都以独立脚本保存并执行到运行库，便于留档与追查。重导全新环境时，请按脚本补齐各项增量。运行 `deploy.sh` 前也须先按 DEPLOY.md 建库并补齐全部增量脚本——脚本本身不执行数据库迁移。

---

## 致谢与许可

- 原始项目：[MixJade/MixPet](https://github.com/MixJade/MixPet)
- 原始项目许可证见 [LICENSE](./LICENSE)
- 二次开发部分版权归本仓库作者所有