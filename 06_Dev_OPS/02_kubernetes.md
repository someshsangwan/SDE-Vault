# Chapter 2 — Orchestration with Kubernetes (My Notes)

> Reference repo: `/Users/youruser/Desktop/acme-api-infra`
> Hands-on requires minikube — try on personal PC (not office PC).

---

## Section 1 — Why Kubernetes exists

### The problem with raw Docker at scale

Running containers manually across many servers breaks down fast:

| Problem | Docker alone |
|---------|-------------|
| Container crashes at 3am | Find out from users, restart manually |
| Deploy new version with zero downtime | Take down one by one manually, risky |
| Server dies (hardware failure) | Container gone, manual recovery |
| Traffic spike — need 10 containers | Run `docker run` 7 more times manually |
| Which server has free resources? | SSH into each server, check `top` |
| Roll back broken deploy | Run old image on each server manually |

### What Kubernetes does
You declare **desired state** → K8s makes reality match it, forever.

```
You: "Run 3 copies of acme-api at all times"

K8s handles:
  ✓ Picks servers with free resources
  ✓ Starts containers on those servers
  ✓ Crash → auto restart
  ✓ Server dies → moves container elsewhere
  ✓ New version → rolling update (zero downtime)
  ✓ Traffic spike → auto scale
  ✓ Bad deploy → auto rollback
```

This is called **desired state management**.

---

## Section 2 — Kubernetes Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      K8s Cluster                          │
│                                                           │
│  ┌─────────────────────────────────┐                     │
│  │         Control Plane            │  ← "the brain"     │
│  │                                  │                     │
│  │  API Server   etcd               │                     │
│  │  Scheduler    Controller Manager │                     │
│  └─────────────────────────────────┘                     │
│                                                           │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ Worker Node│ │ Worker Node│ │ Worker Node│           │
│  │  kubelet   │ │  kubelet   │ │  kubelet   │           │
│  │  kube-proxy│ │  kube-proxy│ │  kube-proxy│           │
│  │  [pods]    │ │  [pods]    │ │  [pods]    │           │
│  └────────────┘ └────────────┘ └────────────┘           │
└──────────────────────────────────────────────────────────┘
```

### Control Plane components

| Component | Role | Analogy |
|-----------|------|---------|
| **API Server** | Front door — all kubectl commands hit here. Nothing bypasses it. | Manager's desk |
| **etcd** | Distributed key-value DB — stores entire cluster state (pods, configs, secrets) | Cluster's memory/brain |
| **Scheduler** | Decides which worker node a new pod runs on (checks free CPU/RAM) | Hotel receptionist assigning rooms |
| **Controller Manager** | Control loops that watch state and fix drift ("only 2 replicas, need 3 → create 1") | Thermostat |

### Worker Node components

| Component | Role |
|-----------|------|
| **kubelet** | Agent on every node. Gets instructions from API Server, tells container runtime to start/stop containers |
| **kube-proxy** | Handles networking rules on the node — routes traffic to correct pods |

---

## Section 3 — Core Kubernetes Objects

### Pod — smallest unit
Wrapper around one (or a few) containers. Smallest thing K8s can schedule.
You almost never create Pods directly — use Deployments instead.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: acme-api-pod
spec:
  containers:
    - name: acme-api
      image: acme-api:latest
      ports:
        - containerPort: 8080
```

### Deployment — manages pods + rolling updates
Declares "run N copies, keep them alive, handle updates gracefully."

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: acme-api
spec:
  replicas: 3                      # always keep 3 running
  selector:
    matchLabels:
      app: acme-api
  template:
    metadata:
      labels:
        app: acme-api
    spec:
      containers:
        - name: acme-api
          image: acme-api:1.0
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "256Mi"      # minimum guaranteed
              cpu: "250m"          # 250 millicores = 0.25 CPU
            limits:
              memory: "512Mi"      # max (enforced by cgroups — Ch.1)
              cpu: "500m"
