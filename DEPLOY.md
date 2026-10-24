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
- `mixPet/` — 后端（Spring Boot 3 + Java 17，启动类 `com.mix.MixPetApplication`）
- `mix-ui/` — 前端（Vue 3 + Vite）
- `db-backup/` — SQL 脚本（基线 `pet_mix.sql` + 51 个增量迁移脚本，索引见 `db-backup/INDEX.md`）
- `dev-tools/` — 开发辅助脚本（`start.sh` 一键启停 / `stop.sh` / `check.sh` 双端验证等，说明见 `dev-tools/WORKFLOW.md`）
- `avatar/` — 头像图片（WebP）
- `notice/` — 公告配图
- `chat/` — 聊天文件（其下分 `image/` 图片、`file/` 文件两子目录）
- `report/` — 报告附件（其下分 `image/` 与 `file/` 两子目录）
- `.env` — 本机环境变量（**不入库**，复制 `.env.example` 生成；`start.sh` 自动加载）
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

应看到全部业务表（约 40+ 张：adopt/appointment/client/department/doctor/foster/msg/notice/pet/medical_record/prescription/bill/drug/lab_*/image_*/hospital_*/immune_record/surgery*/package*/follow_up/member_*/sys_dict/job/doctor_department/supplier/drug_record/record_template 等，以实际 `SHOW TABLES` 为准）。

---

## 四、后端配置与启动

### 4.1 配置说明

后端配置采用**环境变量写法**，本地开发不改代码，密钥类变量通过 `.env` 文件加载。

`application.yml` 关键项（默认值为空=必须由环境变量提供）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pet_mix?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: ${DB_USER:}
    password: ${DB_PASSWORD:}
  mail:
    host: smtp.qq.com        # 587 端口已配 STARTTLS（QQ 邮箱必须）
    port: 587
app:
  default-password: ${APP_DEFAULT_PASSWORD:}   # 新建/重置账号默认口令
remember-me:
  key: ${REMEMBER_ME_KEY:}                     # 记住我 Cookie 签名密钥（≥32 字符）
appointment:
  max-per-day: 20                              # 每医生每日号源上限（非敏感，可直接改默认）
```

### 4.2 环境变量（.env）

复制模板生成本机配置（**`.env` 含密码/授权码/密钥，已被 .gitignore 拦截，不会入库**）：

```bash
cp .env.example .env
# 编辑 .env 填入真实值，至少：DB_USER / DB_PASSWORD / APP_DEFAULT_PASSWORD /
#   REMEMBER_ME_KEY（≥32字符）/ VERIFY_CODE_SECRET（≥32字符）
# 需要邮箱验证码再填：MAIL_USER（QQ邮箱）/ MAIL_PASS（16位SMTP授权码，非QQ密码）
```

| 变量 | 用途 |
|---|---|
| `DB_USER` / `DB_PASSWORD` | 数据库账号密码（必须） |
| `APP_DEFAULT_PASSWORD` | 新建/重置账号默认口令（必须） |
| `REMEMBER_ME_KEY` / `VERIFY_CODE_SECRET` | 签名密钥，≥32 字符（必须） |
| `MAIL_USER` / `MAIL_PASS` | 注册验证码/找回密码发信（QQ 邮箱 SMTP） |
| `PET_AVATAR` / `PET_NOTICE` / `PET_CHATIMG` | 头像/公告/聊天文件存储目录（默认即本机路径） |

> `start.sh` 会按 `$HOME/.env` → 项目 `.env` → `.env.example` 顺序自动加载；手动 `mvn spring-boot:run` 前先 `set -a; source .env; set +a`。

### 4.3 启动后端（推荐：一键脚本）

`dev-tools/start.sh` 自动完成：加载 `.env` → 中文路径 locale 保命（项目在「公共」目录，mvn 的 JVM 不设 UTF-8 会 `InvalidPathException` 挂掉）→ 限堆启动后端（400m/512m，内存紧张机器防 OOM）→ **后端就绪后再起前端**（错开编译高峰）→ 就绪检测（进程早退立即报）：

```bash
~/公共/mixpet/dev-tools/start.sh     # 后端 8085 + 前端 3120，日志在 /tmp/pet-*.log
~/公共/mixpet/dev-tools/stop.sh      # 停止
```

手动启动（等价最小步骤）：
```bash
cd ~/公共/mixpet
set -a; source .env; set +a          # 加载环境变量
export LANG=C.UTF-8 LC_ALL=C.UTF-8   # 中文路径保命
cd mixPet && mvn spring-boot:run
```

成功标志：
```
Tomcat started on port(s): 8085 (http)
Started MixPetApplication in x.xx seconds
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

> **初始登录流程**：系统初始只有管理员 `admin` 可登录；医生账号由管理员在后台创建；顾客账号有两种来源：① 管理员后台录入（默认口令取 `APP_DEFAULT_PASSWORD`，**首次登录强制改密**，前端 `/changePwd` 守卫）；② **顾客现场自助注册**（手机号即账号、密码自设、邮箱验证码校验），注册后为**待审核**状态——管理员在后台「客户管理」或医生工作台待办看到「待审核顾客」卡片，审核通过才能登录（拒绝需填原因，顾客登录时可见）。基线演示库预置顾客 `ying`、医生 `yun` 便于直接体验。

| 角色 | 登录标识 | 密码 | 入口 |
| :--- | :--- | :--- | :--- |
| 管理员 | `admin` | `123456` | 点登录卡片上的**猫猫头**（初始唯一入口） |
| 顾客 | 手机号（演示库预置 `ying`） | `123456` | 顾客端「手机号 + 密码」登录，**首次须改密** |
| 医生 | `yun` | `123456` | 点登录卡片上的**猫猫头**（由管理员创建） |

> 邮件验证码依赖 `.env` 的 `MAIL_USER`/`MAIL_PASS`（QQ 邮箱已开启 POP3/SMTP 服务、授权码非 QQ 密码）；未配置时注册/找回密码的验证码步骤不可用。

---

## 七、日常启停

### 启动（推荐一键）

```bash
~/公共/mixpet/dev-tools/start.sh   # 先后端（就绪后）再前端，关终端不死
```

成功输出：`后端 PID: xxxx` → `✅ 后端就绪 (xx 秒)` → `✅ 前端就绪`，浏览器开 http://localhost:3120。

### 手动启动（双终端）

```bash
# 终端 1：后端（先加载 .env + 中文路径 locale）
cd ~/公共/mixpet && set -a; source .env; set +a; export LANG=C.UTF-8 LC_ALL=C.UTF-8
cd mixPet && mvn spring-boot:run

# 终端 2：前端
cd ~/公共/mixpet/mix-ui && npm run dev
```

### 停止

```bash
~/公共/mixpet/dev-tools/stop.sh    # 一键停（按端口清理，无残留）
```

或各终端按 `Ctrl+C`。

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
- 检查垃圾邮件箱（发件人显示为医院名，主题「【医院名】邮箱验证码」）
- **仍失败看后端日志真因**：`tail -20 /tmp/pet-backend.log`——发送失败会打完整异常栈（STARTTLS 握手失败 / 认证失败 / 超时分别有不同栈），按栈排查：
  - `MailAuthenticationException` → 授权码错或 SMTP 服务没开；
  - 连接超时 → 网络/防火墙（587 出站）；
  - 前端提示与事实不符（如“发送失败”但收到邮件）→ 多为前端拦截器/ token 链路问题，看浏览器 Network 响应体。
- 已内置防护：STARTTLS（QQ 587 必需）、5 秒连接超时、JWT 密钥任意长度容错（短密钥不再 500）

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