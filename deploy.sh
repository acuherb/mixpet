#!/usr/bin/env bash
#
# MixPet 一键生产部署脚本
#   - 构建前端 (npm run build) 与后端 (mvn package)
#   - 生成完整 nginx 站点：80 ACME/跳转 + 443 静态资源、/api 反代、/chat WSS、
#     安全响应头、API/登录限流
#   - 生成 systemd unit + logrotate，以守护进程方式运行后端（开机自启 / 崩溃重启）
#   - 把密钥收口到 /etc/mixpet/mixpet.env（0600/0640，仅 root 与服务用户可读）
#
# 可选参数：
#   --tls        用 certbot webroot 申请 Let's Encrypt 证书并启用 HTTPS/WSS
#   --firewall   配置 ufw（放行 22/443 并启用）
#   --no-systemd 容器/无 systemd 环境：回退 nohup 启动（不生成 unit）
#
# 前置：JDK17、Maven、Node18+、nginx、已建库并导入（见 DEPLOY.md §三）
# 说明：
#   - APP_DIR 建议纯英文路径（如 /opt/mixpet）；含中文路径会导致 mvn 报 FileURLConnection 异常。
#   - 必须以 root 运行（写 /etc/nginx、/etc/systemd/system、/etc/mixpet 需要权限）。
#   - 脚本不执行数据库迁移；全新环境导入 db-backup/pet_mix.sql（已含 P0/P1 DDL），
#     旧库升级依次执行：
#       db-backup/feature_20261107_p0_payment_audit.sql
#       db-backup/feature_20261107_p1_appointment_slot.sql
#       db-backup/feature_20261107_p3_clinical.sql
#       db-backup/feature_20261107_p4_clinical.sql
#   - 也可一键幂等补齐：bash dev-tools/db-migrate.sh（本地/旧库升级用）
#   - 清测试数据（TRUNCATE，不可回滚）不自动执行，见 DEPLOY.md §10.2。

set -euo pipefail

# ====== 可配置区（均可用环境变量覆盖） ======
APP_DIR="${MIXPET_DIR:-/opt/mixpet}"
DOMAIN="${MIXPET_DOMAIN:-pet.你的诊所域名.com}"
DIST_DIR="${MIXPET_DIST:-$APP_DIR/mix-ui/dist}"
BACKEND_PORT="${SERVER_PORT:-8085}"
ENV_FILE="${MIXPET_ENV_FILE:-/etc/mixpet/mixpet.env}"
SERVICE_USER="${MIXPET_USER:-mixpet}"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
NGINX_SITE="/etc/nginx/sites-available/mixpet"
NGINX_SSL_SITE="/etc/nginx/sites-available/mixpet-ssl"
NGINX_LIMITS="/etc/nginx/conf.d/mixpet-limits.conf"
SYSTEMD_UNIT="/etc/systemd/system/mixpet.service"
LOGROTATE_CONF="/etc/logrotate.d/mixpet"
LOG_DIR="/var/log/mixpet"
FIXED_JAR="$APP_DIR/mixpet-backend.jar"

DO_TLS=0; DO_FW=0; NO_SYSTEMD=0
for a in "$@"; do
  case "$a" in
    --tls) DO_TLS=1 ;;
    --firewall) DO_FW=1 ;;
    --no-systemd) NO_SYSTEMD=1 ;;
  esac
done

if [ "$(id -u)" -ne 0 ]; then
  echo "请以 root 运行：sudo -E bash deploy.sh（需要写 nginx/systemd 配置；-E 保留环境变量）" >&2
  exit 1
fi

# 二次部署：sudo 默认清空环境变量，若本机已有上次生成的 EnvironmentFile 则直接复用，
# 免得把密钥再贴一遍命令行；调用者显式 export 的值优先（以 DB_PASSWORD 是否已有为准）
if [ -f "$ENV_FILE" ] && [ -z "${DB_PASSWORD:-}" ]; then
  echo "复用已有环境文件: $ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

