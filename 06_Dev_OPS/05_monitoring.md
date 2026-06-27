# Chapter 5 — Monitoring, Logging & Observability (My Notes)

> Reference repo: `/Users/somesh.sangwan/Desktop/rcash_api-roc`
> Real files taught: `roc/prod/common/caas/filebeat.yaml`, `roc/build/docker/custom_logging.properties`
> Your repo uses the FULL observability stack: New Relic (APM) + Filebeat + Logstash (logs).

---

## Section 1 — Why monitoring exists
Once running in prod: how do you know it's healthy? When it breaks at 3am, what & why?
Without monitoring you're blind: slow app (users complain first), failed payments (angry
customer tells you), memory creeping to crash (invisible), which Pod misbehaves (no idea).
Monitoring = giving your running system "senses".

---

## Section 2 — Three pillars of observability

| Pillar | What | Answers | Tool |
|--------|------|---------|------|
| 📊 **Metrics** | Numbers over time | "Is it healthy? How much load?" | Prometheus + Grafana |
| 📝 **Logs** | Timestamped text events | "What exactly happened?" | ELK (Elasticsearch/Logstash/Kibana) + Filebeat |
| 🔍 **Traces** | Path of one request across services | "Where is the slowness?" | New Relic, Jaeger |

Examples:
- Metric: "CPU 80%", "200 req/sec", "memory 450MB"
- Log: "Payment failed for user X", "Error: timeout"
- Trace: "request spent 80ms in DB, 200ms calling payment API"

> Your repo: New Relic (metrics+traces/APM), Filebeat+Logstash (logs). Uses all three.

---

## Section 3 — Metrics: Prometheus + Grafana pattern

A metric = a number sampled over time (req/sec, error rate, p95 latency, CPU%, memory, GC time).

```
Your App (exposes /metrics) ◄── Prometheus scrapes every ~15s (stores time-series)
                                      │ query (PromQL)
                                      ▼
                                 Grafana (dashboards 📊 + alerts 🚨)
```
- **Prometheus**: time-series DB, PULLS ("scrapes") metrics from your app, evaluates alert rules.
- **Grafana**: dashboard tool, connects to Prometheus, draws graphs.
- **Alerting**: "error rate > 5% for 5 min → page on-call".

> Office uses New Relic for this layer instead, but concept is identical: collect → visualize → alert.

---

## Section 4 — APM & New Relic (in YOUR Dockerfile)

Dockerfile Block 6 (Ch.1) downloaded the New Relic Java agent. APM = Application Performance Monitoring.

```
Pod → JVM (Payara)
        rcash-api code  ◄── New Relic agent (attaches INSIDE the JVM)
                              │ sends data
                              ▼
                        New Relic Cloud (SaaS): 📊 dashboards 🔍 traces 🚨 alerts
```
Agent runs inside the JVM, auto-instruments code, measures:
- Response time per endpoint (which API is slow)
- Throughput (req/min)
- Error rate (which endpoints throw)
- Traces (request journey → find WHERE the slowness is)
- JVM health: heap, garbage collection (ties to `MEM_MAX_RAM_PERCENTAGE=50` in Dockerfile)

Baked into the image → every Pod auto-reports to New Relic.
`acl-newrelic.yaml` files = NetworkPolicies opening the firewall door to New Relic servers
(of the 41 NetworkPolicies that act as Pod firewalls).

---

## Section 5 — Logging: ELK / Filebeat pattern (in YOUR repo)

### The problem
6 Pods × 2 datacenters = logs scattered everywhere. SSH into one Pod = only its logs.
Pod dies → its logs die (Ch.1 writable layer destroyed). Solution: centralized logging.

### The pattern
```
Each Pod writes log files → Filebeat ships them → Logstash processes → Elasticsearch + Kibana (search)
```
= **ELK stack** (Elasticsearch + Logstash + Kibana) with **Filebeat** as lightweight shipper.

### Real filebeat.yaml explained
```yaml
filebeat.inputs:
  - type: log
    paths: [ /opt/payara/logs/access.* ]    # web access logs
    fields: { geap_log_group: ..._access_logs }
  - type: log
    paths: [ /opt/payara/logs/server.log.* ] # app logs
    tags: ["json"]
  # also: gc logs, and dozens of business logs (m-payvault, m-error, m-riskrating, ...)
```
- `filebeat.inputs` = list of log files to tail (like `tail -f`) and ship.
- `geap_log_group` = label so central system buckets each log type.

**Multiline config (clever):**
```yaml
multiline.pattern: '^[0-9]{4}/[0-1][0-9]/[0-3][0-9]\s...'  # matches a date 2026/06/26 14:30:01.123
multiline.negate: true
multiline.match: after
```
- Problem: a Java stack trace = many lines but ONE event. Without this each line = separate log.
- Rule: "new entry starts with a timestamp; any line NOT starting with timestamp is appended
  to the previous entry." → 30-line stack trace becomes one event.

