# Chapter 3 — CI/CD Pipelines (My Notes)

> Reference repo: `/Users/somesh.sangwan/Desktop/rcash_api-roc`
> Real file taught: `roc/gitlab/.gitlab-rcash-dev.yaml` + `.gitlab-api-validation.yaml`

---

## Section 1 — What CI/CD is & why it exists

### Life before CI/CD (manual deploy pain)
For each release someone manually: pull code → mvn build → build Docker image → push to
Harbor → kubectl apply → repeat for the 2nd datacenter → hope no step was missed.

Problems: human error, "works on my machine", no record of who/what/when, slow & serial,
scary high-stakes deploys → people deploy rarely → big risky releases.

### CI vs CD
| Term | Full name | Meaning |
|------|-----------|---------|
| CI | Continuous **Integration** | Every push is auto **built + tested** |
| CD | Continuous **Delivery/Deployment** | Tested code auto **deployed** to environments |

Goal: automate everything from "code pushed" → "running in prod". Fast, repeatable, error-free.

```
Push code → [Fetch → Build → Test → Package → Scan → Deploy] → running in prod
            (all automated, no human runs a command)
```

---

## Section 2 — Pipeline anatomy (vocabulary)

```
PIPELINE (whole process for one push)
├── STAGE 1: fetch    ← stages run IN ORDER (sequential)
│     └── job
├── STAGE 2: build    ← next stage starts only if previous succeeds
│     └── job
├── STAGE 3: docker   ← one stage can have MULTIPLE jobs in PARALLEL
│     ├── job: jpe2b  ┐ run at
│     └── job: jpw1a  ┘ same time
└── STAGE 4: deploy
      ├── job: jpe2b
      └── job: jpw1a
```

| Term | Meaning |
|------|---------|
| Pipeline | Entire automated workflow triggered by a push |
| Stage | A phase. Stages run **in order**. |
| Job | A task in a stage. Jobs in same stage run **in parallel**. |
| Runner | The machine/container that executes a job |
| Artifact | File produced by a job, passed to later jobs (e.g. the .war) |
| Image | Each job runs inside a Docker container — you pick the image |

> KEY: every CI job runs INSIDE a Docker container. The whole pipeline is built on
> containers — that's why Docker (Ch.1) came first. Each job picks an image with the
> right tools (Maven to build, Kaniko to build images, kubectl to deploy).

**Cache vs Artifact:**
- Cache = speed up FUTURE runs (reuse downloaded deps like .m2/repository)
- Artifact = pass files to the NEXT stage in THIS run (the .war)

---

## Section 3 — Real dev pipeline line-by-line (.gitlab-rcash-dev.yaml)

### Reusable rules template
```yaml
.rcash:dev:rules:                          # leading DOT = hidden template job (reused via extends)
  rules:
    - if: '$CI_COMMIT_BRANCH =~ /^(dev)\/.*$/'  # only runs on branches like dev/xxx (=~ is regex)
      when: manual                              # won't auto-run — someone clicks "play" (safety gate)
  variables:
    VERSION: $CI_COMMIT_SHORT_SHA            # built-in var = short git commit hash
    TENANT_NAMESPACE: rcash-dev-dev
```

### STAGE: fetch — get source code
```yaml
rcash:fetch:dev:
  extends: .rcash:dev:rules                 # inherit the template (manual, dev-branch-only)
  stage: fetch
  image: gitlab/gitlab-ee:latest            # container this job runs in
  before_script:                            # setup: write SSH key to clone private repo
    - cat $ID_RCASH_API_KEY > ~/id_rcash_api_key   # $ID_RCASH_API_KEY = GitLab CI secret
    - chmod 600 ~/id_rcash_api_key          # SSH requires private key perms
  script:
    - sh roc/build/90_build_ci.sh update_submodule   # fetch source (git submodule)
  artifacts:
    paths: [ rcash_api/ ]                   # save folder → next stage can use it
    expire_in: 1 days                       # auto-delete after a day
```
> This automates the SSH-key + submodule steps the README described doing by hand.

### STAGE: build — compile the .war
```yaml
rcash:build:dev:
  stage: build
  image: maven:3.6.3-jdk-11                 # Maven + Java 11 image (tools to compile Java)
  cache:
    key: ${CI_COMMIT_REF_SLUG}
    paths: [ .m2/repository ]               # cache Maven deps for speed
  script:
    - sh roc/build/90_build_ci.sh build_war # compile → produces .war
  artifacts:
    paths: [ "*.war", "roc/build/docker/lib/*" ]   # save .war for docker stage
  needs: ["rcash:fetch:dev"]                # runs after fetch, pulls its artifacts
```

