#!/usr/bin/env bash
# Starts the application as Spring Shell in the current terminal, with Tab completion, history and
# line editing. Gradle (bootRun) only connects the app to pipes, never to a terminal, so
# Spring Shell falls back to a dumb terminal there.
#
# Usage: scripts/shell.sh [application arguments]
# Works in the project (builds the jar first) and in the Docker image, where it is copied next to app.jar:
#   docker exec -it <container> scripts/shell.sh
# Always runs with only the "shell" profile; SPRING_PROFILES_ACTIVE from the caller or the env file is ignored.
set -euo pipefail

cd "$(dirname "$0")/.."

profile="shell"

# Same lookup as Gradle: docker/.env.<profile>, otherwise docker/.env
env_file="docker/.env.$profile"
[ -f "$env_file" ] || env_file="docker/.env"

# In the project, build the jar first; in the Docker image, app.jar is already in place.
if [ -x ./gradlew ]; then
  ./gradlew --quiet bootJar
  jar="build/libs/app.jar"
else
  jar="app.jar"
fi

if [ -f "$env_file" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" == \#* || "$line" != *=* ]] && continue
    export "${line%%=*}=${line#*=}"
  done < "$env_file"
fi

export SPRING_PROFILES_ACTIVE="$profile"

exec java --enable-native-access=ALL-UNNAMED -jar "$jar" "$@"