**Output:**
```yaml
output.logstash:
  hosts: ${EAAS_HOSTS}          # ← from CI deploy vars!
  compression_level: 9          # save bandwidth
  loadbalance: true             # spread across logstash hosts
  ssl.certificate_authorities: /config/filebeat/root-ca.crt  # encrypted
```

> FULL CIRCLE: `.gitlab-rcash-dev.yaml` deploy job set:
>   EAAS_HOSTS: '[...logstash-gw101...:6227, ...]'
>   EAAS_SECRET: $STG_JPE_EAAS_SECRET
> These inject right here into Filebeat output. EAAS = Elasticsearch as a Service
> (Rakuten managed ELK). CI pipeline (Ch.3) wires logging to the central platform.

### custom_logging.properties (the file Dockerfile's sed edited in Ch.1!)
```properties
handlers=...ConsoleHandler,...FileHandler
FileHandler.limit=10485760       # rotate at 10MB
FileHandler.count=10             # keep 10 rotated files
FileHandler.formatter=co.elastic.logging.jul.EcsFormatter   # structured JSON logs!
FileHandler.pattern=${RCASH_LOGS_DIR}/server.log
EcsFormatter.serviceName=${RCASH_SERVICE_NAME}
```
- `EcsFormatter` = Elastic Common Schema → writes logs as structured JSON (so ES indexes
  every field). That's why a filebeat input had `tags: ["json"]`.
- `limit + count` = log rotation (prevents filling the disk).
- `${RCASH_LOGS_DIR}` / `${RCASH_SERVICE_NAME}` = the exact placeholders Dockerfile `sed`
  replaced in Ch.1 Block 9 → so logs are tagged with service name + written to the volume Filebeat reads.

---

## Section 6 — Complete observability picture for rcash-api
```
rcash-api Pod
  JVM (Payara) + rcash-api code
     │  └─ New Relic agent ──► New Relic Cloud (METRICS + TRACES) 📊🔍🚨
     │
     └─ writes logs → /opt/payara/logs/*.log (volume)
                          │
                       Filebeat (ships, compressed + SSL)
                          │
                       Logstash (EAAS) → Elasticsearch + Kibana (LOGS 🔍 search)
```
METRICS + TRACES → New Relic   |   LOGS → Filebeat → Logstash → Elasticsearch/Kibana

---

## Chapter 5 Summary
| Concept | One line |
|---------|---------|
| Why monitoring | Know if prod is healthy; diagnose what/why when it breaks |
| 3 pillars | Metrics (numbers), Logs (events), Traces (request journeys) |
| Metrics | Prometheus scrapes + stores, Grafana visualizes, alerts on rules |
| APM | App Performance Monitoring — agent inside the app (New Relic in your Dockerfile) |
| Traces | Follow one request across services to find the slow part |
| Centralized logging | Ship all Pod logs to one searchableplace (logs die with Pods otherwise) |
| ELK | Elasticsearch + Logstash + Kibana; Filebeat = lightweight shipper |
| Filebeat multiline | Joins multi-line stack traces into one log event |
| EcsFormatter | Writes logs as structured JSON for indexing/search |
| Full circle | CI vars (EAAS_HOSTS/SECRET) wire Filebeat → central Logstash |

---

## My Questions & Clarifications

### Q: We use Prometheus+Grafana (CPU/mem), New Relic (transactions/QPS), PagerDuty. Where is the config? How are they connected?

**KEY INSIGHT: monitoring config lives in TWO places. Most is NOT in rcash_api-roc.**

| Category | What | Where |
|----------|------|-------|
| "Agent" side (app/cluster) | New Relic agent ON, log shipping, resource limits | ✅ THIS repo |
| "Backend/dashboard" side | Grafana dashboards, Prometheus scrape rules, alert thresholds, PagerDuty routing | ❌ Platform team / web UIs |

#### 1. New Relic — configured HERE (the Ch.1 missing link!)
`roc/prod/common/caas/rcash-deployment.yaml` JVM_ARGS (line ~100) turns the agent ON:
```yaml
- name: JVM_ARGS
  value: "... -javaagent:/opt/payara/newrelic/newrelic.jar
          -Dnewrelic.config.app_name=$(NEWRELIC_APP_NAME)
          -Dnewrelic.config.license_key=$(NEWRELIC_LICENSE_KEY) ..."
```
- Dockerfile (Ch.1) DOWNLOADED newrelic.jar. THIS file SWITCHES IT ON via `-javaagent:`.
- `app_name` = name shown in New Relic dashboard.
- `license_key` = connects to company's New Relic account (from rcash-api-secrets).
- Your transactions / QPS / response times come from THIS agent.
- `acl-newrelic.yaml` = NetworkPolicy/firewall letting Pod reach New Relic servers.
- Dashboards & alert rules you VIEW = configured in New Relic web UI (not any repo).