```

Rolling update: update image → K8s starts new pods one by one, waits for health → removes old pods. Zero downtime.

### Service — stable network endpoint
Pods are temporary (different IP each time). A Service gives a stable name + IP forever.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: acme-api-service
spec:
  selector:
    app: acme-api                 # routes to pods with this label
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

Service types:
| Type | Access | Use case |
|------|--------|---------|
| `ClusterIP` | Inside cluster only | Pod-to-pod communication (default) |
| `NodePort` | Outside via node IP + port | Dev/testing |
| `LoadBalancer` | Cloud load balancer provisioned | Production (= LBaaS in office) |

> Office LBaaS = Kubernetes LoadBalancer Service — K8s asks cloud to provision a real LB.

### Namespace — logical isolation
Divide one cluster into virtual sub-clusters.

```
cluster
├── namespace: dev
├── namespace: qa
├── namespace: staging
└── namespace: prod
```

Your office `.gitlab-ci.yml` has separate files per env (dev/qa/st/stg/prod) — each deploys
to a different namespace on the same cluster.

---

## Section 4 — More Kubernetes Objects

### ConfigMap — non-secret config
Store config outside the image — no rebuild needed to change a setting.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: acme-api-config
data:
  DB_HOST: "mariadb-service"
  TZ: "Asia/Tokyo"
  LOG_LEVEL: "INFO"
```

Use in pod:
```yaml
envFrom:
  - configMapRef:
      name: acme-api-config
```

### Secret — sensitive config
Same as ConfigMap but for passwords/tokens. Base64 encoded at rest.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: acme-api-secrets
type: Opaque
data:
  DB_PASSWORD: c2VjcmV0MTIz    # base64 encoded value
```

> `.gitlab-ci.yml` has `MASTER_SECRET: "$MASTER_SECRET"` — GitLab CI secret →
> injected into pipeline → creates a K8s Secret object at deploy time.

### Ingress — HTTP routing
Smart HTTP router — routes traffic by hostname or URL path.

```
Internet → Ingress
  api.acme.com   → acme-api Service
  admin.acme.com → acme-admin Service
```

> Office GSLB (Global Server Load Balancer) sits in front of Ingress —
> DNS-based routing to jpe2b or jpw1a datacenter.

---

## Section 5 — kubectl commands

```bash
# CLUSTER INFO
kubectl cluster-info
kubectl get nodes
kubectl get nodes -o wide              # with IPs and roles

# PODS
kubectl get pods                       # default namespace
kubectl get pods -n qa                 # specific namespace
kubectl get pods -A                    # ALL namespaces
kubectl describe pod <pod-name>        # full detail: events, config, status
kubectl logs <pod-name>                # container stdout
kubectl logs <pod-name> -f             # follow live
kubectl logs <pod-name> --previous     # logs from crashed container
kubectl exec -it <pod-name> -- sh      # shell into pod (like docker exec)

# DEPLOYMENTS
kubectl get deployments
kubectl describe deployment acme-api
kubectl scale deployment acme-api --replicas=5
kubectl rollout status deployment acme-api    # watch rollout
kubectl rollout history deployment acme-api   # history
kubectl rollout undo deployment acme-api      # roll back

# SERVICES
kubectl get services
kubectl describe service acme-api-service

# APPLY / DELETE
kubectl apply -f deployment.yaml       # create or update
kubectl delete -f deployment.yaml      # delete
kubectl delete pod <pod-name>          # delete pod (Deployment recreates it)

# NAMESPACES
kubectl get namespaces
kubectl config set-context --current --namespace=qa  # switch default namespace
```

---

## Section 6 — How office uses Kubernetes (acme-api-infra)

### Deploy scripts
```bash
cd icp/qa/jpe2b/
./00_deploy.sh
```
Almost certainly runs `kubectl apply -f` — applying Deployment + Service YAMLs to the cluster.

### Multiple environments
```
.gitlab-acme-dev.yaml   → kubectl apply to dev namespace
.gitlab-acme-qa.yaml    → kubectl apply to qa namespace
.gitlab-acme-stg.yaml   → kubectl apply to stg namespace
.gitlab-acme-prod.yaml  → kubectl apply to prod namespace
```
Same YAML, different namespace (or different cluster) per environment.

### KUBECTL_IMAGE in CI
```yaml
KUBECTL_IMAGE: registry-jpe2.acme-registry.internal/.../bitnami/kubectl:1.19.9
```
The CI pipeline spins up THIS Docker container, runs `kubectl apply` from inside it,
and the deploy happens. CI runner = Docker container that talks to K8s cluster.

### Infrastructure mapping
| Office term | Kubernetes term |
|-------------|----------------|
| CaaS | Kubernetes cluster |
| LBaaS | Service type: LoadBalancer |
| GSLB | Ingress + DNS (multi-datacenter routing) |
| DBaaS | External managed DB (referenced by Service/Endpoint) |
| jpe2b / jpw1a | Two separate clusters (or two regions of one cluster) |

---

## Section 7 — Hands-on (personal PC only — needs minikube)

### Setup
```bash
brew install minikube
minikube start
kubectl get nodes           # one node: "minikube"
kubectl get pods -A         # K8s system pods
```

### Deploy an app
Save as `deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-app
          image: nginx:alpine
          ports:
            - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: my-app-service
