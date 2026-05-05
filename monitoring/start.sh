#!/usr/bin/env bash
# Local dev only — starts Prometheus, Alertmanager, and Grafana as standalone containers.
# Requires TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID to be set in the environment.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Substitute local hostnames into config files
export BFF_HOST=host.docker.internal
export USER_HOST=host.docker.internal
export STORE_HOST=host.docker.internal
export ORDER_HOST=host.docker.internal
export NOTIFICATION_HOST=host.docker.internal

envsubst < "$SCRIPT_DIR/prometheus.yml"    > /tmp/baemin-prometheus.yml
envsubst < "$SCRIPT_DIR/alertmanager.yml"  > /tmp/baemin-alertmanager.yml

docker network create monitoring 2>/dev/null || true

docker run -d --name prometheus --network monitoring \
  -p 9090:9090 \
  -v /tmp/baemin-prometheus.yml:/etc/prometheus/prometheus.yml:ro \
  -v "$SCRIPT_DIR/alerting-rules.yml":/etc/prometheus/alerting-rules.yml:ro \
  prom/prometheus:v2.53.4

docker run -d --name alertmanager --network monitoring \
  -p 9093:9093 \
  -v /tmp/baemin-alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro \
  prom/alertmanager:v0.27.0

docker run -d --name grafana --network monitoring \
  -p 3000:3000 \
  -v "$SCRIPT_DIR/grafana/provisioning":/etc/grafana/provisioning:ro \
  -v "$SCRIPT_DIR/grafana/dashboards":/var/lib/grafana/dashboards:ro \
  grafana/grafana:11.4.0

echo "Prometheus  → http://localhost:9090"
echo "Alertmanager→ http://localhost:9093"
echo "Grafana     → http://localhost:3000  (admin / admin)"
