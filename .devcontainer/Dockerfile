FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# Extra OS packages for development; add more to the list and rebuild the container.
RUN apt-get update \
  && export DEBIAN_FRONTEND=noninteractive \
  && apt-get install -y --no-install-recommends \
    jq \
  && rm -rf /var/lib/apt/lists/*
