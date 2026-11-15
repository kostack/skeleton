#!/usr/bin/env bash
# PreToolUse hook (Edit|Write|MultiEdit): refuse changes to Flyway migrations that are already
# committed, because Flyway checksums applied migrations. New, uncommitted migrations may be edited.
set -euo pipefail

file_path=$(jq -r '.tool_input.file_path // empty')
[[ -z "$file_path" ]] && exit 0

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
relative="${file_path#"$project_dir"/}"

case "$relative" in
  src/main/resources/db/migration/*) ;;
  *) exit 0 ;;
esac

if git -C "$project_dir" ls-files --error-unmatch -- "$relative" >/dev/null 2>&1; then
  jq -n --arg path "$relative" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: ("\($path) is a committed Flyway migration and must not be changed. Add a new V<n+1>__<description>.sql migration instead.")
    }
  }'
fi
exit 0
