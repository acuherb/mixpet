#!/bin/bash
# 一键启动前后端（后台运行，关终端不死）
set -e

PROJ="$HOME/公共/mixpet"
BK_LOG="/tmp/pet-backend.log"   # 后端统一日志
FT_LOG="/tmp/pet-frontend.log"  # 前端统一日志

# 加载环境变量：只认真实 .env，绝不回退到仓库内公开的 .env.example（占位密钥不能进生产）
for f in "$HOME/.env" "$PROJ/.env"; do
  if [ -f "$f" ]; then
    echo "加载环境变量: $f"
    set -a
    # shellcheck disable=SC1090
    source "$f"
    set +a
    break
  fi
done
for required in DB_USER DB_PASSWORD APP_DEFAULT_PASSWORD REMEMBER_ME_KEY VERIFY_CODE_SECRET; do
  if [ -z "${!required:-}" ]; then
    echo "缺少必需环境变量 $required：请复制 .env.example 为 .env 并填入真实值" >&2
    exit 1
  fi
done
if [ "${#REMEMBER_ME_KEY}" -lt 32 ] || [ "${#VERIFY_CODE_SECRET}" -lt 32 ]; then
  echo "REMEMBER_ME_KEY / VERIFY_CODE_SECRET 必须至少 32 字符（.env 当前值过短）" >&2
  exit 1
fi

# 数据库 schema 自检（fail-fast，不自动改库）：
# mapper 已引用 P3/P4 临床列，旧库没跑迁移时运行时抛 Unknown column，
# 被 ExceptionAdvice 兜底成「操作未保存」，表现为诊疗流/收银/化验全线加载不出数据。
# 缺列直接拦住启动，避免「服务起来了但每个页面都报错」这种最难排查的状态。
DB_CHECK="unavailable"
if command -v mysql >/dev/null 2>&1 && [ -n "${DB_USER:-}" ]; then
  COLS=$(MYSQL_PWD="${DB_PASSWORD:-}" mysql -u "$DB_USER" -N pet_mix -e \
    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_mix' AND TABLE_NAME='medical_record' AND COLUMN_NAME IN ('version','differential_diagnosis','review_status','diagnosis_code')" 2>/dev/null) || COLS=""
  case "$COLS" in
    4) DB_CHECK="ok" ;;
    0|1|2|3) DB_CHECK="missing" ;;
  esac
fi
case "$DB_CHECK" in
  missing)
    echo "❌ 数据库缺少 P3/P4 临床迁移列，启动后诊疗流/收银/化验接口都会失败。" >&2
    echo "   请先执行：bash dev-tools/db-migrate.sh （幂等，只加列/建表）" >&2
    exit 1 ;;
  unavailable)
    echo "⚠️ 无法连库做 schema 自检（缺 mysql 客户端或数据库不可达），继续启动但不保证接口可用；" >&2
    echo "   若页面报「操作未保存」或 Unknown column，请执行：bash dev-tools/db-migrate.sh" >&2 ;;
esac

# 中文路径保命：项目在「公共」目录下，mvn/JVM 默认 locale 非 UTF-8 时
# sun.jnu.encoding 会把中文路径解析成 ?????? 直接 InvalidPathException 挂掉
export LANG=C.UTF-8 LC_ALL=C.UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# JDK 必须锁定「完整的 17」：Debian 13 默认 java 常指向 JDK21 的 JRE（无 javac），
# Maven in-process 编译会直接报 "release version 17 not supported"。
# 桌面双击 / 非登录 shell 启动时 ~/.bashrc 的 JAVA_HOME 不一定生效，所以在这里兜底。
JAVA17=""
for d in "${JAVA_HOME:-}" /usr/lib/jvm/temurin-17-jdk-amd64 /usr/lib/jvm/java-17-openjdk-amd64; do
  if [ -n "$d" ] && [ -x "$d/bin/javac" ] && "$d/bin/javac" -version 2>&1 | grep -q 'javac 17'; then
    JAVA17="$d"
    break
  fi
