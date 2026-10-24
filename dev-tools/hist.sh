#!/bin/bash
cd "$(dirname "$0")/.." || exit 1
git tag -l 'snap-*' | sort