#### 2. Prometheus + Grafana (CPU/mem) — NOT in repo (and that's correct)
```
Your Pod → (kubelet/cAdvisor auto-exposes CPU & memory)
         → Platform's SHARED Prometheus scrapes ALL pods
         → SHARED Grafana shows dashboards 📊
```
- CPU/memory are INFRASTRUCTURE metrics — Kubernetes exposes them automatically for
  every Pod. Your app does nothing.
- CPD platform team runs ONE shared Prometheus + ONE shared Grafana for the whole cluster.
- The ONLY Prometheus-related thing you own = resource limits in your deployment:
  ```yaml
  resources:
    limits: { cpu: 2, memory: 6.0Gi }   # ← Grafana shows usage AGAINST this 6Gi ceiling
    requests: { cpu: 2, memory: 6.0Gi }
  ```
- When Grafana shows "memory 4.2Gi / 6Gi" after a release — the 6Gi ceiling is defined here.

#### 3. PagerDuty — NOT in repo; last link in the alert chain
```
Prometheus/New Relic (metric crosses threshold)
   → Alertmanager / New Relic Alert Policy (fires alert)
   → PagerDuty (routing, escalation, on-call schedule)
   → You get paged 📟
```
- Metric breaks a rule → alerting system fires → it has a PagerDuty integration KEY →
  calls PagerDuty API → PagerDuty decides who/escalation → you're paged.

| Piece | Configured in | In repo? |
|-------|--------------|----------|
| Alert threshold CPU/mem | Prometheus Alertmanager (platform team) | ❌ |
| Alert threshold txns/errors | New Relic web UI → Alert Policies | ❌ |
| PagerDuty integration key | New Relic / Alertmanager settings | ❌ |
| On-call schedule, escalation | PagerDuty web UI | ❌ |

#### Where config lives (direct answer)
```
IN THIS REPO (rcash_api-roc)              NOT IN REPO (elsewhere)
• New Relic agent ON + app name           • Grafana dashboards (Grafana UI)
  (rcash-deployment.yaml JVM_ARGS)        • Prometheus scrape config (CPD team)
• New Relic license key ref (secrets)     • Alert thresholds (New Relic UI / Alertmgr)
• Resource limits (6Gi ceiling)           • PagerDuty keys/schedules/escalation (PD UI)
• Log shipping (filebeat.yaml)            • New Relic dashboards (NR UI)
• Firewall to New Relic (acl-newrelic)
```

**Rule of thumb:** your repo configures WHAT YOUR APP EMITS (agent on, ship logs, declare
limits). Dashboards/alerts/PagerDuty routing live in web UIs / platform repos because
they're shared across teams and changed without redeploying your app.

**To find the rest:** New Relic console (Alert Policies → notification channels → PagerDuty);
PagerDuty console (services & escalation); a separate CPD/platform GitLab repo for
Prometheus Alertmanager rules. Ask team lead which CPD repo holds Alertmanager config.

---

# 🎓 CAPSTONE — The Complete DevOps Picture (all 5 chapters)

Everything you learned, as ONE flow for rcash-api:

```
1. CODE         Developer writes Java code, pushes to dev/* branch
                      │
2. CI/CD (Ch.3) GitLab pipeline triggers (manual gate):
                  fetch → build (.war via Maven)
                  → docker (Kaniko builds the Dockerfile → Harbor registry)
                  → deploy (kubectl apply to BOTH clusters)
                      │
3. DOCKER (Ch.1) Dockerfile packages: Payara + JVM + New Relic agent + .war + logging config
                      │
4. K8s (Ch.2)   Deployment runs N replica Pods across nodes in each cluster
                  Service (ClusterIP) routes to healthy Pods by label (BRANCH_SLUG)
                  DLB Service (LoadBalancer) exposes externally via Rakuten gateway
                      │
5. CLOUD/IaC(Ch4) GSLB JSON routes users to jpe2b/jpw1a (failover by health check)
                  LBaaS spreads traffic to worker nodes
                  DBaaS (MariaDB) holds data
                      │
6. MONITORING(Ch5) New Relic agent → metrics/traces to New Relic Cloud
                  Filebeat → logs → Logstash (EAAS) → Elasticsearch/Kibana
                      │
                   Live, observable, self-healing, multi-datacenter service ✅
```

### Request flow (user → answer), tying Ch.2 + Ch.4 + Ch.5:
```
User → GSLB (pick region) → LBaaS (pick machine) → DLB/Service (pick Pod)
     → Pod → Container (Payara :8080) → answers
     (all the while: New Relic measures it, Filebeat ships its logs)
```

### Next steps to go deeper (personal PC)
- Run the Docker labs (Ch.1) + minikube labs (Ch.2)
- Build a CI pipeline on a personal GitLab/GitHub repo (Ch.3)
- Provision something with Terraform (Ch.4)
- Spin up Prometheus + Grafana locally, or a free New Relic account (Ch.5)
- Capstone: take any small app → Dockerize → CI/CD → deploy to minikube → add monitoring