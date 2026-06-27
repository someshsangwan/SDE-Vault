# DevOps Learning Course — Index

A focused, hands-on DevOps course for someone who already works in IT (~2 yrs) and uses
Docker, but wants to understand **how things actually work under the hood** — from the
*history and "why"* of each tool, to docs-level concepts, to practical labs on the local machine.

Each chapter follows the same rhythm:
**History / Why it exists → How it works → Core concepts (docs-level) → Hands-on practical → Notes.**

> 🔗 **Real-world reference repo:** `/Users/somesh.sangwan/Desktop/rcash_api-roc`
> This is a production Java/Payara backend deployed on Rakuten "One Cloud". It already contains a
> `Dockerfile`, a GitLab CI pipeline (`.gitlab-ci.yml`), Kubernetes (CaaS), load balancers (LBaaS),
> GSLB, DBaaS, and New Relic monitoring — so we connect every chapter to code you actually work with.

---

## Course Structure (5 Chapters)

### Chapter 1 — Containers with Docker
**The foundation: package once, run anywhere.**
- How deployment worked *before* containers: bare metal → VMs → "works on my machine" hell
- Why Docker appeared, VMs vs containers, and how containers really work (namespaces, cgroups, union filesystems)
- Images vs containers, Dockerfile, layers & build cache, volumes, port mapping
- Docker networking, multi-container apps with `docker-compose`, registries (Docker Hub / Harbor)
- **Hands-on:** install & run Docker locally, build an image from scratch, run/inspect/debug containers
- 🔗 *Connect to work:* read & understand the real `rcash_api-roc/Dockerfile` (Payara base image, New Relic, custom logging, WAR deploy)

### Chapter 2 — Orchestration with Kubernetes
**Running containers at scale, the right way.**
- The problem with running containers manually → what "orchestration" solves
- Kubernetes architecture deep-dive: control plane, API server, etcd, scheduler, controller-manager, kubelet, kube-proxy
- Core objects: Pods, ReplicaSets, Deployments, Services, Namespaces
- Config & runtime: ConfigMaps, Secrets, Ingress, volumes, scaling, health checks, rolling updates
- **Hands-on:** run a local cluster (minikube/kind), deploy an app with `kubectl`, scale & update it
- 🔗 *Connect to work:* the repo's **CaaS (Kubernetes clusters)** + `kubectl`-based deploy scripts under `roc/`

### Chapter 3 — CI/CD Pipelines
**Automating build → test → deploy.**
- What CI and CD mean, why they exist, the cost of manual deployments
- Pipeline anatomy: stages, jobs, runners, artifacts, environments, rollbacks
- GitLab CI in depth (since that's what your team uses), with GitHub Actions for comparison
- **Hands-on:** write a pipeline that builds a Docker image, runs tests, and deploys
- 🔗 *Connect to work:* dissect the real `.gitlab-ci.yml` — stages `validate-reservation → fetch → build → docker → artifact-scan → deploy → rollback-vs`

### Chapter 4 — Cloud & Infrastructure as Code
**Provisioning infrastructure in code, not by clicking.**
- Cloud fundamentals: compute, storage, networking, IAM; managed Kubernetes (EKS/AKS/GKE)
- Why Infrastructure as Code, and how Terraform works (providers, state, plan/apply, modules)
- Load balancers, DNS-based routing, and managed databases — the cloud building blocks
- **Hands-on:** provision real infrastructure with Terraform from scratch
- 🔗 *Connect to work:* map "One Cloud" pieces — **LBaaS** (load balancers), **GSLB** (DNS), **DBaaS** (MariaDB) — to IaC concepts

### Chapter 5 — Monitoring, Logging & Capstone
**Knowing what's happening in production + tying it all together.**
- The observability mindset: metrics, logs, traces, alerting
- Prometheus + Grafana for metrics; centralized logging; APM tools
- **Capstone project:** take an app → Dockerize → CI/CD pipeline → deploy to Kubernetes on cloud → monitored
- 🔗 *Connect to work:* the **New Relic** agent baked into the repo's Dockerfile + `custom_logging.properties`

---

## How we'll work
- We go **one chapter at a time, in the terminal**. Say *"teach me Chapter 1"* and we begin.
- As we go, I'll create **notes files** in this `devops/` directory (e.g. `chapter-01-docker.md`)
  so you build a personal reference you can revisit.
- We continually tie concepts back to the `rcash_api-roc` repo so theory sticks to real work.

## Progress Tracker
- [x] Chapter 1 — Containers with Docker
- [x] Chapter 2 — Orchestration with Kubernetes
- [x] Chapter 3 — CI/CD Pipelines
- [x] Chapter 4 — Cloud & Infrastructure as Code
- [x] Chapter 5 — Monitoring, Logging & Capstone