spec:
  selector:
    app: my-app
  ports:
    - port: 80
      targetPort: 80
  type: NodePort
```

```bash
kubectl apply -f deployment.yaml
kubectl get pods                         # 3 pods running
kubectl get services
minikube service my-app-service          # open in browser
```

### Self-healing (K8s restarts crashed pods)
```bash
kubectl delete pod <any-pod-name>
kubectl get pods                         # new pod appears in seconds
```

### Rolling update
```bash
kubectl set image deployment/my-app my-app=nginx:latest
kubectl rollout status deployment/my-app  # watch pod-by-pod rollout
kubectl rollout history deployment/my-app
kubectl rollout undo deployment/my-app    # roll back
```

### Scaling
```bash
kubectl scale deployment my-app --replicas=6
kubectl get pods                          # 6 pods
kubectl scale deployment my-app --replicas=2
kubectl get pods                          # back to 2
```

---

## My Questions & Clarifications

### Q: In Docker we run a container. In Kubernetes we run a Pod. Who has the IP — Pod or container?

**The POD has the IP address, not the container.**

```
        Pod  (IP: 10.244.0.5)
   ┌─────────────────────────────┐
   │   ┌─────────────────────┐   │
   │   │  Container           │   │  ← shares the Pod's IP
   │   │  (acme-api :8080)   │   │
   │   └─────────────────────┘   │
   └─────────────────────────────┘
