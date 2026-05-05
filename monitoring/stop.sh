#!/usr/bin/env bash
# Stops and removes the local monitoring containers.
set -euo pipefail

docker stop prometheus alertmanager grafana 2>/dev/null || true
docker rm   prometheus alertmanager grafana 2>/dev/null || true
docker network rm monitoring 2>/dev/null || true
