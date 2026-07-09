#!/usr/bin/env bash
# Docker wrapper for gradlew — runs builds inside a container with all required
# toolchains (Java, Go, .NET, Python) so no host-side toolchain exclusions are needed.
#
# Falls through to ./gradlew.real when already inside Docker.
# Uses bco-build-env:latest — rebuild with: docker build -t bco-build-env /tmp/bco-build-env/
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Already inside Docker — run the real Gradle Wrapper directly
if [ -f /.dockerenv ]; then
    exec "$SCRIPT_DIR/gradlew.real" "$@"
fi

# Shared Gradle user home for all Docker builds in this BCO session.
# Using a fixed path ensures BVS experiments share the same build cache.
DOCKER_GRADLE_HOME="/tmp/gradle-home-docker"
mkdir -p "$DOCKER_GRADLE_HOME"

# Pass Develocity access key from the host keys file into the container.
DV_HOST="ge.solutions-team.gradle.com"
KEYS_FILE="$HOME/.gradle/develocity/keys.properties"
ACCESS_KEY_ENV=""
if [ -f "$KEYS_FILE" ]; then
    KEY=$(grep "^${DV_HOST}=" "$KEYS_FILE" 2>/dev/null | cut -d= -f2- || true)
    [ -n "$KEY" ] && ACCESS_KEY_ENV="${DV_HOST}=${KEY}"
fi

exec docker run --rm \
    --network host \
    -v "${SCRIPT_DIR}:${SCRIPT_DIR}" \
    -v "${DOCKER_GRADLE_HOME}:/root/.gradle" \
    -v "${HOME}/.cache:${HOME}/.cache" \
    -v "${HOME}/.claude/plugins:${HOME}/.claude/plugins" \
    -v "/tmp:/tmp" \
    -w "${SCRIPT_DIR}" \
    -e "GRADLE_USER_HOME=/root/.gradle" \
    -e "HOME=/root" \
    -e "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dfile.io.encoding=UTF-8" \
    -e "LANG=en_US.UTF-8" \
    -e "LC_ALL=en_US.UTF-8" \
    -e "DEVELOCITY_ACCESS_KEY=${ACCESS_KEY_ENV}" \
    bco-build-env:latest \
    "${SCRIPT_DIR}/gradlew.real" "$@"