### STAGE: docker — build image (TWICE, parallel: jpe + jpw)
```yaml
rcash:docker:jpe:dev:
  stage: docker
  image: gcr.io/kaniko-project/executor:v1.5.2-debug   # Kaniko = build images w/o Docker daemon (secure)
  before_script:
    - mv *.war rcash_api/RCashAPI-BusinessLogic/target  # put .war where Dockerfile COPY expects it
  script:
    - sh roc/build/90_build_ci.sh build_kaniko_jpe      # build image + push to jpe2b Harbor
  needs: ["rcash:fetch:dev", "rcash:build:dev"]
```
- Kaniko runs your Chapter-1 Dockerfile. Two jobs (jpe/jpw) → one image per datacenter registry.

### STAGE: deploy — kubectl apply (TWICE, parallel: jpe + jpw)
```yaml
rcash:deploy:jpe:dev:
  stage: deploy
  image: ${KUBECTL_IMAGE}                   # the kubectl container
  script:
    - bash roc/dev/common/00_deploy_ci.sh deploy   # runs kubectl apply (CI version of 00_deploy.sh)
  environment:
    name: rcash-api-dev-jpe2b               # GitLab tracks deploy history per environment
  variables:
    BRANCH_SLUG: rcash-api-dev-jpe2b-${APP_VERSION}  # ← fills selector in Service YAML!
    CAAS_CLUSTER: jpe2-caas1-dev1           # which cluster to deploy to
    CLUSTER_PREFIX: jpe2b                   # ← fills ${CLUSTER_PREFIX} in DLB Service YAML
```
> FULL CIRCLE: `BRANCH_SLUG` here → substituted into the Service's `selector: app: "${BRANCH_SLUG}"`
> from Chapter 2. The pipeline sets the variable that decides which Pods the Service routes to.
> jpw version deploys identically to the jpw1a cluster (two-datacenter setup).

---

## Section 4 — Full picture of the dev pipeline
```
Push to dev/* branch → click "play" (manual)
  STAGE fetch:   rcash:fetch:dev   (gitlab-ee)  → SSH clone source → artifact rcash_api/
  STAGE build:   rcash:build:dev   (maven)      → mvn compile → artifact *.war
  STAGE docker:  jpe + jpw (Kaniko, PARALLEL)   → build image, push to each datacenter Harbor
  STAGE deploy:  jpe + jpw (kubectl, PARALLEL)  → kubectl apply to each cluster
→ rcash-api running in BOTH datacenters, fully automated
```

Connections to earlier chapters:
- Docker stage runs the **Dockerfile** from Chapter 1 (via Kaniko)
- Deploy stage runs **kubectl apply** of the Service/Deployment YAML from Chapter 2
- `BRANCH_SLUG` / `CLUSTER_PREFIX` fill the `${...}` placeholders in those YAMLs

---

## Section 5 — Validation gate (.gitlab-api-validation.yaml)
```yaml
.reservation:validation:
  image: .../alpine-curl:v1
  script:
    - |
      PAYLOAD=$(cat <<EOF
      { "secret": "${SECRET}", "bookingId": "${BOOKING_ID}", ... }
      EOF
      )
      CURL=$(curl -X POST -d "$PAYLOAD" "$VALIDATION_API_URL" -w "%{http_code}" -o /dev/null)
      if [ "$API_STATUS" !="200" ] && [ "$SECRET" != "$MASTER_SECRET" ]; then
        echo "Validation failed!"
        exit 1     # non-zero exit = job fails = pipeline STOPS
      fi
```
- Runs in `validate-reservation` stage (very first, before fetch).
- Calls a validation API with a secret/booking ID. If not approved → `exit 1` kills pipeline.
- A **deployment gate** — prevents unauthorized/conflicting deploys to shared environments.
- Shell rule: exit code 0 = success, non-zero = failure → CI uses this to pass/fail jobs.

---

## Chapter 3 Summary
| Concept | One line |
|---------|---------|
| Why CI/CD | Automate code→prod so it's fast, repeatable, no human error |
| CI | Auto build + test every push |
| CD | Auto deploy tested code |
| Pipeline | Whole automated workflow for a push |
| Stage | A phase; stages run in order |
| Job | A task; jobs in a stage run in parallel |
| Runner | The container/machine that executes a job |
| Artifact | File passed from one stage to the next (the .war) |
| Cache | Reused across runs to speed things up (Maven deps) |
| needs | Declares job dependencies + pulls their artifacts |
| when: manual | Job waits for a human to click play (safety gate) |
| Kaniko | Builds Docker images inside CI without the Docker daemon |
| Full circle | CI vars (BRANCH_SLUG, CLUSTER_PREFIX) fill the ${...} in K8s YAMLs |