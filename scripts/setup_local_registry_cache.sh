#!/usr/bin/env bash
set -euo pipefail

REG_DIR=$(pwd)/registry
CONF=$REG_DIR/config.yml
CACHE_NAME=registry-cache

echo "Using registry config: $CONF"

# Ensure config exists
if [ ! -f "$CONF" ]; then
  echo "Missing $CONF" >&2
  exit 1
fi

# Start registry container with pull-through cache config
if docker ps -a --format '{{.Names}}' | grep -q "^${CACHE_NAME}$"; then
  echo "Registry container exists; starting it"
  docker start ${CACHE_NAME} || true
else
  echo "Creating registry container ${CACHE_NAME}..."
  docker run -d -p 5000:5000 --name ${CACHE_NAME} \
    -v "$CONF:/etc/docker/registry/config.yml:ro" \
    -v "${REG_DIR}/data:/var/lib/registry" \
    registry:2 || true
fi

# Add local registry to Docker daemon config as insecure registry and mirror
DAEMON_JSON=/etc/docker/daemon.json
sudo bash -c 'if [ -f /etc/docker/daemon.json ]; then cp /etc/docker/daemon.json /etc/docker/daemon.json.bak.$(date +%s); fi'

sudo bash -c "cat > ${DAEMON_JSON} <<JSON
{
  \"registry-mirrors\": [\"http://localhost:5000\"],
  \"insecure-registries\": [\"localhost:5000\"]
}
JSON"

sudo systemctl restart docker
sleep 2

if ! sudo systemctl is-active --quiet docker; then
  echo "Docker not running after restart" >&2
  exit 2
fi

echo "Local registry cache setup attempted. Try pulling images (node:18-alpine/nginx:stable-alpine):"
docker pull node:18-alpine || true
docker pull nginx:stable-alpine || true

echo "If pulls still fail, your host network blocks Docker Hub; consider configuring a proxy or using a different upstream mirror."
