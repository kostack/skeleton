#!/usr/bin/env bash
# Prepares the devcontainer after it is created.
set -euo pipefail

cd "$(dirname "$0")/.."

# The Gradle cache volumes are created as root; hand them to the container user.
sudo chown -R "$(id -u):$(id -g)" "$HOME/.gradle" .gradle

# Create docker/.env with fresh JWT keys if it does not exist yet.
if [ ! -f docker/.env ]; then
  cp docker/.env.dist docker/.env

  key_dir="$(mktemp -d)"
  trap 'rm -rf "$key_dir"' EXIT
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$key_dir/private.pem" 2>/dev/null
  openssl rsa -pubout -in "$key_dir/private.pem" -out "$key_dir/public.pem" 2>/dev/null

  private_key="$(base64 < "$key_dir/private.pem" | tr -d '\n')"
  public_key="$(base64 < "$key_dir/public.pem" | tr -d '\n')"
  sed -i \
    -e "s|^JWT_PRIVATE_KEY=.*|JWT_PRIVATE_KEY=$private_key|" \
    -e "s|^JWT_PUBLIC_KEY=.*|JWT_PUBLIC_KEY=$public_key|" \
    docker/.env

  echo "Created docker/.env with generated JWT keys."
else
  echo "docker/.env already exists; leaving it unchanged."
fi

# Download Gradle and the project dependencies so the first build is fast.
./gradlew --quiet dependencies > /dev/null
echo "Devcontainer ready. Run ./gradlew bootRun to start the service."
