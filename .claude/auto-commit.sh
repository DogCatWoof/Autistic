#!/usr/bin/env bash
cd /home/paul/projects/Autistic || exit 0

# Nothing to commit
git status --porcelain 2>/dev/null | grep -q . || exit 0

# Determine primary changed file for the message
FILE=$(git diff --name-only HEAD 2>/dev/null | head -1)
[ -z "$FILE" ] && FILE=$(git ls-files --others --exclude-standard 2>/dev/null | head -1)
BASE=$(basename "${FILE:-files}")

# Count total changed files
TOTAL=$(git status --porcelain | wc -l | tr -d ' ')
[ "$TOTAL" -gt 1 ] && EXTRA=" (+$((TOTAL - 1)) more)" || EXTRA=""

git add -A && git commit -m "Update ${BASE}${EXTRA}"
