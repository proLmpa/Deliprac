#!/usr/bin/env bash
# Bootstraps Elasticsearch with the ILM policy, index template,
# Logstash writer role, and Logstash writer user.
# Safe to run multiple times — all operations are idempotent.
# Usage: ELASTIC_PASSWORD=<pw> LOGSTASH_WRITER_PASSWORD=<pw> ./bootstrap-elk.sh

set -euo pipefail

ES="http://localhost:9200"
AUTH="-u elastic:${ELASTIC_PASSWORD}"

echo "==> ILM policy"
curl -sf ${AUTH} -X PUT "${ES}/_ilm/policy/baemin-logs-policy" \
  -H "Content-Type: application/json" \
  -d '{
    "policy": {
      "phases": {
        "delete": { "min_age": "7d", "actions": { "delete": {} } }
      }
    }
  }'

echo ""
echo "==> Index template"
curl -sf ${AUTH} -X PUT "${ES}/_index_template/baemin-logs" \
  -H "Content-Type: application/json" \
  -d '{
    "index_patterns": ["baemin-*"],
    "template": {
      "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
        "index.lifecycle.name": "baemin-logs-policy"
      },
      "mappings": {
        "properties": {
          "@timestamp":  { "type": "date" },
          "level":       { "type": "keyword" },
          "logger":      { "type": "keyword" },
          "service":     { "type": "keyword" },
          "traceId":     { "type": "keyword" },
          "message":     { "type": "text" },
          "stackTrace":  { "type": "text", "index": false }
        }
      }
    },
    "priority": 100
  }'

echo ""
echo "==> logstash_writer role"
curl -sf ${AUTH} -X PUT "${ES}/_security/role/logstash_writer" \
  -H "Content-Type: application/json" \
  -d '{
    "cluster": ["manage_index_templates", "monitor", "manage_ilm"],
    "indices": [{ "names": ["baemin-*"], "privileges": ["write","create","create_index","manage","manage_ilm"] }]
  }'

echo ""
echo "==> logstash_writer user"
curl -sf ${AUTH} -X PUT "${ES}/_security/user/logstash_writer" \
  -H "Content-Type: application/json" \
  -d "{
    \"password\": \"${LOGSTASH_WRITER_PASSWORD}\",
    \"roles\": [\"logstash_writer\"],
    \"full_name\": \"Logstash Writer\"
  }"

echo ""
echo "==> Bootstrap complete."
