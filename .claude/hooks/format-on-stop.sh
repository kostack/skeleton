#!/usr/bin/env bash
# Stop hook: when Kotlin or Gradle Kotlin files changed, run Spotless so formatting never depends
# on the agent remembering. If formatting fails (usually a syntax error), hand the error back to
# the agent once; on the retry (stop_hook_active) let it stop to avoid a loop.
set -uo pipefail

input=$(cat)
project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$project_dir" || exit 0

changed=$( { git diff --name-only HEAD; git ls-files --others --exclude-standard; } 2>/dev/null | grep -E '\.kts?$' || true)
[[ -z "$changed" ]] && exit 0

if output=$(./gradlew spotlessApply -q 2>&1); then
  exit 0
fi

if [[ "$(jq -r '.stop_hook_active // false' <<<"$input")" == "true" ]]; then
  exit 0
fi

echo "./gradlew spotlessApply failed. Fix the problem before finishing:" >&2
echo "$output" | tail -30 >&2
exit 2
