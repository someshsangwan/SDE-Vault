# Chapter 1 — Containers with Docker (My Notes)

> Reference repo: `/Users/youruser/Desktop/acme-api-infra` (Payara/Java backend, real Dockerfile)
> My environment: macOS (Apple Silicon, arm64), Docker 28.1.1, daemon running.

---

## Section 1 — How deployment worked BEFORE Docker (the "why")

### Era 1: Bare metal (one app per physical server)
- App installed directly on the OS of a physical machine.
- Problems: wasteful (low utilization), slow to provision, fragile dependency conflicts
  (App A needs Python 3.8, App B needs 3.10 → clash on the same OS).

### Era 2: Virtual Machines (VMs)
- A **hypervisor** (VMware/VirtualBox/KVM) splits one server into many VMs.
- Each VM runs a **full guest OS**.
- Win: strong isolation between apps.
- Pain: each VM is GBs in size, boots in minutes, heavy RAM/CPU overhead → only ~dozens per host.

### Era 3: "Works on my machine" problem
- App works on dev laptop but breaks in prod: different OS/lib versions, missing deps, wrong env/timezone.
- This is THE problem Docker was created to solve.

### Docker (2013) — the idea
- Package the app + everything it needs (runtime, libraries, tools, config) into one portable **image**.
- The image runs **identically** on laptop, CI, and prod.
- Containers **share the host OS kernel** (no full guest OS) → MBs not GBs, start in milliseconds.

### Mental model
| | Virtual Machine | Container |
|---|---|---|
| Contains | Full guest OS + app | Just app + its dependencies |
| Size | Gigabytes | Megabytes |
| Startup | Seconds–minutes | Milliseconds |
| Isolation | Strong (own kernel) | Process-level (shares host kernel) |
| Density per host | Dozens | Hundreds–thousands |

---

## Section 2 — How containers actually work (the internals)

Docker uses 3 Linux kernel features — it didn't invent these, just packaged them nicely.

### 1. Namespaces — isolation
Wraps a resource so a process thinks it's the only one using it.

| Namespace | Isolates |
|-----------|---------|
| `pid` | Process IDs — container sees only its own processes, PID 1 = your app |
| `net` | Network — each container gets its own IP/interfaces |
| `mnt` | Filesystem — container sees its own root (`/`) |
| `uts` | Hostname |
| `ipc` | Shared memory / semaphores |
| `user` | User IDs — container root ≠ host root |

> When you ran `docker run -it alpine sh` and saw a different filesystem with `ls /` —
> that was the `mnt` namespace at work.

### 2. cgroups (control groups) — resource limits
Limits how much CPU/RAM/disk a container can consume.
```
docker run --memory="256m" --cpus="0.5" my-app
```
Without cgroups, one container could starve the host. Kubernetes `requests/limits` on Pods are
built directly on top of cgroups (Chapter 2 connects this).

