#!/usr/bin/env bash
set -euo pipefail

if [ ! -f pom.xml ]; then
	exit 0
fi

if ! git diff --name-only -- . ':!target/**' | grep -Eq '^(pom\.xml|logging\.properties|docs/.*\.(md|yml|yaml)|src/(main|test|stub)/java/.*\.java)$'; then
	exit 0
fi

mvn -q spotless:apply
