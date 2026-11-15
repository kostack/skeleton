#!/bin/bash
set -euo pipefail

# exec replaces this shell with the JVM, so Java runs as PID 1 and receives SIGTERM directly
# (docker stop, Kubernetes rollouts) and can run its graceful shutdown.
exec java -jar /var/www/application/app.jar "$@"