```

- A Pod is a wrapper around one (or a few) containers.
- Containers inside a Pod share: the same IP, the same network (talk via `localhost`),
  and start/stop together.
- 99% of the time a Pod has ONE container. Multiple containers in a Pod = "sidecar"
  pattern (e.g. app container + log-shipper container that must live together).

| Docker | Kubernetes |
|--------|-----------|
| Container has an IP | **Pod** has the IP, container shares it |
| You run a container | You run a Pod (which runs the container) |

---

### Q: How is Kubernetes installed? What are cluster / nodes / remote machines?

Build it up from physical machines:

1. You have some servers (machines): A, B, C, D
2. You install Kubernetes software on each:
    - Control plane components (API Server, etcd, scheduler...) on one → **master node**
    - kubelet + kube-proxy on the rest → **worker nodes**
3. All machines together working as one system = a **CLUSTER**

```
┌──────────────────────────────────────────────┐
│                  CLUSTER                       │
│  Node A       Node B      Node C     Node D    │
│  (master)    (worker)    (worker)   (worker)   │
└──────────────────────────────────────────────┘
```

**Vocabulary (this confuses everyone):**
| Word | Meaning |
|------|---------|
| **Node** | Just one machine (physical or virtual) in the cluster |
| **Cluster** | All nodes together, working as one system |
| **Master / Control Plane** | The node that makes decisions (the boss) |
| **Worker node** | Nodes that actually run your Pods |

> **A cluster is made of nodes. A node is just a machine.**

**Office (CaaS):** you do NOT install Kubernetes yourself. The cloud team set up the
cluster already. You just get access and run `kubectl apply`. That's what "as a Service" means.

**Personal PC (minikube):** creates a tiny ONE-node cluster — your laptop is both master
and worker — so you can learn without renting servers.

---

### Q: How does a request reach my container? (request → Pod → container?)

Full journey of a user request:

```
User (https://api.acme.com)
   │
   ▼
1. GSLB / DNS        → "api.acme.com → jpe2b datacenter"
   │
   ▼
2. Load Balancer     → (LBaaS / LoadBalancer Service) spreads traffic
   │
   ▼
3. Service           → stable cluster address, "send to any healthy acme-api Pod"
   │  (load balances, picks a healthy Pod)
   ▼
4. Pod (10.244.0.5)  → receives traffic on its IP
   │
   ▼
5. Container :8080   → your acme-api answers
```

Yes — request goes to the Pod, then the Pod hands it to the container inside.
But it passes through a **Service** first.

**Why a Service in the middle? Why not connect directly to the Pod?**
Because **Pod IPs keep changing:**
- Pod crashes → new Pod → new IP
- Scale 3→6 Pods → 3 new IPs
- New deploy → all Pods replaced → all new IPs

The Service has a **stable IP + name that never changes.** It always tracks which Pods
are alive (by label, e.g. `app: acme-api`) and forwards to a healthy one.
Like a receptionist who always knows which staff are currently working.

```
Service (stable: acme-api-service)
   ├──► Pod 1 (10.244.0.5)   ← come and go,
   ├──► Pod 2 (10.244.0.6)      get new IPs constantly
   └──► Pod 3 (10.244.0.7)
```

---

### Q: What is GSLB? What is LBaaS? Is there a load balancer in front of Pods too?

**Load Balancer (plain words):** a traffic distributor. Sits in front of multiple
servers, spreads requests so none gets overwhelmed. Also does health checks — stops
sending traffic to dead servers.

```
              ┌─► Server 1
Requests ─► LB ┼─► Server 2
              └─► Server 3
```

**The confusion:** there are MULTIPLE load balancers at different "zoom levels":

```
GSLB          → picks the DATACENTER / REGION    (across regions)
LBaaS         → picks the WORKER MACHINE         (across nodes in 1 datacenter)
K8s Service   → picks the POD                    (across pods on those nodes)
```

#### GSLB (Global Server Load Balancer) — works at DNS level
- Decides **which datacenter/region** → jpe2b (East Japan) or jpw1a (West Japan)
- Why: app runs in 2 datacenters for safety. If jpe2b dies → shift to jpw1a.
  Also routes user to the geographically closer datacenter (lower latency).
- "Global" = makes a geographic / cross-datacenter decision.
- **Returns an IP** = the IP of the load balancer in the chosen datacenter.

#### LBaaS (Load Balancer as a Service) — inside one datacenter
- The load balancer INSIDE a datacenter that spreads traffic across worker machines/Pods.
- "as a Service" = cloud platform provides it for you, no hardware to manage —
  you request one and it appears.

#### Is there a load balancer in front of Pods? YES
- That's the **Kubernetes Service** (fed by the LBaaS).
- Not a duplicate — each LB handles a different zoom level:
    - GSLB: "which region?" → jpe2b
    - LBaaS: "which machine in jpe2b?" → Node 2
    - Service: "which Pod?" → 10.244.0.6

### Complete request journey (with IPs)

```
STEP 1 — User types api.acme.com
   Browser asks DNS: "IP for api.acme.com?"
        ▼
STEP 2 — GSLB (smart DNS) answers
   Checks: which datacenter is healthy + closest?
   Decision: jpe2b → returns IP of jpe2b's load balancer (e.g. 203.0.113.10)
        ▼
STEP 3 — Browser connects to 203.0.113.10 (LBaaS)
   LBaaS checks healthy worker nodes, picks Node 2, forwards into cluster
        ▼
STEP 4 — K8s Service (acme-api-service) receives it
   Knows all healthy Pods with label app=acme-api, picks Pod 10.244.0.6
        ▼
STEP 5 — Pod 10.244.0.6 receives it
   Forwards to the container inside on port 8080
        ▼
STEP 6 — acme-api container answers
   Java/Payara processes, response travels back up the same chain
```

| Step | Component | Decides | Returns/does |
|------|-----------|---------|--------------|
| 1 | DNS query | — | Browser asks "IP for api.acme.com?" |
| 2 | **GSLB** | Which **datacenter**? | Returns IP of that datacenter's LB |
| 3 | **LBaaS** | Which **machine**? | Forwards request into cluster |
| 4 | **K8s Service** | Which **Pod**? | Routes to a healthy Pod IP |
| 5 | **Pod** | — | Hands request to container inside |
| 6 | **Container** | — | App processes & responds |

**One-liners:**
- **GSLB** = smart DNS that picks the region/datacenter → returns an IP
- **LBaaS** = load balancer inside a datacenter that picks a machine → cloud-provided
- **K8s Service** = load balancer inside the cluster that picks a Pod → handles unstable Pod IPs

Zoom path: **planet → datacenter → machine → Pod → container**

---

### Q: Same code on multiple machines, multiple Pods per machine? Same region = same or different cluster?

**Part 1 — Same code on many machines, many Pods per machine? YES.**
```
Region jpe2b
  Worker Node 1      Worker Node 2     Worker Node 3
  ┌────────────┐    ┌────────────┐    ┌────────────┐
  │ Pod (acme)│    │ Pod (acme)│    │ Pod (acme)│
  │ Pod (acme)│    │ Pod (acme)│    │ Pod (acme)│
  └────────────┘    └────────────┘    └────────────┘
```
- Same image (`acme-api`) runs on multiple machines ✅
- Each machine can run multiple Pods of the same code ✅
- All identical copies (replicas). `replicas: 6` → Scheduler spreads them across nodes.
- Why multiple Pods per machine? A 32GB machine running one 512MB Pod = wasted. Pack several.

**Part 2 — IMPORTANT correction: request does NOT stay on the machine it landed on.**
```
LBaaS → Node 2 (just the entry door)
          │
          ▼
     K8s Service (load balances across ALL pods cluster-wide)
     ┌────────┼────────┐
     ▼        ▼        ▼
  Pod@Node1 Pod@Node2 Pod@Node3   ← ANY can be chosen
```
- The Service load-balances across ALL healthy Pods in the cluster, on ANY node.
- Pod network is "flat" — any Pod reachable from any node.
- The machine LBaaS picks is just the entry door; the Service picks the real Pod
  (which may live on a different machine).

**Part 3 — Same region = ONE cluster. Different region = different cluster.**
```
Region jpe2b → ONE cluster (Node1, Node2, Node3... all same cluster)
Region jpw1a → ANOTHER separate cluster (Node1, Node2... all same cluster)
```
- All nodes in one region = one cluster ✅
- Different region = separate, independent cluster ✅
- Why separate clusters per region? Isolation — if jpe2b cluster/datacenter fails,
  jpw1a is fully independent and keeps running. One cluster across regions = a
  network split could break everything.

Maps to repo deploy paths:
```
icp/qa/jpe2b/00_deploy.sh   → deploys same code to jpe2b cluster
icp/qa/jpw1a/00_deploy.sh   → deploys same code to jpw1a cluster
```

**Full corrected picture:**
```
              User
               │
          GSLB (DNS) ── picks region
          /          \
   jpe2b CLUSTER   jpw1a CLUSTER
   LBaaS           LBaaS
     │               │
   Service         Service
   Nodes+Pods      Nodes+Pods
   (same code deployed separately to each cluster)
```

---

## Section 8 — Inspection commands (SEE what's running where)

These answer real questions you'll ask daily on the job.

### "Which node is each Pod running on?"
```bash
kubectl get pods -o wide
# Adds NODE column showing which machine each Pod runs on.
# Also shows the Pod's IP. Example output:
# NAME            READY   STATUS    IP            NODE
# acme-api-abc   1/1     Running   10.244.0.5    worker-node-2
# acme-api-def   1/1     Running   10.244.1.7    worker-node-1
```

```bash
kubectl get pods -o wide -n qa        # for a specific namespace
kubectl get pods -A -o wide           # all namespaces, with nodes
```

### "What nodes (machines) do I have?"
```bash
kubectl get nodes                     # list nodes + status
kubectl get nodes -o wide             # + internal/external IPs, OS, k8s version
kubectl describe node <node-name>     # full detail: CPU/RAM capacity, pods on it, conditions
kubectl top node                      # live CPU/RAM usage per node (needs metrics-server)
```

### "Which pods are on a specific node?"
```bash
kubectl get pods -A -o wide --field-selector spec.nodeName=<node-name>
```

### "How much CPU/RAM is each Pod using?"
```bash
kubectl top pods                      # live usage per pod (needs metrics-server)
kubectl top pods -n qa
```

### "Why is my Pod not running / crashing?"
```bash
kubectl describe pod <pod-name>       # scroll to Events section at the bottom — the gold
kubectl logs <pod-name>               # app output
kubectl logs <pod-name> --previous    # logs from the crashed (previous) container
kubectl get events --sort-by=.lastTimestamp   # recent cluster events
```

### "What's the status of everything at a glance?"
```bash
kubectl get all                       # pods, services, deployments, replicasets in one view
kubectl get all -n qa                 # for a namespace
kubectl get pods --watch              # live-updating view (Ctrl+C to stop)
```

### "Which pods does this Service actually route to?"
```bash
kubectl get endpoints <service-name>  # lists the actual Pod IPs behind the Service
kubectl describe service <service-name>
# This proves the "Service → healthy Pods" mapping from the request-flow section.
```

### "What's my current cluster / context / namespace?"
```bash
kubectl config current-context        # which cluster am I pointing at (jpe2b? jpw1a?)
kubectl config get-contexts           # list all clusters I have access to
kubectl config use-context <name>     # switch to a different cluster
```
> IMPORTANT at work: always check `current-context` before running commands —
> so you don't accidentally run something against PROD instead of QA.

### Labels — how Pods/Services/Deployments link together
```bash
kubectl get pods --show-labels        # see labels on each pod
kubectl get pods -l app=acme-api     # filter pods by label
# A Service finds its Pods by matching labels (selector: app=acme-api).
# This is the "glue" that connects Service → Pods.
```

---

## Section 9 — Real Service YAML walkthrough (from office repo)

```yaml
apiVersion: v1
kind: Service
metadata:
  labels:
    app: "acme-api"
  name: "acme-api"
  annotations:
    wildic.cpd.acme-cloud.internal/enable: "false"
spec:
  ports:
    - name: http
      port: 80
      protocol: TCP
      targetPort: 8080
    - name: https
      port: 443
      protocol: TCP
      targetPort: 8081
  selector:
    app: "${BRANCH_SLUG}"
  type: ClusterIP
```

### Line by line

**`apiVersion: v1` + `kind: Service`**
- `kind` = what object this is (Service). `apiVersion` = which API rules.
- Core objects (Service/Pod/ConfigMap/Secret) = `v1`. Deployments = `apps/v1`.
- Every K8s YAML starts with these two lines.

**`metadata.name: "acme-api"`**
- The Service's name = its DNS name inside the cluster.
- Other Pods reach it by typing `acme-api` (full: `acme-api.<namespace>.svc.cluster.local`).

**`metadata.labels: app: acme-api`**
- Labels = tags to organize/find objects. Describes THIS Service object.
- `kubectl get services -l app=acme-api`
- ⚠️ Different from `selector` below! This labels the Service itself; selector picks Pods.

**`metadata.annotations: wildic.cpd.acme-cloud.internal/enable: "false"`**
- Annotations = config data that TOOLS read (K8s itself ignores them).
- This is a internal platform annotation.
- **Labels vs Annotations:**
    - Labels = for selecting/grouping (K8s matches on them)
    - Annotations = config data for tools/platforms to read

**`spec.ports:` — the port mappings (like Docker `-p HOST:CONTAINER`)**
| Field | Meaning |
|-------|---------|
| `name` | Human label for the port (http/https) |
| `port` | Port the **Service** listens on (what callers connect TO) — like Docker HOST side |
| `targetPort` | Port the **container** listens on (forwarded to) — like Docker CONTAINER side |
| `protocol` | TCP (vs UDP) |

```
Caller → Service:80  → forwarded → container:8080  (HTTP)
Caller → Service:443 → forwarded → container:8081  (HTTPS)
```
- 80 = standard HTTP port, 443 = standard HTTPS port (what browsers use by default)
- 8080/8081 = the actual Payara container ports (from the Dockerfile)
- Service maps standard web ports → real Payara ports.

**`spec.selector: app: "${BRANCH_SLUG}"` — the GLUE**
- Tells the Service: "route to all Pods with label `app: <value>`."
- Service watches for matching Pods and load-balances across them.
- This is the "Service → healthy Pods" mapping — pure label matching.

```
selector: app=feature-new-login
   matches ▼
  Pod (app=feature-new-login)  ✅ gets traffic
  Pod (app=feature-new-login)  ✅ gets traffic
  Pod (app=something-else)     ❌ ignored
```

**`${BRANCH_SLUG}` — why a variable?**
- Placeholder replaced at deploy time (like Dockerfile/`.gitlab-ci.yml` vars).
- `BRANCH_SLUG` = slugified Git branch name (special chars replaced).
- **Per-branch review-app pattern:**
    1. Push branch `feature/new-login`
    2. CI sets `BRANCH_SLUG=feature-new-login`
    3. Substitutes into YAML → `selector: app: feature-new-login`
    4. Deploys Pods labeled `app: feature-new-login`
    5. Service routes only to THAT branch's Pods
- Each branch gets its own isolated Service+Pods for testing.
- Connects to `.gitlab-ci.yml` stages `delete-vs` / `rollback-vs` (vs = virtual/per-branch env).

**`spec.type: ClusterIP`**
- ClusterIP = reachable INSIDE the cluster only (not directly from internet).
- External traffic reaches it via: GSLB → LBaaS → Ingress → THIS ClusterIP Service → Pods.
- ClusterIP is the internal final hop; external exposure happens at a higher layer.

### Full picture
```
External traffic (via Ingress/LBaaS, defined elsewhere)
        │
        ▼
Service "acme-api" (ClusterIP)
  port 80  → targetPort 8080 (HTTP)
  port 443 → targetPort 8081 (HTTPS)
  selector: app=${BRANCH_SLUG}
        │ routes to matching Pods
   ┌────┼────┐
   ▼    ▼    ▼
  Pod  Pod  Pod   (label app=${BRANCH_SLUG}, listening on 8080/8081)
```

---

## Section 10 — Ingress explained

### The problem it solves
ClusterIP Services are reachable only inside the cluster. Giving every Service its own
cloud LoadBalancer = 20 services → 20 load balancers → 20 IPs → wasteful & costly.

### What Ingress is
A single smart **HTTP router** at the edge of the cluster. ONE entry point routes many
services by **hostname** or **URL path**.

```
              ONE entry point
                    │
              ┌──────────────┐
              │   INGRESS     │
              │ api.acme.com   → acme-api Service   │
              │ admin.acme.com → acme-admin Service │
              │ acme.com/pay   → payments Service    │
              └──────────────┘
                 │      │       │
               Service Service Service → Pods
```
Like a building receptionist: everyone enters one door, gets directed to the right office.

### Two routing styles
**Host-based** (by domain):
```
host: api.acme.com   → acme-api Service
host: admin.acme.com → acme-admin Service
```
**Path-based** (by URL path):
```
host: acme.com
  /api      → acme-api Service
  /admin    → acme-admin Service
```

### Example Ingress YAML
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: acme-ingress
spec:
  rules:
    - host: api.acme.com           # visit this hostname...
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: acme-api      # ...route to THIS Service
                port:
                  number: 80         # the Service's port (matches Service YAML port: 80)
  tls:
    - hosts:
        - api.acme.com
      secretName: acme-tls-cert     # HTTPS cert stored in a K8s Secret
```
Locks together with the Service file: `backend.service.name/port` → Service `port: 80`
→ `targetPort: 8080` → Pod → container.

### IMPORTANT: Ingress object vs Ingress Controller
- **Ingress object** (the YAML) = just rules on paper. Does NOTHING by itself.
- **Ingress Controller** = an actual running Pod (NGINX/Traefik/HAProxy) that reads the
  rules and does the real routing.
```
Ingress YAML (instruction manual) → read by → Ingress Controller (the worker that routes)
```

### Where Ingress fits in the FULL flow
```
User (api.acme.com)
   ▼
GSLB (DNS)          → picks region (jpe2b)
   ▼
LBaaS               → enters the cluster via a worker machine
   ▼
INGRESS             → reads hostname, routes to acme-api Service  ◄── HTTP-aware router
   ▼
Service (ClusterIP) → picks a healthy Pod (selector match)
   ▼
Pod (8080)          → hands to container
   ▼
Container           → acme-api answers
```

### Comparison
| Thing | Level | Job |
|-------|-------|-----|
| GSLB | Global/DNS | Pick region/datacenter |
| LBaaS | Datacenter | Get traffic into the cluster |
| **Ingress** | Cluster edge | Route by hostname/path to right Service (HTTP smarts + HTTPS certs) |
| Service | Inside cluster | Pick a healthy Pod |
| Pod | — | Run the container |

Ingress = the HTTP-aware router (understands hostnames, paths, TLS certs). Lower LBs just
move raw traffic; Ingress reads the actual web request and routes smartly.

---

## Section 11 — Does acme-api use Ingress? (repo investigation)

**Answer: NO — no Kubernetes `Ingress` object is used.**

Searched the whole repo. Object kinds found:
| Kind | Count |
|------|-------|
| NetworkPolicy | 41 (firewall rules — which Pods can talk to which) |
| Service | 20 |
| Deployment | 7 |
| **Ingress** | **0** |

"ingress" only appears as the label value `shared-ingressgateway` — a reference to
the company's platform gateway, not a K8s Ingress object.

### What's used instead: a `LoadBalancer` Service ("DLB")
File: `icp/<env>/common/caas/acme-dlb.yaml`  (dlb = Distributed Load Balancer)
```yaml
kind: Service
metadata:
  name: "acme-api-dev-${CLUSTER_PREFIX}-dlb"
  labels:
    network.cpd.acme-cloud.internal/dlb-binding: shared-ingressgateway   # binds to the company's gateway
  annotations:
    service.beta.kubernetes.io/dlb-nw-type: private
    service.beta.kubernetes.io/dlb-is-global-server-group: "true"  # ← this is the GSLB hook
spec:
  ports:
    - port: 21580
      targetPort: 8080        # → Payara container
  selector:
    app: "${BRANCH_SLUG}"     # per-branch routing
  type: LoadBalancer          # NOT Ingress
```

### How external traffic is actually exposed (real setup, no Ingress)
```
User
  ▼
GSLB ◄──── annotation: dlb-is-global-server-group: "true"
  ▼
the company's DLB / shared-ingressgateway ◄── label: dlb-binding: shared-ingressgateway
  ▼
LoadBalancer Service (acme-api-...-dlb)  port 21580 → targetPort 8080
  ▼
Pod (selector: app=${BRANCH_SLUG})
  ▼
Container (Payara :8080)
```

### Two Services work together
| File | Type | Role |
|------|------|------|
| `acme-api` Service | ClusterIP | Internal — in-cluster traffic |
| `acme-api-...-dlb` Service | LoadBalancer | External — exposes via the company's gateway |

The `-dlb` LoadBalancer Service is the **"Ingress replacement."** The company's platform
(shared-ingressgateway + DLB + GSLB) does what a vanilla K8s Ingress + Ingress Controller
would do.

### Why no Ingress?
Company has its own managed networking platform ("CPD" = Cloud Platform Dept, from
`cpd.acme-cloud.internal`). Instead of deploying your own NGINX Ingress Controller, you bind to
the company's shared gateway via annotations on a LoadBalancer Service. Same concept as Ingress,
different implementation — managed by the platform team.

> Takeaway: Ingress is the vanilla-Kubernetes way. Managed platforms (cloud providers,
> the company's internal platform (CPD)) often expose apps via annotated LoadBalancer Services instead. Learn both —
> the CONCEPT (route external traffic to internal Services) is identical.

---

## Chapter 2 Summary

| Concept | One line |
|---------|---------|
| Why K8s | Manual container management at scale is impossible — K8s automates it |
| Desired state | You declare what you want, K8s makes it happen and keeps it that way |
| Control Plane | Brain: API Server (front door), etcd (memory), Scheduler (placement), Controller (watchdog) |
| Worker Node | kubelet (runs containers), kube-proxy (networking) |
| Pod | Smallest unit — wrapper around container(s) |
| Deployment | Manages pods: replicas, rolling updates, rollback |
| Service | Stable network endpoint for pods (ClusterIP / NodePort / LoadBalancer) |
| Namespace | Logical isolation — dev/qa/stg/prod on same cluster |
| ConfigMap | Non-secret config outside the image |
| Secret | Sensitive config (passwords, tokens) |
| Ingress | HTTP router — hostname/path → Service |
| kubectl | CLI for K8s (same role as `docker` CLI for Docker) |