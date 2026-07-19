# ELK Stack — Centralized Log Aggregation

Centralized logging for the Baemin platform using **Elasticsearch 8.x · Logstash · Kibana · Filebeat**.

All six Spring Boot services emit structured JSON to stdout. Filebeat ships those logs to Logstash, which normalizes and indexes them into Elasticsearch. Kibana provides search and analysis on top.

---

## 1. Architecture

```
┌─────────────────────────────── Kubernetes (minikube) ───────────────────────────────┐
│                                                                                      │
│  user-service   store-service   order-service   notification-service                 │
│  bff-service    ai-service                                                           │
│       │               │               │                │           │         │      │
│       └───────────────┴───────────────┴────────────────┴───────────┴─────────┘      │
│                              stdout (JSON, prod profile)                             │
│                                        │                                            │
│                              ┌─────────▼──────────┐                                 │
│                              │ Filebeat DaemonSet  │  tails /var/log/containers/    │
│                              │  (baemin namespace) │  namespace-filtered            │
│                              └─────────┬───────────┘                                │
└────────────────────────────────────────┼────────────────────────────────────────────┘
                                         │  Beats protocol :5044
                                         ▼
                              ┌────────────────────────────────────┐
                              │           ELK VM                   │
                              │                                    │
                              │  Logstash :5044                    │
                              │    ├── parse JSON                  │
                              │    ├── rename fields               │
                              │    └── drop noise                  │
                              │         │                          │
                              │         ▼                          │
                              │  Elasticsearch :9200               │
                              │    └── baemin-{service}-YYYY.MM.dd │
                              │         │                          │
                              │         ▼                          │
                              │  Kibana :5601                      │
                              └────────────────────────────────────┘
```

### Files in this directory

```
elk/
├── logstash/
│   └── conf.d/
│       └── baemin.conf        # Logstash input → filter → output pipeline
└── setup/
    └── bootstrap-elk.sh       # Idempotent Elasticsearch bootstrap (ILM, template, user)
```

---

## 2. Key Design Decisions

### Per-service daily indices

Logs are written to `baemin-{service}-{YYYY.MM.dd}` (e.g. `baemin-user-service-2026.07.20`).

One index per service per day means:
- Kibana filters by service without a query clause — just select the index pattern
- A single noisy service can be rolled or deleted independently
- Logs with no `service` field (framework startup noise, unrecognized formats) land in `baemin-unknown-*` rather than polluting service indices

### Keyword vs. text field mapping

```
@timestamp   → date     (time-range queries)
level        → keyword  (exact filter: ERROR, WARN, INFO)
logger       → keyword  (exact filter by class name)
service      → keyword  (exact filter by service)
traceId      → keyword  (exact lookup — never partial match)
message      → text     (full-text search)
stackTrace   → text, NOT indexed  (stored but not searchable — saves disk)
```

`stackTrace` is stored (`index: false`) so it appears in Kibana document view but does not bloat the inverted index. Stack traces are only useful when you already have the document — you never search for partial stack trace fragments.

### 7-day ILM delete policy

A single delete-only ILM phase removes indices after 7 days. No warm/cold tiers, no rollover — the platform is single-node, so replica and rollover overhead would be pure cost. 7 days covers any realistic incident investigation window.

### `logstash_writer` least-privilege role

Logstash authenticates as a dedicated `logstash_writer` user rather than the `elastic` superuser. The role grants only:
- `manage_index_templates`, `monitor`, `manage_ilm` (cluster level)
- `write`, `create`, `create_index`, `manage`, `manage_ilm` on `baemin-*` indices

If Logstash credentials leak, the blast radius is limited to baemin log indices.

### Distributed trace correlation via `traceId`

Every log line carries a `traceId` keyword field that is the same value across all services that handled a single request:

| Component | What it does |
|---|---|
| `MdcFilter` (common, `HIGHEST_PRECEDENCE`) | Reads `X-Request-Id` header or generates a UUID; stores in MDC as `traceId`; clears MDC at request end |
| `TraceIdInterceptor` (BFF + ai-service, each `RestClient`) | Copies `traceId` from MDC into `X-Request-Id` on every outbound call |
| `MdcFilter` (backend services) | Receives the forwarded `X-Request-Id` and stores it in MDC — same `traceId` appears in backend logs |
| `runAsyncWithMdc` (common) | Captures MDC before handing off to a thread pool; restores it inside the async block so fire-and-forget notification calls also carry `traceId` |

In Kibana: filter `traceId: "a1b2c3"` → see the complete request journey across bff, store, order, and notification logs in one view.

