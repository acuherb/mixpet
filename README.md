# pet-hms

> 宠物医院管理系统，基于 [MixPet](https://github.com/MixJade/MixPet) 重置版二次开发。
> 用于本地自建、局域网部署。

---

## 项目说明

- **来源**：本项目最初由 [MixJade](https://github.com/MixJade) 开发，名为 MixPet，是一个宠物医院管理系统的毕设作品。
- **本仓库**：在 MixPet 重置版（`shipPet` + `ship-ui`）基础上继续迭代。
- **用途**：自建使用，非公开产品。
- **当前状态**：预约、用户/医生/管理员管理、在线咨询、寄养、领养等基础模块可用；电子病历、处方、收费、库存、住院等功能尚未实现。

---

## 技术栈

| 层 | 技术 |
| :--- | :--- |
| 后端 | Java 17 + Spring Boot 3.1 + Spring Security 6 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8 / MariaDB 11 |
| 前端 | Vue 3 + Vite 4 + TypeScript + Element Plus |
| 实时通信 | WebSocket |
| 邮件 | Spring Boot Mail（QQ 邮箱 SMTP） |

---

## 目录结构

```text
pet-hms/
├── shipPet/            # 后端（Spring Boot）
│   └── src/main/
│       ├── java/com/ship/    # 代码
│       └── resources/        # 配置、Mapper XML
├── ship-ui/            # 前端（Vue 3 + Vite）
│   └── src/
│       ├── components/       # 组件
│       ├── views/            # 页面
│       ├── request/          # API 封装
│       ├── router/           # 路由
│       └── model/            # 数据模型
├── images/             # 宠物/医生头像等上传图片
├── notice/             # 网站公告文本
├── chatImg/            # 聊天上传图片
├── 数据库备份/
│   └── pet_ship.sql    # 数据库初始化脚本
└──  DEPLOY.md           # 完整本地部署教程
```

---

## 快速开始

完整部署步骤见 [DEPLOY.md](./DEPLOY.md)。下面是概要：

### 环境要求

- JDK 17（推荐 Temurin）
- Maven 3.8+
- Node.js 18+
- MySQL 8 或 MariaDB 11

### 1. 数据库

```bash
sudo mysql <<'EOF'
CREATE DATABASE pet_ship CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE USER 'mixpet'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON pet_ship.* TO 'mixpet'@'localhost';
FLUSH PRIVILEGES;
EOF

mysql -u mixpet -p pet_ship < 数据库备份/pet_ship.sql
```

### 2. 后端

```bash
cd shipPet
mvn spring-boot:run
```

启动成功后监听 `http://localhost:8085`。

### 3. 前端

```bash
cd ship-ui
npm install
npm run dev
```

启动成功后访问 `http://localhost:3120`。

---

## 配置说明

后端配置已改为**环境变量写法**，本地开发不用改代码即可运行：

| 环境变量 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `DB_USER` | `mixpet` | 数据库账号 |
| `DB_PASSWORD` | `MixPet2024` | 数据库密码 |
| `MAIL_USER` | `your_qq@qq.com` | 发件 QQ 邮箱 |
| `MAIL_PASS` | `your_auth_code` | QQ 邮箱 SMTP 授权码 |
| `PET_IMAGES` | `~/公共/pet-hms/images/` | 照片存储路径 |
| `PET_NOTICE` | `~/公共/pet-hms/notice/` | 公告存储路径 |
| `PET_CHATIMG` | `~/公共/pet-hms/chatImg/` | 聊天图片存储路径 |

换机器部署时只需设这些环境变量，不用改代码。详见 DEPLOY.md。

---

## 默认账号

| 角色 | 账号 | 密码 | 入口 |
| :--- | :--- | :--- | :--- |
| 顾客 | `ying` | `123456` | 直接登录 |
| 医生 | `yun` | `123456` | 点登录卡片上的**猫猫头**切换 |
| 管理员 | `admin` | `123456` | 点登录卡片上的**猫猫头**切换 |

其他账号可从后台「用户管理」「医生管理」中查看。

> ⚠️ 正式使用前请务必修改所有默认密码。

---

## 已实现功能

- 用户登录、找回密码（邮箱验证码），不开放自主注册，由前台录入
- 宠物档案管理
- 医生管理、科室管理
- 预约挂号
- 在线咨询（WebSocket 聊天）
- 领养信息
- 寄养信息
- 网站公告

## 待开发功能

- 电子病历、处方、医嘱
- 收费/订单/支付
- 药品/耗材库存
- 住院管理
- 疫苗/驱虫记录
- 检验/影像上传
- 报表统计

---

## 致谢与许可

- 原始项目：[MixJade/MixPet](https://github.com/MixJade/MixPet)
- 原始项目许可证见 [LICENSE](./LICENSE)
- 二次开发部分版权归本仓库作者所有


---

### 启动前端

```bash
cd ~/公共/pet-hms/ship-ui
npm run dev
```

### 启动后端

```bash
cd ~/公共/pet-hms/shipPet
mvn spring-boot:run
```

