#!/usr/bin/env bash
set -euo pipefail

MIRROR_URL=${1:-${MIRROR_URL:-https://hub-mirror.c.163.com}}

if [ -z "$MIRROR_URL" ]; then
  echo "Specify a mirror URL via argument or MIRROR_URL environment variable. Example: $0 https://hub-mirror.c.163.com"
  exit 1
fi

echo "Using registry mirror: $MIRROR_URL"

# Require sudo for daemon file and restart
if [ "$EUID" -ne 0 ]; then
  echo "This script requires sudo to write /etc/docker/daemon.json and restart Docker. You may be prompted for your password."
fi

sudo bash -c "mkdir -p /etc/docker"

# Backup existing daemon.json if any
sudo bash -c 'if [ -f /etc/docker/daemon.json ]; then cp /etc/docker/daemon.json /etc/docker/daemon.json.bak.$(date +%s); echo "Backed up /etc/docker/daemon.json"; fi'

# Write a minimal daemon.json with registry-mirrors. If you need to preserve additional keys, merge manually.
sudo bash -c "cat > /etc/docker/daemon.json <<JSON
{
  \"registry-mirrors\": [\"$MIRROR_URL\"]
}
JSON"

echo "Wrote /etc/docker/daemon.json with registry mirror. Restarting Docker..."

sudo systemctl restart docker
sleep 2

# Verify docker is active
if ! sudo systemctl is-active --quiet docker; then
  echo "Docker service is not running after restart. Check 'systemctl status docker' for details." >&2
  exit 2
fi

echo "Docker restarted successfully. Attempting to pull test images (node:18-alpine, nginx:stable-alpine)..."

sudo docker pull node:18-alpine || true
sudo docker pull nginx:stable-alpine || true

echo "Setup finished. If pull failed, you may try a different mirror (e.g., https://registry.docker-cn.com or https://mirror.ccs.tencentyun.com)."