# 安全相关变量一律不做默认值兜底：缺失或仍是公开模板值直接拒绝部署
for required in DB_USER DB_PASSWORD APP_DEFAULT_PASSWORD REMEMBER_ME_KEY VERIFY_CODE_SECRET; do
  if [ -z "${!required:-}" ]; then
    echo "缺少必需环境变量 $required，请先按 DEPLOY.md 配置后重试" >&2
    exit 1
  fi
done
for secret in REMEMBER_ME_KEY VERIFY_CODE_SECRET; do
  case "${!secret}" in
    *YourSecure*|*CHANGE_ME*|MixPet@Remember2026)
      echo "$secret 仍是公开模板值，请换成强随机密钥" >&2
      exit 1 ;;
  esac
done
if [ "${#REMEMBER_ME_KEY}" -lt 32 ] || [ "${#VERIFY_CODE_SECRET}" -lt 32 ]; then
  echo "REMEMBER_ME_KEY / VERIFY_CODE_SECRET 必须至少 32 字符" >&2
  exit 1
fi
if [ "$APP_DEFAULT_PASSWORD" = "YourSecureDefaultPassword123!" ] || [ "$APP_DEFAULT_PASSWORD" = "123456" ]; then
  echo "APP_DEFAULT_PASSWORD 仍是演示口令，请换成强口令" >&2
  exit 1
fi
MAIL_USER="${MAIL_USER:-}"
MAIL_PASS="${MAIL_PASS:-}"
PET_AVATAR="${PET_AVATAR:-$APP_DIR/avatar/}"
PET_NOTICE="${PET_NOTICE:-$APP_DIR/notice/}"
PET_CHATIMG="${PET_CHATIMG:-$APP_DIR/chat/}"
PET_REPORT="${PET_REPORT:-$APP_DIR/report/}"
BACKUP_DIR="${BACKUP_DIR:-$APP_DIR/db-backup/runtime/}"
DB_URL="${DB_URL:-jdbc:mysql://localhost:3306/pet_mix?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8}"
COOKIE_SECURE="${COOKIE_SECURE:-true}"
AI_API_KEY="${AI_API_KEY:-}"

[ -d "$APP_DIR" ] || { echo "项目目录不存在: $APP_DIR（请用 MIXPET_DIR 指定）"; exit 1; }
case "$DOMAIN" in
  *你的诊所域名*|*example.com*) echo "MIXPET_DOMAIN 仍是占位域名，请设置真实域名" >&2; exit 1 ;;
esac

echo "==> MixPet 部署：目录=$APP_DIR  域名=$DOMAIN  后端端口=$BACKEND_PORT"
if [ "$NO_SYSTEMD" -eq 0 ] && [ ! -d /run/systemd/system ]; then
  echo "⚠️ 未检测到运行中的 systemd，回退 nohup 启动（可用 --no-systemd 显式指定）"
  NO_SYSTEMD=1
fi

echo "==> [依赖检查]"
for c in java mvn node npm; do command -v "$c" >/dev/null || { echo "缺少 $c，请先安装"; exit 1; }; done
# 编译与运行统一用完整 JDK 17：Debian 13 默认 java 可能是 JDK21 的 JRE（无 javac），
# Maven 会报 "release version 17 not supported"；中文项目路径还需要 UTF-8 locale。
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; then
  for d in /usr/lib/jvm/temurin-17-jdk-amd64 /usr/lib/jvm/java-17-openjdk-amd64; do
    if [ -x "$d/bin/javac" ] && "$d/bin/javac" -version 2>&1 | grep -q 'javac 17'; then
      export JAVA_HOME="$d"
      break
    fi
  done
fi
if [ ! -x "${JAVA_HOME:-}/bin/javac" ]; then
  echo "缺少完整 JDK 17（需含 javac）：请安装 temurin-17-jdk 或设置 JAVA_HOME" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
export LANG=C.UTF-8 LC_ALL=C.UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
JAVA_BIN="${JAVA_BIN:-$JAVA_HOME/bin/java}"
echo "    JDK: $JAVA_HOME"

echo "==> [1/6] 构建前端"
cd "$APP_DIR/mix-ui"
npm install
npm run build
[ -d "$DIST_DIR" ] || { echo "前端产物目录不存在: $DIST_DIR"; exit 1; }

