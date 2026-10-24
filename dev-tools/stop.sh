#!/bin/bash
# 按端口精准停止前后端，避免误杀其他 java/vite 进程导致端口占用卡死
# 优先级：ss > fuser > lsof > pkill(兜底)

# 通过端口找 PID（第一个监听该端口的进程）
port_pid() {
  local port="$1" pid=""
  if command -v ss >/dev/null 2>&1; then
    pid=$(ss -ltnp 2>/dev/null | awk -v p=":$port" '$4 ~ p {if (match($0, /pid=([0-9]+)/, a)) {print a[1]; exit}}')
  fi
  if [ -z "$pid" ] && command -v fuser >/dev/null 2>&1; then
    pid=$(fuser "$port"/tcp 2>/dev/null | tr -d ' ')
  fi
  if [ -z "$pid" ] && command -v lsof >/dev/null 2>&1; then
    pid=$(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -1)
  fi
  echo "$pid"
}

kill_port() {
  local port="$1" name="$2" pid
  pid=$(port_pid "$port")
  if [ -n "$pid" ]; then
    kill "$pid" 2>/dev/null
    # 等进程退出（最多 5 秒）
    for _ in $(seq 1 5); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    kill -0 "$pid" 2>/dev/null && { echo "✅ $name($port) 未响应，强制终止"; kill -9 "$pid" 2>/dev/null; } || echo "✅ $name($port, PID $pid) 已停止"
  else
    echo "ℹ️ $name($port) 未在运行"
  fi
}

kill_port 8085 "后端"
kill_port 3120 "前端"

# 兜底：确认没有残留的 vite 进程（前端进程名统一为 vite）
pkill -f "vite" 2>/dev/null && echo "ℹ️ 已清理残留 vite 进程" || echo "ℹ️ 无残留 vite 进程"