done
if [ -z "$JAVA17" ]; then
  echo "❌ 未找到完整 JDK 17（需含 javac）：sudo apt install temurin-17-jdk，或设置 JAVA_HOME" >&2
  exit 1
fi
export JAVA_HOME="$JAVA17"
export PATH="$JAVA_HOME/bin:$PATH"
echo "JDK: $JAVA_HOME ($("$JAVA_HOME/bin/javac" -version 2>&1 | head -n 1))"

# 先停干净（按端口，避免误杀与占用卡死）
"$PROJ/dev-tools/stop.sh" || true
sleep 1

# 后端（限堆：本机内存紧张，mvn+vite 并发曾把 mvn 的 JVM 被 OOM Killer 杀掉——
#      表现是日志无 ERROR 直接断在 test-compile。MAVEN_OPTS 限 mvn 自身，
#      jvmArguments 限 spring-boot:run fork 出来的应用 JVM）
# 注意：必须先 clean（新增依赖 / 代码改动后不清缓存会编译失败）
# 两个启动期开关（踩坑记录）：
#   1) -Dmaven.test.skip=true —— 生产启动不编译/跑测试。spring-boot:run 会触发 test-compile，
#      偶发因中文路径+增量缓存导致 test 类编译失败、连带阻塞启动；启动脚本本就不需要测试。
#   2) -Dspring.devtools.restart.enabled=false —— 关掉 devtools 热重启的 restart 类加载器。
#      它在 fork 子 JVM 时用独立 URLClassLoader 加载中文路径(公共/)下的类会失败，
#      表现为「NoClassDefFoundError: com/mix/common/Result」等诡异运行时报错。
cd "$PROJ/mixPet"
mvn clean
MAVEN_OPTS="-Xmx400m" setsid mvn spring-boot:run \
  -Dmaven.test.skip=true \
  -Dspring-boot.run.jvmArguments="-Xmx512m -Dspring.devtools.restart.enabled=false" > "$BK_LOG" 2>&1 < /dev/null &
BK_PID=$!
echo "后端 PID: $BK_PID  日志: $BK_LOG"

# 等后端就绪（找启动成功标志；类名已随 ship→mix 替换为 MixPetApplication）
echo ""
echo "等待服务起来..."
BK_OK=no
for i in $(seq 1 90); do
  if grep -q "Started MixPetApplication" "$BK_LOG" 2>/dev/null; then BK_OK=yes; echo "✅ 后端就绪 ($i 秒)"; break; fi
  # 进程不在了说明启动失败（如被 OOM 杀），提前退出不用傻等
  kill -0 "$BK_PID" 2>/dev/null || { echo "❌ 后端进程已退出（可能 OOM 或启动报错），请看日志"; break; }
  sleep 1
done
[ "$BK_OK" = yes ] || echo "⚠️ 后端未就绪，请看日志"

# 后端就绪后再起前端（错开内存/CPU 高峰：mvn 编译+Spring 启动是最重阶段）
cd "$PROJ/mix-ui"
setsid npm run dev > "$FT_LOG" 2>&1 < /dev/null &
FT_PID=$!
echo "前端 PID: $FT_PID  日志: $FT_LOG"

FT_OK=no
for i in $(seq 1 30); do
  if curl -s -o /dev/null --max-time 1 http://localhost:3120; then FT_OK=yes; echo "✅ 前端就绪 ($i 秒)"; break; fi
  sleep 1
done
[ "$FT_OK" = yes ] || echo "⚠️ 前端 30 秒未就绪，请看日志"

echo ""
echo "===== 状态 ====="
[ "$BK_OK" = yes ] && echo "后端 8085: ✅ 已启动" || echo "后端 8085: ❌ 未就绪"
[ "$FT_OK" = yes ] && echo "前端 3120: ✅ 已启动" || echo "前端 3120: ❌ 未就绪"
echo ""
echo "浏览器打开：http://localhost:3120"
echo "看日志：tail -f $BK_LOG / $FT_LOG"
echo "停服务：$PROJ/dev-tools/stop.sh"