echo "==> [2/6] 构建后端"
cd "$APP_DIR/mixPet"
mvn clean package -DskipTests
shopt -s nullglob
JARS=("$APP_DIR/mixPet/target"/mix-*.jar)
[ "${#JARS[@]}" -gt 0 ] || { echo "未找到后端 jar，构建可能失败"; exit 1; }
# 固定路径：unit 的 ExecStart 不随版本号变化，重启/发版都不用改 unit
install -m 0644 "${JARS[0]}" "$FIXED_JAR"
echo "    jar -> $FIXED_JAR"

echo "==> [3/6] 生成服务环境变量与运行目录"
install -d -m 0750 "$(dirname "$ENV_FILE")"
if ! id -u "$SERVICE_USER" >/dev/null 2>&1; then
  useradd --system --no-create-home --shell /usr/sbin/nologin "$SERVICE_USER"
fi
for d in "$PET_AVATAR" "$PET_NOTICE" "$PET_CHATIMG" "$PET_REPORT" "$BACKUP_DIR" "$LOG_DIR"; do
  mkdir -p "$d"
  chown -R "$SERVICE_USER":"$SERVICE_USER" "$d"
done
# EnvironmentFile 值用双引号包裹并转义，避免密码里的空格/#/引号破坏解析
env_line() {
  local k="$1" v="$2"
  if [[ "$v" == *$'\n'* ]]; then echo "环境变量 $k 含换行，拒绝写入 $ENV_FILE" >&2; exit 1; fi
  v="${v//\\/\\\\}"; v="${v//\"/\\\"}"
  printf '%s="%s"\n' "$k" "$v"
}
(
  umask 077
  {
    env_line SPRING_PROFILES_ACTIVE prod
    env_line SERVER_PORT "$BACKEND_PORT"
    env_line DB_URL "$DB_URL"
    env_line DB_USER "$DB_USER"
    env_line DB_PASSWORD "$DB_PASSWORD"
    env_line APP_DEFAULT_PASSWORD "$APP_DEFAULT_PASSWORD"
    env_line REMEMBER_ME_KEY "$REMEMBER_ME_KEY"
    env_line VERIFY_CODE_SECRET "$VERIFY_CODE_SECRET"
    env_line MAIL_USER "$MAIL_USER"
    env_line MAIL_PASS "$MAIL_PASS"
    env_line PET_AVATAR "$PET_AVATAR"
    env_line PET_NOTICE "$PET_NOTICE"
    env_line PET_CHATIMG "$PET_CHATIMG"
    env_line PET_REPORT "$PET_REPORT"
    env_line BACKUP_DIR "$BACKUP_DIR"
    env_line BACKUP_AUTO_ENABLED "${BACKUP_AUTO_ENABLED:-true}"
    env_line BACKUP_AUTO_KEEP "${BACKUP_AUTO_KEEP:-14}"
    env_line COOKIE_SECURE "$COOKIE_SECURE"
    env_line AI_API_KEY "$AI_API_KEY"
  } > "$ENV_FILE"
)
chmod 0640 "$ENV_FILE"
chown root:"$SERVICE_USER" "$ENV_FILE"
echo "    环境变量 -> $ENV_FILE (root:$SERVICE_USER 0640)"

echo "==> [4/6] 生成 nginx 站点配置（80 / 443 / 限流）"
install -d -m 0755 /var/www/letsencrypt "$(dirname "$NGINX_SITE")" "$(dirname "$NGINX_LIMITS")" /etc/nginx/sites-enabled
# limit_req_zone 只能放在 http 块：借 conf.d（nginx.conf 默认 include）
cat > "$NGINX_LIMITS" <<'LIMITS'
# MixPet：按客户端 IP 限制 API / 登录频率（后端 LoginAttemptLimiter 之外的第一道闸）
limit_req_zone $binary_remote_addr zone=mixpet_api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=mixpet_login:10m rate=1r/s;
LIMITS
if ! grep -q 'conf.d' /etc/nginx/nginx.conf 2>/dev/null; then
  echo "⚠️ /etc/nginx/nginx.conf 未 include conf.d，请手动在 http{} 内引入 mixpet-limits.conf"
