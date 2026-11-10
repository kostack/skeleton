#!/usr/bin/env bash
# Starts the application as Spring Shell in the current terminal, with Tab completion, history and
# line editing. Gradle (runJar, bootRun) only connects the app to pipes, never to a terminal, so
# Spring Shell falls back to a dumb terminal there.
#
# Usage: scripts/shell.sh [application arguments]
# Profiles: SPRING_PROFILES_ACTIVE, default "shell".
set -euo pipefail

cd "$(dirname "$0")/.."

profiles="${SPRING_PROFILES_ACTIVE:-shell}"

# Same lookup as Gradle: docker/.env.<profile> for the first profile, otherwise docker/.env
env_file="docker/.env.${profiles%%,*}"
[ -f "$env_file" ] || env_file="docker/.env"

./gradlew --quiet bootJar

if [ -f "$env_file" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" == \#* || "$line" != *=* ]] && continue
    export "${line%%=*}=${line#*=}"
  done < "$env_file"
fi

export SPRING_PROFILES_ACTIVE="$profiles"

exec java --enable-native-access=ALL-UNNAMED -jar build/libs/app.jar "$@"
