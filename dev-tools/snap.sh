#!/bin/bash
cd "$(dirname "$0")/.." || exit 1

TAG="snap-$(date +%Y%m%d-%H%M%S)"

# 暂存所有修改，包括未跟踪文件
git add -A

# 创建一个临时 stash commit，但不改变工作区
STASH=$(git stash create "snap: $TAG")

if [ -n "$STASH" ]; then
  git tag "$TAG" "$STASH"
else
  # 没有任何修改，直接给 HEAD 打标签
  git tag "$TAG"
fi

# 取消暂存，工作区保持不变
git reset >/dev/null 2>&1

echo "✅ 快照：$TAG"