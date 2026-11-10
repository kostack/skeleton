#!/usr/bin/env bash
# Starts the application as Spring Shell in the current terminal, with Tab completion, history and
# line editing. Gradle (bootRun) only connects the app to pipes, never to a terminal, so
# Spring Shell falls back to a dumb terminal there.
#
# Usage: scripts/shell.sh [application arguments]
# Always runs with only the "shell" profile; SPRING_PROFILES_ACTIVE from the caller or the env file is ignored.
set -euo pipefail

cd "$(dirname "$0")/.."

profile="shell"

# Same lookup as Gradle: docker/.env.<profile>, otherwise docker/.env
env_file="docker/.env.$profile"
[ -f "$env_file" ] || env_file="docker/.env"

./gradlew --quiet bootJar

if [ -f "$env_file" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" == \#* || "$line" != *=* ]] && continue
    export "${line%%=*}=${line#*=}"
  done < "$env_file"
fi

export SPRING_PROFILES_ACTIVE="$profile"

exec java --enable-native-access=ALL-UNNAMED -jar build/libs/app.jar "$@"
