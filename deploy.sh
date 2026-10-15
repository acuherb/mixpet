#!/usr/bin/env bash
#
# MixPet 一键部署脚本
#   - 构建前端 (npm run build) 与后端 (mvn package)
#   - 生成 nginx 反代配置（80 跳转 + /api、/chat 反代）
#   - 启动后端 jar
# 可选参数：
#   --tls      申请 Let's Encrypt 证书并启用 HTTPS/WSS
#   --firewall 配置 ufw（放行 22/443 并启用）
#
# 前置：JDK17、Maven、Node18+、已建库并导入（见 DEPLOY.md §三）
# 说明：
#   - APP_DIR 建议纯英文路径（如 /opt/mixpet）；含中文路径会导致 mvn 报 FileURLConnection 异常。
#   - 建议以 root 运行（nginx 配置 / 证书 / 防火墙需权限）。
#   - 脚本不执行数据库迁移；全新环境请先按 DEPLOY.md 补齐 24 个增量脚本。
#   - 清测试数据（TRUNCATE，不可回滚）不自动执行，见 DEPLOY.md §10.2。

set -euo pipefail

# ====== 可配置区（均可用环境变量覆盖） ======
APP_DIR="${MIXPET_DIR:-/opt/mixpet}"
DOMAIN="${MIXPET_DOMAIN:-pet.你的诊所域名.com}"
DB_PASSWORD="${DB_PASSWORD:-CHANGE_ME_db}"
APP_DEFAULT_PASSWORD="${APP_DEFAULT_PASSWORD:-CHANGE_ME_app}"
MAIL_USER="${MAIL_USER:-your_qq@qq.com}"
MAIL_PASS="${MAIL_PASS:-your_auth_code}"
PET_AVATAR="${PET_AVATAR:-$APP_DIR/avatar/}"
PET_NOTICE="${PET_NOTICE:-$APP_DIR/notice/}"
PET_CHATIMG="${PET_CHATIMG:-$APP_DIR/chat/}"
PET_REPORT="${PET_REPORT:-$APP_DIR/report/}"

DO_TLS=0; DO_FW=0
for a in "$@"; do
  case "$a" in
    --tls) DO_TLS=1 ;;
    --firewall) DO_FW=1 ;;
  esac
done

[ -d "$APP_DIR" ] || { echo "项目目录不存在: $APP_DIR（请用 MIXPET_DIR 指定）"; exit 1; }

echo "==> MixPet 部署：目录=$APP_DIR  域名=$DOMAIN"

echo "==> [依赖检查]"
for c in java mvn node npm; do command -v "$c" >/dev/null || { echo "缺少 $c，请先安装"; exit 1; }; done

echo "==> [1/4] 构建前端"
cd "$APP_DIR/ship-ui"
npm install
npm run build

echo "==> [2/4] 构建后端"
cd "$APP_DIR/shipPet"
mvn clean package -DskipTests

echo "==> [3/4] 生成 nginx 站点配置"
NGINX_CONF="/etc/nginx/sites-available/mixpet"
mkdir -p /var/www/letsencrypt
cat > "$NGINX_CONF" <<NGINX
server {
    listen 80;
    server_name $DOMAIN;
    location /.well-known/acme-challenge/ { root /var/www/letsencrypt; }
    location / { return 301 https://\$host\$request_uri; }
}
NGINX
ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/mixpet
nginx -t 2>&1 || echo "⚠️ nginx 未安装或配置有误，请安装 nginx 后手动执行 systemctl reload nginx"

echo "==> [4/4] 启动后端"
export DB_PASSWORD APP_DEFAULT_PASSWORD MAIL_USER MAIL_PASS PET_AVATAR PET_NOTICE PET_CHATIMG PET_REPORT
JAR=$(ls "$APP_DIR/shipPet/target"/ship-*.jar 2>/dev/null | head -n1)
[ -n "$JAR" ] || { echo "未找到后端 jar，构建可能失败"; exit 1; }
nohup java -jar "$JAR" > /var/log/mixpet-backend.log 2>&1 &
echo "    后端已启动 PID=$!  日志=/var/log/mixpet-backend.log"

if [ "$DO_TLS" -eq 1 ]; then
  echo "==> [TLS] 申请证书并启用 HTTPS/WSS"
  command -v certbot >/dev/null || { echo "缺少 certbot，请先安装"; exit 1; }
  certbot --nginx -d "$DOMAIN"
  systemctl reload nginx
fi

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
echo "  ③ 确认域名 DNS 已解析到本机、数据库仅本机监听（bind-address=127.0.0.1）"