---

## 3. How It Works

### Log pipeline flow

```
Spring Boot service (prod profile)
  │
  │  logstash-logback-encoder emits one JSON line per log event to stdout:
  │  {"@timestamp":"2026-07-20T14:00:00.000Z","level":"INFO",
  │   "logger_name":"o.s.boot.SpringApplication","service":"user-service",
  │   "traceId":"a1b2c3d4","message":"Started in 2.1s"}
  │
  ▼
Kubernetes writes to /var/log/containers/<pod>_baemin_<container>-<id>.log
  │
  ▼
Filebeat DaemonSet
  ├── Input: /var/log/containers/*.log
  ├── Filter: namespace == "baemin" only
  └── Output: Logstash :5044 (Beats protocol)
  │
  ▼
Logstash baemin.conf
  ├── input  { beats { port => 5044 } }
  ├── filter
  │     ├── json { source => "message" target => "spring" }        # parse JSON
  │     ├── date { match => ["[spring][@timestamp]", "ISO8601"] }  # fix timestamp
  │     ├── mutate { rename { ... }, remove_field => [...] }       # flatten + drop noise
  │     └── if ![service] → mutate { add_field "service" => "unknown" }
  └── output
        └── elasticsearch { index => "baemin-%{service}-%{+YYYY.MM.dd}" }
  │
  ▼
Elasticsearch index: baemin-user-service-2026.07.20
  └── ILM policy: auto-delete after 7 days
  │
  ▼
Kibana :5601
  └── Index pattern: baemin-*
```

### Logstash filter detail (`baemin.conf`)

The filter has two guards:

**Guard 1 — only parse lines that look like JSON:**
```
if [message] =~ /^\{/ {
  json { source => "message"  target => "spring"  skip_on_invalid_json => true }
}
```
Non-JSON lines (Tomcat access logs, JVM startup text) pass through unparsed and land in `baemin-unknown-*` via the `![service]` fallback.

**Guard 2 — only rename/clean when JSON parse succeeded:**
```
if [spring][level] {
  date   { ... }   # replace Filebeat timestamp with the application's own timestamp
  mutate {
    rename       => { "[spring][message]" => "message", ... }
    remove_field => ["spring", "log", "input", "agent", "ecs"]
  }
}
```
`rename` and `remove_field` are in a **single `mutate` block** — Logstash guarantees `rename` executes before `remove_field` within the same block. This prevents the rename block from running on raw/non-JSON lines, which would create empty fields and bloat documents.

**Guard 3 — set `service` to `unknown` for non-JSON lines:**
```
if ![service] {
  mutate { add_field => { "service" => "unknown" } }
}
```
Non-JSON lines that pass through Guard 1 unparsed have no `service` field. This guard sets it to `"unknown"` so they land in `baemin-unknown-YYYY.MM.dd` rather than producing an index with an empty segment in the name. The Elasticsearch output can then use plain `%{service}` — always populated at this point.

### `bootstrap-elk.sh` — what it sets up

Run once (idempotent — safe to re-run on every deploy):

```
ELASTIC_PASSWORD=<pw> LOGSTASH_WRITER_PASSWORD=<pw> ./setup/bootstrap-elk.sh
```

| Step | Elasticsearch API | Effect |
|---|---|---|
| ILM policy | `PUT /_ilm/policy/baemin-logs-policy` | Delete phase at 7 days |
| Index template | `PUT /_index_template/baemin-logs` | Applies mapping + ILM to all `baemin-*` indices; 1 shard, 0 replicas |
| Writer role | `PUT /_security/role/logstash_writer` | Least-privilege write access to `baemin-*` |
| Writer user | `PUT /_security/user/logstash_writer` | Service account for Logstash |

The template priority is `100`, which overrides any lower-priority default Elasticsearch templates for the `baemin-*` pattern.

---

## 4. CI/CD Integration

Stage 5 of the Jenkins pipeline ("Deploy ELK Config") runs **after** Stage 4 ("Deploy") sequentially, and **before** Stage 6 ("Update Helm Tag") so Elasticsearch is ready to receive logs before new pods start rolling out.

