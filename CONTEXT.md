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

## 已完成（截至 2026-10-10）

### 数据库
- pet 表：+pet_birthday_estimated、pet_color、pet_weight、pet_sterilization、pet_chip、pet_allergy、pet_history、pet_alive、pet_note
- client 表：+client_email、client_pwd_changed、client_tel2、client_birthday_estimated、client_address、client_id_card、client_level、client_points、client_balance、client_source、client_blacklist

### 后端
- 手机号即登录账号，唯一性校验
- 绑定/修改邮箱接口 PUT /client/email
- 修改密码接口 PUT /client/password
- 管理员重置密码接口 PUT /client/reset/{id}
- updateSelfById 只更新用户可改字段

### 前端
- 后台侧边栏可折叠
- 宠物/主人详情和表单分组显示
- 生日与年龄双向联动，估算加「约」
- DetailTable 窄屏适配
- 绑定邮箱弹窗宽度自适应

## 待办

### 待测试
- [ ] 邮件发送（需先配 QQ 邮箱授权码到 MAIL_USER/MAIL_PASS 环境变量）
- [ ] 手机端按钮错位修复效果
- [ ] 后台用户管理手机号唯一性校验
- [ ] 顾客改邮箱 / 改密码

### 待开发
- [ ] 首次登录强制改密（client_pwd_changed 字段已埋，逻辑未写）
- [ ] 忘记密码改成邮箱找回（现 findMail 逻辑需重构）
- [ ] 顾客自己添加宠物
- [ ] 顾客自己预约挂号
- [ ] 主页视觉优化（去"幼稚感"，参见 MyCard.vue / Index.vue）

### 上线前
- [ ] 删除测试数据
- [ ] 改默认密码 123456
- [ ] 配置 HTTPS 和域名

## 关键决策

1. 账号 = 手机号，不开放自主注册，前台录入客户时自动生成
2. 密码统一 123456，顾客可自己改；忘记密码三种方式（后台重置 / 手机号 / 邮箱），手机号找回因短信服务成本暂缓
3. 只存生日（真相源），年龄前端实时计算；反推的生日标记为估算
4. pet_source 字段暂未加，做「顾客添加宠物」时再加

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
