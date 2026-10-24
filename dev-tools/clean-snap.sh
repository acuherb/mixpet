#!/bin/bash
cd "$(dirname "$0")/.." || exit 1
COUNT=$(git tag -l 'snap-*' | wc -l)
git tag -l 'snap-*' | xargs -r git tag -d > /dev/null
echo "✅ 已清除 $COUNT 个快照"
