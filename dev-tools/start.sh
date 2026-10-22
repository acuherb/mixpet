#!/bin/bash
# 一键启动前后端（后台运行，关终端不死）
set -e

PROJ="$HOME/公共/mixpet"
BK_LOG="/tmp/pet-backend.log"   # 后端统一日志
FT_LOG="/tmp/pet-frontend.log"  # 前端统一日志

# 加载环境变量（优先用已有的 /home/debian/.env，或项目根目录的 .env.example）
for f in "$HOME/.env" "$PROJ/.env" "$PROJ/.env.example"; do
  [ -f "$f" ] && echo "加载环境变量: $f" && export $(grep -v '^#' "$f" | xargs) 2>/dev/null; break
done

# 中文路径保命：项目在「公共」目录下，mvn/JVM 默认 locale 非 UTF-8 时
# sun.jnu.encoding 会把中文路径解析成 ?????? 直接 InvalidPathException 挂掉
export LANG=C.UTF-8 LC_ALL=C.UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# 先停干净（按端口，避免误杀与占用卡死）
"$PROJ/dev-tools/stop.sh" || true
sleep 1

# 后端（限堆：本机内存紧张，mvn+vite 并发曾把 mvn 的 JVM 被 OOM Killer 杀掉——
#      表现是日志无 ERROR 直接断在 test-compile。MAVEN_OPTS 限 mvn 自身，
#      jvmArguments 限 spring-boot:run fork 出来的应用 JVM）
# 注意：必须先 clean（新增依赖 / 代码改动后不清缓存会编译失败）
cd "$PROJ/mixPet"
mvn clean
MAVEN_OPTS="-Xmx400m" setsid mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xmx512m" > "$BK_LOG" 2>&1 < /dev/null &
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