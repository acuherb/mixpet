#!/bin/bash
echo "===== 后端日志（最后 30 行）====="
tail -n 30 /tmp/pet-backend.log
echo ""
echo "===== 前端日志（最后 15 行）====="
tail -n 15 /tmp/pet-frontend.log
for f in /tmp/pet-backend.log /tmp/pet-frontend.log; do
  if [ -f "$f" ]; then
    echo "===== $f ====="
    tail -n 30 "$f"
  else
    echo "缺少日志：$f"
  fi
done