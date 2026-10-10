# MixPet 宠物医院管理系统 - 本地部署完整教程

> 环境：Debian 13 (Trixie) / 局域网部署 / 2026-10 实测通过

---

## 一、环境准备

### 1.1 系统要求
- Debian 13 (Trixie) 或兼容的 Linux 发行版
- 建议 4GB 以上内存
- 局域网内可访问

### 1.2 安装 JDK 17

Debian 13 的默认源**没有** `openjdk-17-jdk`，必须通过 Adoptium 源安装：

```bash
# 安装依赖
sudo apt install -y wget apt-transport-https gnupg

# 导入 Adoptium GPG 密钥
sudo wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg

# 添加 Adoptium 源（自动识别 Debian 版本）
echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

# 更新并安装
sudo apt update
sudo apt install -y temurin-17-jdk
```

### 1.3 设置 JDK 17 为默认

```bash
# 查看并选择 JDK 17
sudo update-alternatives --config java
# 按提示选 temurin-17-jdk-amd64 对应编号

# 永久设置 JAVA_HOME，追加到 ~/.bashrc
echo 'export JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# 验证
java -version   # 应显示 17.0.x
mvn -version    # Java version 应为 17.0.x
```

### 1.4 安装其他依赖

```bash
# Maven（一般已自带，如没有则装）
sudo apt install -y maven

# Node.js 20（如果版本低于 18 需要升级）
node -v   # 应 >= v18
npm -v

# MariaDB（Debian 13 默认源里是 MariaDB 11）
sudo apt install -y mariadb-server
sudo systemctl enable --now mariadb
```

---

## 二、获取项目代码

```bash
cd ~/公共
git clone https://github.com/MixJade/MixPet.git
cd MixPet
```

**目录结构说明**：
- `shipPet/` — 后端（重置版，要用的就是这个）
- `ship-ui/` — 前端 Web
- `forgePet/` — 旧版后端（不用）
- `forgePetWX/` — 旧版微信小程序（不用）
- `数据库备份/pet_ship.sql` — 重置版数据库脚本
- `重置版启动说明.md` — 作者原版说明

---

## 三、数据库配置

### 3.1 创建数据库和专用账号

```bash
sudo mysql <<'EOF'
CREATE DATABASE pet_ship CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE USER 'mixpet'@'localhost' IDENTIFIED BY 'MixPet2024';
GRANT ALL PRIVILEGES ON pet_ship.* TO 'mixpet'@'localhost';
FLUSH PRIVILEGES;
EOF
```

> 如果 `utf8_general_ci` 报错，改用 `utf8mb3` 和 `utf8mb3_general_ci`。

### 3.2 导入数据

```bash
mysql -u mixpet -pMixPet2024 pet_ship < ~/公共/MixPet/数据库备份/pet_ship.sql
```

### 3.3 验证

```bash
mysql -u mixpet -pMixPet2024 pet_ship -e "SHOW TABLES;"
```

应看到 9 张表：`adopt`、`appointment`、`client`、`department`、`doctor`、`foster`、`msg`、`notice`、`pet`。

---

## 四、后端配置与启动

### 4.1 修改 application.yml

```bash
nano ~/公共/MixPet/shipPet/src/main/resources/application.yml
```

修改数据源部分：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/pet_ship?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: mixpet
    password: MixPet2024
```

### 4.2 修改 application-dev.yml

```bash
nano ~/公共/MixPet/shipPet/src/main/resources/application-dev.yml
```

修改资源路径（Linux 路径，**结尾斜杠不能少**）：

```yaml
pet-ship:
  images-path: /home/debian/公共/MixPet/images/
  notice-path: /home/debian/公共/MixPet/notice/
  chatImg-path: /home/debian/公共/MixPet/chatImg/
```

邮件配置（可选，如果要用邮箱验证码注册）：

```yaml
spring:
  mail:
    username: 你的QQ号@qq.com      # 你自己的QQ邮箱
    password: 你的16位授权码        # QQ邮箱设置里开启SMTP后获取
```

### 4.3 启动后端

```bash
cd ~/公共/MixPet/shipPet
mvn clean spring-boot:run
```

**成功标志**：
```
Tomcat started on port(s): 8085 (http)
Started ShipPetApplication in x.xx seconds
宠物医院管理系统2.0启动
http://localhost:8085
```

> ⚠️ **这个终端不要关**，后端在前台运行。

---

## 五、前端配置与启动

### 5.1 换 npm 镜像

```bash
npm config set registry https://registry.npmmirror.com
```

### 5.2 安装依赖

**新开一个终端**（后端终端保持运行）：

```bash
cd ~/公共/MixPet/ship-ui
npm install
```

### 5.3 启动前端

```bash
npm run dev
```

**成功标志**：
```
VITE v4.3.9  ready in xxx ms
➜  Local:   http://localhost:5932/
```

### 5.4 局域网访问

默认只能本机访问。要让局域网其他电脑访问，改 `vite.config.ts`：

```bash
nano ~/公共/MixPet/ship-ui/vite.config.ts
```

在 `server` 块加 `host: '0.0.0.0'`：

```ts
server: {
    host: '0.0.0.0',
    port: 5932,
    proxy: {
        '/api': {
            target: "http://localhost:8085",
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, '')
        }
    }
}
```

重启前端后，用 `http://本机IP:5932` 访问。

