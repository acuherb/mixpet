#!/bin/bash
cd "$(dirname "$0")/.." || exit 1

TAG=$(git tag -l 'snap-*' | sort | tail -1)
if [ -z "$TAG" ]; then echo "❌ 无快照，请先运行 snap.sh"; exit 1; fi

echo "将回滚到快照：$TAG"
echo "⚠️ 注意：这会用快照内容【覆盖】当前已修改但未提交的文件！"
echo "   未跟踪的新文件不会受影响，也不会删除。"
read -r -p "确认回滚？(y/N) " ans
case "$ans" in
  y|Y)
    git checkout "$TAG" -- .
    echo "✅ 已回滚到 $TAG"
    git status --short
    ;;
  *)
    echo "已取消"
    ;;
esac