```
Stage 3: Push Images
      │
Stage 4: Deploy  (monitoring — Prometheus, Alertmanager, Grafana)
      │
Stage 5: Deploy ELK Config
      │
      ├── ssh-keyscan  (pre-populate ELK VM host key — replaces StrictHostKeyChecking=no)
      ├── scp baemin.conf → /opt/elk/logstash/conf.d/
      ├── ssh: sudo cp to /etc/logstash/conf.d/ && sudo systemctl restart logstash
      ├── scp bootstrap-elk.sh → /opt/elk/setup/
      └── ssh: chmod +x && ELASTIC_PASSWORD=... LOGSTASH_WRITER_PASSWORD=... ./bootstrap-elk.sh
            │
Stage 6: Update Helm Tag  (git push — triggers ArgoCD sync)
```

### Jenkins credentials required

| Credential ID | Type | Used for |
|---|---|---|
| `elk-host` | SSH Username with private key | `sshagent` — SSH/SCP to the ELK VM |
| `ELK_HOST` | Secret text | `user@<ELK_VM_IP>` — SSH target |
| `ELASTIC_PASSWORD` | Secret text | Elasticsearch `elastic` superuser password (passed to `bootstrap-elk.sh`) |
| `LOGSTASH_WRITER_PASSWORD` | Secret text | `logstash_writer` account password (passed to `bootstrap-elk.sh`) |

---

## 5. Code Review — AS-IS / TO-BE

### 5-1. Missing Artifacts

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 1 | Filebeat configuration | No Filebeat config file, Kubernetes DaemonSet manifest, or Helm chart exists anywhere in `elk/` or `helm/baemin/` — the README describes the namespace filter and DaemonSet in detail but the actual config cannot be reproduced from this repo | Add `elk/filebeat/filebeat.yml` (or a Helm values fragment) covering: `filebeat.inputs` path to `/var/log/containers/*.log`, `processors` to drop events where `kubernetes.namespace != "baemin"`, and `output.logstash` pointing to the ELK VM at `:5044` |

---

### 5-2. Security

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 3 | `logstash_writer` role privileges | `bootstrap-elk.sh` grants `["write","create","create_index","manage","manage_ilm"]` on `baemin-*`; the `manage` privilege includes delete, close, and refresh — if Logstash credentials leak, an attacker can wipe all log indices | Remove `manage`; a log shipper needs only `["write","create","create_index","manage_ilm"]` — sufficient to write documents, create daily indices, and poll ILM status |
| 4 | Transport security | Both `bootstrap-elk.sh` (`ES="http://localhost:9200"`) and `baemin.conf` (`hosts => ["http://localhost:9200"]`) use plaintext HTTP; the `elastic` superuser and `logstash_writer` credentials are transmitted unencrypted | Document this as an explicit constraint ("co-located on the same VM; TLS is not required for loopback"); if Elasticsearch is ever split to a separate host, switch to `https://` and supply a CA certificate in both the script and the conf before doing so |
| 9 | `elastic` superuser in CI pipeline | Stage 5 runs `bootstrap-elk.sh` with `ELASTIC_PASSWORD` on every deploy, meaning the `elastic` superuser credential must live as a permanent Jenkins secret and is exercised on every CI run | Treat bootstrap as a one-time manual operation (run only when ILM policy, template, or role definitions change); remove `ELASTIC_PASSWORD` from the regular pipeline and replace Stage 5 with a step that only restarts Logstash after deploying `baemin.conf` |

---

### 5-3. Code Smell

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 5 | Redundant `[spring][thread_name]` in `remove_field` | `baemin.conf` line 32: `remove_field => ["spring", ..., "[spring][thread_name]"]` — `"spring"` already removes the entire parent hash including all children; the explicit `[spring][thread_name]` entry is a no-op | Remove the redundant `"[spring][thread_name]"` entry; if `thread_name` must be retained as a top-level field, add it to the `rename` block before the parent is dropped |

---

### 5-4. Undocumented Decisions / Missing Steps

| # | Area | AS-IS | TO-BE |
|---|---|---|---|
| 7 | `thread_name` field handling | `thread_name` is silently discarded — left inside the `spring` hash and deleted with it; not mentioned in the mapping table or anywhere in the README | Add a note to the mapping table: `thread_name → discarded (not promoted)`; if thread-pool debugging is needed, add `"[spring][thread_name]" => "threadName"` to the `rename` block and `"threadName": { "type": "keyword" }` to the index template |
| 8 | Kibana index pattern setup | `bootstrap-elk.sh` automates ILM, template, role, and user; Kibana index pattern creation is not automated and not mentioned — a first-time user hits a blank Kibana with no data view | Document the manual step: open Kibana → Stack Management → Data Views → Create `baemin-*` with `@timestamp`; or automate it with `POST /api/data_views/data_view` to the Kibana API (`:5601`) using `elastic` credentials |