### 3. Union filesystem (layers)
Images are a **stack of read-only layers**. A container adds one thin writable layer on top.
```
┌──────────────────────────┐  ← Writable layer (your running container, discarded on stop)
├──────────────────────────┤  ← COPY app code
├──────────────────────────┤  ← RUN install dependencies
├──────────────────────────┤  ← RUN apt-get / apk add
├──────────────────────────┤  ← Base image (alpine / payara / python…)
└──────────────────────────┘
```
- **Cache:** unchanged layers are reused on rebuild — makes rebuilds fast.
- **Shared:** 10 containers from the same base image share that base layer on disk (it's stored once).
- Check layers of an image: `docker history <image-name>`

---

## Section 3 — Docker's client/server architecture

```
docker CLI  ──(REST API)──►  dockerd (daemon)  ──►  Registry (Docker Hub / Harbor)
(your terminal)               (does the work)         (where images live)
```

- **CLI** — just a client, talks to the daemon over a socket.
- **Daemon (`dockerd`)** — manages images, containers, networks, volumes.
- **Registry** — storage for images. Docker Hub = public. Your office uses **Harbor**:
  `registry-jpe2.acme-registry.internal` (seen on line 1 of `acme-api-infra/Dockerfile`).

---

## Section 4 — Image vs Container

| | Image | Container |
|---|---|---|
| What | Read-only template | Running instance of an image |
| Analogy | Class (OOP) | Object / instance |
| Lives | On disk | In memory + thin r/w layer |
| Command | `docker build` | `docker run` |

```bash
docker images       # list images (templates on disk)
docker ps           # list running containers
docker ps -a        # all containers including stopped
```

---

## Section 5 — Writing a Dockerfile

Each instruction that changes the filesystem creates a **layer**. Order matters: put
things that change least (base image, deps) at the TOP, things that change most (your code)
at the BOTTOM — so the cache is preserved on most rebuilds.

```dockerfile
FROM python:3.11-alpine    # Layer 1 — base image
WORKDIR /app               # Layer 2 — set working dir
COPY app.py .              # Layer 3 — copy code (changes often → near bottom)
EXPOSE 8080                # metadata only, no layer
CMD ["python", "app.py"]   # default command on container start, no layer
```

```bash
docker build -t my-first-app:v1 .          # build image, tag it
docker run -d -p 8080:8080 --name my-app my-first-app:v1
# -d          = detached (background)
# -p 8080:8080 = host-port:container-port
# --name      = human-readable container name

curl http://localhost:8080                 # test it
docker logs my-app                         # see stdout from container
docker exec -it my-app sh                  # shell into running container
docker stop my-app && docker rm my-app     # stop and remove
```

---

---

## My Questions & Clarifications

### Q: What does `-p 8080:8080` mean?

A container is completely isolated — like a house with all doors locked.
Your app runs inside on its own port, but nobody outside can reach it.

`-p HOST:CONTAINER` cuts a hole in the wall:
> "Knock on my Mac's port 8080 → forward it into the container's port 8080."

```
Your browser → Mac port 8080 → (forwarded) → Container port 8080 → your app answers
```

- **Left side** = your Mac's port
- **Right side** = container's port
- They don't have to be the same number:
    - `-p 8080:8080` → same (most common)
    - `-p 9000:8080` → different (useful if 8080 is already taken on your Mac)
- Only rule: the **host port (left) must be free** on your Mac.

---

### Q: Do the ports HAVE to be different?

No. Same or different — your choice. You only change the left side if that port is already
in use on your Mac by another process.

---

### Q: What about IP addresses — Mac vs container?

Every container gets its **own private IP** assigned by Docker (e.g. `172.17.0.2`).
Your Mac has its own IP (e.g. `192.168.1.10` on WiFi, or `localhost`).

```
┌─────────────────────────────────────────────────┐
│  Your Mac  (192.168.1.10)                        │
│  ┌───────────────────────────────────────────┐  │
│  │  Docker Engine                             │  │
│  │  ┌─────────────────┐ ┌─────────────────┐  │  │
│  │  │  Container A     │ │  Container B│  │  │
│  │  │  IP: 172.17.0.2  │ │  IP: 172.17.0.3  │  │  │
│  │  └─────────────────┘ └─────────────────┘  │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

- Container IPs (`172.17.x.x`) are **private to Docker** on your machine.
- Without `-p`, the container is **invisible** to the outside world.
- With `-p 8080:8080`, Docker forwards: `your Mac IP:8080` → `172.17.0.2:8080`

```bash
# See your container's private IP
docker inspect my-app --format '{{.NetworkSettings.IPAddress}}'

# Both of these reach the same app:
curl http://172.17.0.2:8080   # direct container IP (only works from your Mac)
curl http://localhost:8080    # via port mapping (works from anywhere on your network)
```

| | Address | Reachable from |
|---|---|---|
| Your Mac | `192.168.1.x` or `localhost` | Anyone on your network |
| Container | `172.17.x.x` (Docker private) | Only from your Mac |
| `-p` mapping | Bridges the two | Makes container reachable from outside |

---

---

## Section 6 — Volumes (how data survives when a container dies)

### The problem
The container's writable layer is **destroyed when the container stops or is deleted.**
Any files written inside the container (logs, DB data, uploads) are gone forever.

### The solution: Volumes
A volume is a folder that lives **outside the container on the host (your Mac).**
Mount it into the container → container reads/writes it → container dies → data stays safe.

```
Your Mac (real disk)           Container
  ~/mydata/  ◄──── mount ────► /app/data/
  (permanent)                  (inside container)

Container dies → data stays on Mac
New container + same mount → data is back
```

### Two types

**Bind mount** — you specify the exact path on your Mac:
```bash
docker run -v /your/mac/path:/container/path image-name
```

**Named volume** — Docker manages the location:
```bash
docker volume create mydata
docker run -v mydata:/container/path image-name
```

### Key commands
```bash
docker volume create mydata              # create a named volume
docker volume ls                         # list all volumes
docker volume inspect mydata            # see where Docker stores it
docker volume rm mydata                  # delete volume (data gone!)

# write data in container A
docker run -it --rm -v mydata:/data alpine sh
# → echo "survives" > /data/file.txt && exit

# read it back in a brand new container B
docker run -it --rm -v mydata:/data alpine sh
# → cat /data/file.txt   ← still there!
```

### Connects to office work
`acme-api-infra/Dockerfile` creates log dirs (`${ACME_LOGS_DIR}/acme_api`).
In production those dirs are mounted as volumes so:
- Logs survive container restarts / redeployments
- Log collectors (New Relic, ELK) can read them from the host

---

---

## Section 7 — Docker Networking (how containers talk to each other)

### The problem
Containers are isolated by default. Two containers cannot reach each other by name
unless they are on the same Docker network.

### The solution — Docker Networks
Create a named network → attach containers to it → they find each other **by container name**.
Docker handles DNS automatically inside the network. No hardcoded IPs needed.

```
Docker Network: "my-network"
  ┌──────────────┐      ┌──────────────┐
  │   app-a       │◄────►│   app-b       │
  │  172.18.0.2  │      │  172.18.0.3  │
  └──────────────┘      └──────────────┘
  app-b can reach app-a just by name: ping app-a
```

### Hands-on
```bash
docker network create my-network
docker run -d --name app-a --network my-network alpine sleep 3600
docker run -d --name app-b --network my-network alpine sleep 3600
docker exec app-b ping app-a -c 3    # works — Docker resolves the name
docker rm -f app-a app-b && docker network rm my-network
```

### Real world pattern — API + Database
```
Docker Network: "acme-network"
  ┌──────────────────┐       ┌──────────────┐
  │   acme-api       │──────►│   mariadb     │
  │   port 8080       │       │   port 3306   │
  └──────────────────┘       └──────────────┘
          │
          │ only API is exposed outside via -p 8080:8080
          ▼
     your Mac / users
```
- API talks to DB using just the name `mariadb` — not an IP.
- DB port 3306 is NOT exposed outside — only reachable within the network (secure).

### Network drivers
| Driver | What it does | When |
|--------|-------------|------|
| `bridge` | Private network, containers talk by name | Default for local setups |
| `host` | Container shares host network, no isolation | Rarely used, Linux only |
| `none` | No network at all | Security-sensitive containers |

### Key commands
```bash
docker network create my-network           # create
docker network ls                          # list all
docker network inspect my-network         # see connected containers + IPs
docker network connect my-network my-app  # add running container to network
docker network rm my-network              # delete
```

### Connects to office work
- `docker compose up -d` (from README) auto-creates a shared network for all services.
  That's how `acme-api` reaches `mariadb` — same compose file = same network = name-based DNS.
- In Kubernetes (Chapter 2), the same concept is called a **Service** — same idea, different name.

---

---

## Section 8 — docker-compose (run a whole app with one command)

### The problem
Running multi-container apps manually means remembering many `docker run` flags,
creating networks, volumes — every developer must do it perfectly every time.

### The solution
Describe the entire app in one `docker-compose.yml` file. One command starts everything.

```yaml
services:

  mariadb:
    image: mariadb:10.6
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: acme
    volumes:
      - db-data:/var/lib/mysql      # persistent data

  acme-api:
    image: acme-api:latest
    ports:
      - "8080:8080"                 # same as -p
    depends_on:
      - mariadb                     # start db first

volumes:
  db-data:
```

What docker compose does automatically:
- Creates a **shared network** — all services find each other by service name
- Creates declared **volumes**
- Starts containers in the right order (`depends_on`)

### Key commands
```bash
docker compose up -d          # start all services in background
docker compose down           # stop + remove containers + network
docker compose down -v        # also remove volumes (data gone!)
docker compose ps             # list running services
docker compose logs           # logs from all services
docker compose logs api       # logs from one service
docker compose build          # rebuild images
docker compose restart api    # restart one service
docker compose exec api sh    # shell into a running service
```

### build vs image in compose
```yaml
api:
  build: .          # build from local Dockerfile (dev)
  image: my-api     # use a pre-built image (prod/CI)
```

### Connects to office work
- `docker compose up -d` in `acme-api-infra` README starts API + MariaDB + all services
  with one command — identical environment for every developer.
- CI pipeline (`.gitlab-ci.yml`) runs on `docker` tagged runners — services like DB
  are spun up as compose services during test stages.

---

---

## Section 9 — acme-api-infra Dockerfile deep-dive (real production code)

> File: `/Users/youruser/Desktop/acme-api-infra/Dockerfile`

### Layer-by-layer breakdown

**Block 1 — Base image**
```dockerfile
FROM registry-jpe2.acme-registry.internal/acme-dev-qa/payara/micro:5.2022.3-jdk11
```
- Pulls from your office **Harbor private registry** (not Docker Hub)
- Payara Micro = lightweight Java app server that runs `.war` files
- JDK 11 bundled — this is why the container knows how to run Java

**Block 2 — Build-time variables**
```dockerfile
ARG module="AcmeAPI-BusinessLogic-1.0-SNAPSHOT"
ARG service=acme-api
```
- `ARG` = build-time parameters (like function arguments for the Dockerfile)
- Override at build time: `docker build --build-arg service=acme-api-v2 .`

**Block 3 — System packages + timezone**
```dockerfile
RUN apk add --no-cache tzdata
RUN apk add --no-cache wget
RUN apk add --no-cache curl
ENV TZ Asia/Tokyo
```
- `apk` = Alpine Linux package manager
- 3 separate RUN = 3 separate layers (not ideal — could be 1 RUN with all 3 packages)
- `ENV` sets environment variable visible to all processes in the container

**Block 4 — App environment variables**
```dockerfile
ENV ACME_SERVICE_NAME=${service}         # = "acme-api"
ENV ACME_LOGS_DIR="${HOME_DIR}/logs"
ENV MEM_MAX_RAM_PERCENTAGE=50             # JVM uses max 50% of container memory limit
```
- `MEM_MAX_RAM_PERCENTAGE=50` works with Kubernetes memory limits (Chapter 2) to
  prevent JVM from eating all RAM.

**Block 5 — User switch + directory creation**
```dockerfile
USER payara
RUN mkdir ${HOME_DIR}/setup && mkdir ${ACME_LOGS_DIR}/acme_api ...
```
- Switch from `root` to `payara` — security best practice (least privilege)
- Creates log and config directories. These are mounted as volumes in production
  so logs survive container restarts.

**Block 6 — New Relic agent download**
```dockerfile
RUN wget .../newrelic-java-8.2.0.zip && unzip ...
```
- New Relic APM agent baked into the image at build time
- Attaches to JVM, sends metrics/traces to New Relic dashboard (Chapter 5)
- This is a heavy layer — contributes significantly to the 743MB image size

**Block 7 — Copy build artifacts**
```dockerfile
COPY --chown=payara:payara ./icp/build/docker ${HOME_DIR}/setup
```
- Copies compiled configs and scripts from your Mac (or CI runner) into the image
- `--chown=payara:payara` — files owned by payara user, not root
- Requires `./90_build.sh` to run first (builds the Java code)

**Block 8 — Payara launcher pre-warm**
```dockerfile
RUN java -jar payara-micro.jar --outputlauncher ...
```
- Runs Payara once at BUILD TIME to generate an optimised launcher JAR
- Slow operation done once → fast startup on every container run
- Rule: push slow work into build time, keep runtime fast

**Block 9 — Logging config**
```dockerfile
RUN sed -i "s/\${ACME_LOGS_DIR}/.../" custom_logging.properties ...
```
- Replaces placeholder variables in config files with real paths/names
- Done at build time (paths are known) → faster container startup

**Block 10 — Deploy the WAR (YOUR CODE)**
```dockerfile
COPY --chown=payara:payara acme_api/.../target/${module}.war ${DEPLOY_DIR}/${service}.war
```
- This is your actual Java application entering the image
- `.war` dropped into Payara's deploy dir → auto-deployed on startup
- **Placed near the bottom** — code changes every commit, so all expensive
  layers above stay cached. Only this layer rebuilds on each code change.

**Block 11 — Runtime config**
```dockerfile
CMD []
USER 1000
```
- Empty CMD — actual startup is Payara's entrypoint.sh (set in base image)
- `USER 1000` — numeric user ID, more portable in Kubernetes than named users

### Full layer order + cache strategy
```
Layer 1   FROM payara/micro        ← base (never changes)
Layer 2-5 packages + ENV           ← rarely changes → cached
Layer 6   New Relic download        ← heavy, rarely changes → cached
Layer 7-9 COPY configs + pre-warm  ← changes on Payara upgrades → cached
Layer 10  logging config            ← changes on config updates → cached
Layer 11  COPY acme_api.war       ← YOUR CODE, changes every commit → bottom
Layer 12  CMD + USER               ← never changes
```
Only Layer 11 rebuilds on every commit. Layers 1-10 are cache hits = fast CI builds.

---

## Chapter 1 Complete — Summary

| Concept | One line |
|---------|---------|
| Why Docker | Solve "works on my machine" — package app + deps into a portable image |
| Namespaces | Kernel feature that gives each container isolated PID/net/filesystem |
| cgroups | Kernel feature that limits CPU/RAM a container can use |
| Union filesystem | Images = stacked read-only layers + one writable layer on top |
| Image vs Container | Image = template (class). Container = running instance (object) |
| Dockerfile | Recipe to build an image layer by layer |
| Layer cache | Unchanged layers are reused — put stable things top, code at bottom |
| `-p HOST:CONTAINER` | Forward traffic from Mac port into container port |
| Volumes | Data that lives outside the container — survives restarts |
| Networks | Containers on the same network find each other by name |
| docker-compose | One YAML file + one command to run a whole multi-container app |