查询本机 IP：
```bash
ip addr | grep "inet "
```

---

## 六、登录账号

| 角色 | 账号 | 密码 | 入口 |
| :--- | :--- | :--- | :--- |
| 顾客 | `ying` | `123456` | 直接登录 |
| 医生 | `yun` | `123456` | 点登录卡片上的**猫猫头** |
| 管理员 | `admin` | `123456` | 点登录卡片上的**猫猫头** |

其他账号可从后台「用户管理」「医生管理」中查看，密码都是 `123456`。

---

## 七、日常启停

### 启动
```bash
# 终端 1：后端
cd ~/公共/MixPet/shipPet && mvn spring-boot:run

# 终端 2：前端
cd ~/公共/MixPet/ship-ui && npm run dev
```

### 停止
在各终端按 `Ctrl+C`。

### 后台常驻运行（推荐）
后续可以做成 systemd 服务或 Docker，避免终端关闭后服务停止。

---

## 八、常见问题

### 8.1 `不支持发行版本 17`
Maven 用的 JDK 不是 17。检查：
```bash
mvn -version
```
如果不是 17，检查 `~/.bashrc` 里的 `JAVA_HOME` 设置。

### 8.2 数据库连接失败
- 检查 MariaDB 是否启动：`sudo systemctl status mariadb`
- 检查账号密码是否和 `application.yml` 一致
- 检查数据库是否创建：`SHOW DATABASES;`

### 8.3 邮件验证码发不出去
- 必须是 QQ 邮箱，且开启了 **POP3/SMTP 服务**
- 密码填的是**16 位授权码**，不是 QQ 密码
- 检查垃圾邮件箱

### 8.4 前端能打开但接口 404
- 检查后端是否在跑（`http://localhost:8085`）
- 检查 `vite.config.ts` 里代理地址是否为 `http://localhost:8085`

### 8.5 局域网其他电脑访问不了
- `vite.config.ts` 加了 `host: '0.0.0.0'`
- 检查 GK41 防火墙：`sudo ufw status`
- 检查两台机器是否在同一网段

---

## 九、部署架构图

```
局域网内电脑浏览器
        │
        ▼
   http://192.168.1.13:5932   (前端 Vite Dev Server)
        │
        │ /api 请求代理
        ▼
   http://localhost:8085      (后端 Spring Boot)
        │
        ▼
   MariaDB pet_ship           (本地数据库)
```

---

## 十、后续开发提示

### 10.1 重要路径
- 后端代码：`shipPet/src/main/java/com/ship/`
- 后端配置：`shipPet/src/main/resources/`
- 前端代码：`ship-ui/src/`
- 前端路由：`ship-ui/src/router/`
- 前端 API 封装：`ship-ui/src/api/`

### 10.2 安全提醒（正式上线前必改）
- [ ] 数据库密码改成强密码
- [ ] 默认账号密码 `123456` 全部改掉
- [ ] 邮件授权码改为自己的
- [ ] 关闭顾客自主注册（建议改为后台录入）
- [ ] 配置 HTTPS 和域名
- [ ] 定期备份数据库

### 10.3 部署到云端
- **前端**：`npm run build` 后上传 `dist/` 到 Cloudflare Pages
- **后端**：需云服务器（国内推荐阿里云/腾讯云 2C4G），或先用 Oracle Cloud 免费 ARM 机器
- **数据库**：可与后端同机部署

### 10.4 备份数据库
```bash
# 手动备份
mysqldump -u mixpet -pMixPet2024 pet_ship > ~/backup/pet_ship_$(date +%Y%m%d).sql

# 定时备份（crontab）
# 每天凌晨 2 点备份
0 2 * * * mysqldump -u mixpet -pMixPet2024 pet_ship > ~/backup/pet_ship_$(date +\%Y\%m\%d).sql
```

---

## 十一、版本信息记录

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

**文档整理时间**：2026-10-10
**项目来源**：https://github.com/MixJade/MixPet
**部署状态**：✅ 本地局域网部署成功，前后端已跑通
