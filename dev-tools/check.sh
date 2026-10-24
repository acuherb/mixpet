#!/bin/bash
# 前后端编译检查
# 项目路径含中文（公共），JVM 在 LANG=C 下抛 InvalidPathException，必须设 UTF-8
export LANG=C.UTF-8 LC_ALL=C.UTF-8
cd "$(dirname "$0")/.." || exit 1
ROOT="$PWD"

echo "===== 后端编译 ====="
(cd "$ROOT/mixPet" && mvn clean compile -q >/tmp/check-backend.log 2>&1) \
  && echo "✅ 后端编译通过" \
  || { echo "❌ 后端编译失败"; tail -20 /tmp/check-backend.log; }

echo "===== 前端构建 ====="
(cd "$ROOT/mix-ui" && npm run build >/tmp/check-ui.log 2>&1) \
  && echo "✅ 前端构建通过" \
  || { echo "❌ 前端构建失败"; tail -20 /tmp/check-ui.log; }

cd "$ROOT" >/dev/null 2>&1 || true