fi

cat > "$NGINX_SITE" <<NGINX
server {
    listen 80;
    server_name $DOMAIN;
    server_tokens off;
    add_header X-Content-Type-Options nosniff always;

    # certbot --webroot 校验（--tls 时使用）
    location /.well-known/acme-challenge/ { root /var/www/letsencrypt; }
    location / { return 301 https://\$host\$request_uri; }
}
NGINX

cat > "$NGINX_SSL_SITE" <<NGINX
server {
    listen 443 ssl http2;
    server_name $DOMAIN;
    server_tokens off;

    ssl_certificate     $CERT_DIR/fullchain.pem;
    ssl_certificate_key $CERT_DIR/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_session_cache shared:MixPetSSL:10m;
    ssl_session_timeout 10m;

    # 安全响应头：不设 CSP —— 打印页使用 document.write 内联脚本、PDF 导出依赖 canvas，
    # 强 CSP 会直接破坏打印/导出；如需启用须先改造打印链路。
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options SAMEORIGIN always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

    client_max_body_size 31m;
    root $DIST_DIR;
    index index.html;

    # 反代通用头（子 location 未覆盖时继承）
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;

    location / {
        try_files \$uri \$uri/ /index.html;
    }
    location /assets/ {
        # 只写 expires（不加 add_header），避免覆盖 server 级安全响应头
        expires 30d;
        try_files \$uri =404;
    }
    # 后端 API：与 vite 代理一致，/api/xxx -> /xxx
    location /api/ {
        limit_req zone=mixpet_api burst=20 nodelay;
        proxy_pass http://127.0.0.1:$BACKEND_PORT/;
        proxy_connect_timeout 10s;
        proxy_read_timeout 120s;
    }
    # 登录/注册/找回密码/验证码：更严格的限流，防暴力破解与短信/邮件轰炸
    location = /api/cao/toLogin {
        limit_req zone=mixpet_login burst=5 nodelay;
        proxy_pass http://127.0.0.1:$BACKEND_PORT/cao/toLogin;
    }
    location ^~ /api/login/ {
        limit_req zone=mixpet_login burst=5 nodelay;
        rewrite ^/api/(.*)\$ /\$1 break;
        proxy_pass http://127.0.0.1:$BACKEND_PORT;
    }
    # 在线咨询 WebSocket：HTTPS 页面必须走 wss，Upgrade 头不能漏
    location /chat {
        proxy_pass http://127.0.0.1:$BACKEND_PORT/chat;
        proxy_http_version 1.1;
        # 本 location 一旦出现 proxy_set_header 就不再继承 server 级，需重复通用头
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }
}
NGINX
ln -sf "$NGINX_SITE" /etc/nginx/sites-enabled/mixpet

if [ "$DO_TLS" -eq 1 ]; then
  echo "==> [TLS] 申请证书（certbot webroot，不改写脚本生成的站点配置）"
  command -v certbot >/dev/null || { echo "缺少 certbot，请先安装 python3-certbot-nginx 或 certbot"; exit 1; }
  CERTBOT_ARGS=(certonly --webroot -w /var/www/letsencrypt -d "$DOMAIN" --agree-tos -n)
  if [ -n "${MIXPET_ADMIN_MAIL:-}" ]; then CERTBOT_ARGS+=(-m "$MIXPET_ADMIN_MAIL")
  else CERTBOT_ARGS+=(--register-unsafely-without-email); fi
  if command -v nginx >/dev/null; then nginx -t && (systemctl reload nginx || nginx -s reload) || true; fi
  certbot "${CERTBOT_ARGS[@]}"
fi

SSL_ON=0
if [ -f "$CERT_DIR/fullchain.pem" ] && [ -f "$CERT_DIR/privkey.pem" ]; then
  ln -sf "$NGINX_SSL_SITE" /etc/nginx/sites-enabled/mixpet-ssl
  SSL_ON=1
else
  rm -f /etc/nginx/sites-enabled/mixpet-ssl
  echo "⚠️ 未找到 $CERT_DIR 证书：443 站点未启用，80 会跳转到尚不存在的 443，站点当前不可访问"
  echo "   请执行 'bash deploy.sh --tls' 申请证书，或手动放置证书后重跑本脚本"
  echo "   （prod profile 的 Cookie 为 Secure，正式提供服务必须 HTTPS）"
fi
if command -v nginx >/dev/null; then
  if nginx -t; then
    systemctl reload nginx || nginx -s reload
  else
    echo "⚠️ nginx 配置校验失败，请修正后手动 reload（后端服务不受影响）"
  fi
else
  echo "⚠️ nginx 未安装：配置已生成，安装后执行 nginx -t && systemctl reload nginx"
fi
[ "$SSL_ON" -eq 1 ] && echo "    HTTPS/WSS 站点已启用" || echo "    仅启用 80 跳转站点（证书就绪后会自动启用 443）"

echo "==> [5/6] 配置进程守护"
if [ "$NO_SYSTEMD" -eq 0 ]; then
  cat > "$SYSTEMD_UNIT" <<UNIT
[Unit]
Description=MixPet backend (Spring Boot)
After=network-online.target mariadb.service mysql.service
Wants=network-online.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$APP_DIR/mixPet
EnvironmentFile=$ENV_FILE
Environment=LANG=C.UTF-8
Environment=LC_ALL=C.UTF-8
Environment="JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
ExecStart=$JAVA_BIN -Xms256m -Xmx1024m -jar $FIXED_JAR
Restart=always
RestartSec=10
SuccessExitStatus=143
StandardOutput=append:$LOG_DIR/backend.log
StandardError=append:$LOG_DIR/backend.log
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full

[Install]
WantedBy=multi-user.target
UNIT
  cat > "$LOGROTATE_CONF" <<ROTATE
/var/log/mixpet/*.log {
    daily
    rotate 14
    missingok
    notifempty
    compress
    delaycompress
    copytruncate
    su $SERVICE_USER $SERVICE_USER
}
ROTATE
  systemctl daemon-reload
  systemctl enable mixpet.service
  systemctl restart mixpet.service
  echo "    服务：systemctl status mixpet / journalctl -u mixpet -f"
  echo "    日志：$LOG_DIR/backend.log（logrotate 保留 14 天）"
else
  export SPRING_PROFILES_ACTIVE=prod SERVER_PORT="$BACKEND_PORT" DB_URL \
         DB_USER DB_PASSWORD APP_DEFAULT_PASSWORD REMEMBER_ME_KEY VERIFY_CODE_SECRET \
         MAIL_USER MAIL_PASS PET_AVATAR PET_NOTICE PET_CHATIMG PET_REPORT BACKUP_DIR \
         COOKIE_SECURE AI_API_KEY LANG=C.UTF-8 LC_ALL=C.UTF-8
  nohup java -Xms256m -Xmx1024m -jar "$FIXED_JAR" >> "$LOG_DIR/backend.log" 2>&1 &
  echo "    后端已启动 PID=$!（无 systemd，不自动重启） 日志=$LOG_DIR/backend.log"
fi

echo "==> [6/6] 收尾"
if [ "$DO_FW" -eq 1 ]; then
  echo "==> [防火墙] 放行 22/443 并启用 ufw"
  ufw allow 22/tcp
  ufw allow 443/tcp
  ufw --force enable
fi

echo "==> 完成。"
echo "上线前必做："
echo "  ① 改默认口令：APP_DEFAULT_PASSWORD 已注入新建/重置账号；存量 admin/yun/ying 到后台「用户管理/医生管理」逐个重置"
echo "  ② 清测试数据：见 DEPLOY.md §10.2 的 TRUNCATE 脚本（先备份！不可回滚）"
echo "  ③ 旧库依次执行 P0/P1/P3/P4 四个 feature_20261107_* 迁移脚本（或 bash dev-tools/db-migrate.sh 一键幂等补齐）；新库直接导入 pet_mix.sql"
echo "  ④ 确认域名 DNS 已解析到本机、数据库仅本机监听（bind-address=127.0.0.1）"
echo "  ⑤ 验证：curl -I https://$DOMAIN 、浏览器登录并打开在线咨询（wss 握手）、systemctl status mixpet"
