# MixPet 智慧宠物医疗管理系统 - 本地部署完整教程

> 基于 [MixPet](https://github.com/MixJade/MixPet) 重置版二次开发
> 环境：Debian 13 (Trixie) / 局域网部署 / 2026-10 实测通过

---

## 一、环境准备

### 1.1 系统要求
- Debian 13 (Trixie) 或兼容的 Linux 发行版
- 建议 4GB 以上内存
- 局域网内可访问

### 1.2 安装 JDK 17

Debian 13 默认源没有 `openjdk-17-jdk`，需通过 Adoptium 源安装：

```bash
sudo apt install -y wget apt-transport-https gnupg

sudo wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg

echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt update
sudo apt install -y temurin-17-jdk
```

### 1.3 设置 JDK 17 为默认

```bash
sudo update-alternatives --config java
# 选 temurin-17-jdk-amd64 对应编号

echo 'export JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

java -version   # 应显示 17.0.x
mvn -version    # Java version 应为 17.0.x
```

### 1.4 安装其他依赖

```bash
sudo apt install -y maven

node -v   # 应 >= v18
npm -v

sudo apt install -y mariadb-server
sudo systemctl enable --now mariadb
```

---

## 二、获取项目代码

```bash
cd ~/公共
git clone git@github.com:acuherb/mixpet.git
cd mixpet
```

目录结构：
- `mixPet/` — 后端（Spring Boot 3 + Java 17）
- `mix-ui/` — 前端（Vue 3 + Vite）
- `db-backup/` — SQL 脚本（基线 `pet_mix.sql` + 24 个增量迁移脚本）
- `avatar/` — 头像图片（WebP）
- `notice/` — 公告配图
- `chat/` — 聊天文件（其下分 `image/` 图片、`file/` 文件两子目录）
- `report/` — 报告附件（其下分 `image/` 与 `file/` 两子目录）
- `deploy.sh` — 一键部署脚本（构建 + nginx 反代 + 启动）

---

## 三、数据库配置

### 3.1 创建数据库和专用账号

```bash
sudo mysql <<'EOF'
CREATE DATABASE pet_mix CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE USER 'mixpet'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON pet_mix.* TO 'mixpet'@'localhost';
FLUSH PRIVILEGES;
EOF
```

> 如果 `utf8_general_ci` 报错，改用 `utf8mb3` 和 `utf8mb3_general_ci`。

### 3.2 导入数据

```bash
mysql -u mixpet -p pet_mix < ~/公共/mixpet/db-backup/pet_mix.sql
```

### 3.3 验证

```bash
mysql -u mixpet -p pet_mix -e "SHOW TABLES;"
```

应看到 9 张表：`adopt`、`appointment`、`client`、`department`、`doctor`、`foster`、`msg`、`notice`、`pet`。

---

## 四、后端配置与启动

### 4.1 配置说明

后端配置已改为**环境变量写法**，本地开发不用改代码，默认值就是本机路径和测试账号。

`application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pet_mix?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: ${DB_USER:mixpet}
    password: ${DB_PASSWORD:MixPet2024}
```

`application-dev.yml`：
```yaml
spring:
  mail:
    username: ${MAIL_USER:your_qq@qq.com}
    password: ${MAIL_PASS:your_auth_code}
---
pet-mix:
  images-path: ${PET_AVATAR:/home/debian/公共/mixpet/avatar/}
  notice-path: ${PET_NOTICE:/home/debian/公共/mixpet/notice/}
  chatImg-path: ${PET_CHATIMG:/home/debian/公共/mixpet/chat/}
```

### 4.2 换机器时怎么配

**方式 A（推荐）：设环境变量**，不改代码。

```bash
export DB_USER=mixpet
export DB_PASSWORD=你的数据库密码
export MAIL_USER=你的QQ邮箱
export MAIL_PASS=你的QQ邮箱授权码
export PET_AVATAR=/你的/路径/avatar/
export PET_NOTICE=/你的/路径/notice/
export PET_CHATIMG=/你的/路径/chat/
```

写进 `~/.bashrc` 可永久生效。

**方式 B**：直接改 `application*.yml` 里的默认值。

### 4.3 启动后端

```bash
cd ~/公共/mixpet/mixPet
mvn spring-boot:run
```

成功标志：
```
Tomcat started on port(s): 8085 (http)
Started mixPetApplication in x.xx seconds
宠物医院管理系统2.0启动
http://localhost:8085
```

> ⚠️ 这个终端不要关，后端在前台运行。

---

## 五、前端配置与启动

### 5.1 换 npm 镜像

```bash
npm config set registry https://registry.npmmirror.com
```

### 5.2 安装依赖

新开一个终端（后端终端保持运行）：

```bash
cd ~/公共/mixpet/mix-ui
npm install
```

### 5.3 启动前端

```bash
npm run dev
```

成功标志：
```
VITE v4.3.9  ready in xxx ms
➜  Local:   http://localhost:3120/
```

### 5.4 局域网访问

改 `mix-ui/vite.config.ts`，在 `server` 块加 `host: '0.0.0.0'`：

```ts
server: {
    host: '0.0.0.0',
    port: 3120,
    proxy: {
        '/api': {
            target: "http://localhost:8085",
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, '')
        }
    }
}
```

重启前端后用 `http://本机IP:3120` 访问。查 IP：

```bash
ip addr | grep "inet "
```

---

## 六、登录账号

> **初始登录流程**：系统初始只有管理员 `admin` 可登录；顾客、医生账号均由管理员在后台录入/创建（不开放自主注册）。顾客新建后默认密码 `123456`，**首次登录强制改密**（前端 `/changePwd` 守卫，改后才能用）。顾客端**账号即手机号**：录入时登录账号自动取手机号（`ClientServiceImpl.addClient`）；基线演示库预置顾客 `ying`、医生 `yun` 便于体验。

| 角色 | 登录标识 | 密码 | 入口 |
| :--- | :--- | :--- | :--- |
| 管理员 | `admin` | `123456` | 点登录卡片上的**猫猫头**（初始唯一入口） |
| 顾客 | 手机号（演示库预置 `ying`） | `123456` | 顾客端「手机号 + 密码」登录，**首次须改密** |
| 医生 | `yun` | `123456` | 点登录卡片上的**猫猫头**（由管理员创建） |

其他账号可从后台「用户管理」「医生管理」中查看，密码都是 `123456`。

---

## 七、日常启停

### 启动
```bash
# 终端 1：后端
cd ~/公共/mixpet/mixPet && mvn spring-boot:run

# 终端 2：前端
cd ~/公共/mixpet/mix-ui && npm run dev
```

### 停止
在各终端按 `Ctrl+C`。

---

## 八、常见问题

### 8.1 `不支持发行版本 17`
Maven 用的 JDK 不是 17。检查 `mvn -version`，确认 `~/.bashrc` 里的 `JAVA_HOME`。

### 8.2 数据库连接失败
- 检查 MariaDB：`sudo systemctl status mariadb`
- 检查账号密码、环境变量是否一致
- 检查数据库是否创建：`SHOW DATABASES;`

### 8.3 邮件验证码发不出去
- 必须是 QQ 邮箱，且开启 **POP3/SMTP 服务**
- 填的是**16 位授权码**，不是 QQ 密码
- 检查垃圾邮件箱

### 8.4 前端接口 404
- 检查后端是否在跑（`http://localhost:8085`）
- 检查 `vite.config.ts` 代理是否指向 `http://localhost:8085`

### 8.5 局域网访问不了
- `vite.config.ts` 加了 `host: '0.0.0.0'`
- 检查防火墙：`sudo ufw status`
- 检查两台机器是否在同一网段

---

## 九、部署架构图

```
局域网内电脑浏览器
        │
        ▼
   http://192.168.1.13:3120   (前端 Vite Dev Server)
        │
        │ /api 请求代理
        ▼
   http://localhost:8085      (后端 Spring Boot)
        │
        ▼
   MariaDB pet_mix           (本地数据库)
```

---

## 十、后续开发提示

### 10.1 重要路径
- 后端代码：`mixPet/src/main/java/com/mix/`
- 后端配置：`mixPet/src/main/resources/`
- 前端代码：`mix-ui/src/`
- 前端路由：`mix-ui/src/router/`
- 前端 API 封装：`mix-ui/src/request/`

### 10.2 上线前安全加固清单（必改）
> 以下任一项未做，都不要正式对外提供服务。本项目账号=手机号、由前台录入，**不开放自主注册**；黑名单/停用账号（`active=0`）在直接登录与 remember-me 两端均已被拦截，无需额外处理。

1. **数据库强密码**：创建 `mixpet` 账号时用强口令，部署时 `export DB_PASSWORD=你的强口令`（已支持环境变量，不必改代码）。
2. **改掉所有默认口令 `123456`**：
   - 代码侧 `app.default-password` 已改为环境变量 `${APP_DEFAULT_PASSWORD:...}`，生产务必 `export APP_DEFAULT_PASSWORD=你的强口令`（见 4.1）。
   - 存量演示账号 `admin` / `yun` / `ying` 等的密码仍是 `123456`，必须登录后到「用户管理 / 医生管理」逐个**重置密码**，或让对应用户自行改密。
3. **邮件授权码**：`application-dev.yml` 的 `MAIL_USER` / `MAIL_PASS` 改成自己的 QQ 邮箱与 16 位授权码（或用环境变量 `MAIL_USER` / `MAIL_PASS`）。
4. **配置 HTTPS + WSS**：见下方「nginx 反代配置」。上线后前端 WebSocket 必须走 `wss`（见「前端改动」）。
5. **定期备份**：见 10.4。

**当前已知安全项状态**（生产评估用）：
- CSRF：当前全局关闭（前后端分离 + 自定义登录过滤器，重开需前端配合 XSRF-TOKEN，暂保持；后续如要启用再单独评估）。
- 文件下载 `/common/download`、`/common/downChat`：文件名走 UUID 不可枚举，且已做 `..` / `/` / `\` 路径遍历防护，风险低。
- 越权：业务接口除登录/公开展示类外均要求认证，敏感接口已加 `@Secured` 角色约束（详见 CONTEXT.md 安全基线）。

#### nginx 反代配置（HTTPS + WSS）
前端 `npm run build` 产物 `dist/` 由 nginx 托管，后端 8085 与 WebSocket 经 nginx 反代。证书用 Let's Encrypt：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d pet.你的诊所域名.com
```

`/etc/nginx/sites-available/mixpet`：
```nginx
server {
    listen 443 ssl;
    server_name pet.你的诊所域名.com;

    ssl_certificate     /etc/letsencrypt/live/pet.你的诊所域名.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pet.你的诊所域名.com/privkey.pem;

    # 前端静态资源（npm run build 产物）
    location / {
        root /home/debian/mixpet/mix-ui/dist;
        try_files $uri $uri/ /index.html;
    }
    # 后端 API（与 vite 代理一致的 /api 前缀）
    location /api/ {
        proxy_pass http://127.0.0.1:8085/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    # 在线咨询 WebSocket：走 wss，反代到后端 8085 的 /chat
    location /chat {
        proxy_pass http://127.0.0.1:8085/chat;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 3600s;
    }
}
# HTTP 80 全跳 443
server { listen 80; server_name pet.你的诊所域名.com; return 301 https://$host$request_uri; }
```

**前端已改为同源 `/chat`（已落地）**：`chat.vue` / `d-chat.vue` 的 `wsUrl` 已去掉 `:8085`，改连同源 `location.host + "/chat"`；`vite.config.ts` 已加 `/chat` 的 `ws` 代理。生产由下方 nginx 的 `location /chat` 反代到后端 8085 即可，HTTPS 页不再有混合内容拦截。

#### 清理测试数据（上线前）
演示数据集中在业务表，正式接诊前清空（字典/配置类表按需保留）：
```sql
-- 先备份（见 10.4）再执行；TRUNCATE 不可回滚
TRUNCATE TABLE msg, notice, follow_up, surgery_consent, surgery, record_package, prescription,
  medical_record, appointment, immune_record, hospital_monitor, hospital_record,
  image_order, lab_order_item, lab_order, adopt, pet, client, foster, doctor, department;
-- 药品/库存/供应商/会员等如需重置：
-- TRUNCATE TABLE drug_record, supplier, client_count_card, count_card_template, member_level, sys_dict, job, doctor_department;
```

#### 关闭公网暴露
数据库默认 `bind-address = 127.0.0.1`（仅本机），切勿改为 `0.0.0.0`；防火墙只放行 443（与 22）：
```bash
sudo ufw allow 443/tcp && sudo ufw allow 22/tcp && sudo ufw enable
```

### 10.3 部署到云端
- **前端**：`npm run build` 后上传 `dist/` 到 Cloudflare Pages
- **后端**：云服务器（推荐 2C4G），或先用 Oracle Cloud 免费 ARM
- **数据库**：可与后端同机部署

### 10.4 备份数据库
```bash
mysqldump -u mixpet -p pet_mix > ~/backup/pet_mix_$(date +%Y%m%d).sql

# crontab 每天凌晨 2 点
0 2 * * * mysqldump -u mixpet -p'你的密码' pet_mix > ~/backup/pet_mix_$(date +\%Y\%m\%d).sql
```

---

## 十一、版本信息

| 组件 | 版本 |
| :--- | :--- |
| 操作系统 | Debian 13 (Trixie) |
| JDK | Temurin 17.0.20.1 |
| Maven | 3.9.9 |
| Node.js | v20.20.2 |
| npm | 10.8.2 |
| MariaDB | 11.8.6 |
| Spring Boot | 3.1.0 |
| Vue | 3.2.47 |
| Vite | 4.3.9 |

---

**项目来源**：https://github.com/MixJade/MixPet
**当前仓库**：https://github.com/acuherb/mixpet
**部署状态**：本地局域网部署成功，前后端已跑通