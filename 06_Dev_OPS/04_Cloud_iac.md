# Chapter 4 — Cloud & Infrastructure as Code (My Notes)

> Reference repo: `/Users/somesh.sangwan/Desktop/rcash_api-roc`
> Real files taught: `roc/prod/gslb/gslb-rcash-api-prod_jpe_pri.json`, `roc/prod/lbaas/db/80_apply_lbaas.sh`
> Key realization: **this whole repo IS an Infrastructure-as-Code repo** (Rakuten One Cloud / ROC).

---

## Section 1 — Cloud fundamentals

Instead of buying servers, rent resources from a provider (AWS/Azure/GCP, or Rakuten One Cloud/ROC).

| Building block | What | Office equivalent |
|---------------|------|-------------------|
| Compute | VMs/containers running code | CaaS (K8s clusters) |
| Storage | Disks, object storage | Volumes, DBaaS storage |
| Networking | Virtual networks, LBs, DNS | LBaaS, GSLB |
| Database | Managed DB (you don't run the DB software) | DBaaS (MariaDB/MySQL) |
| IAM | Identity & Access — who can do what | ROC tokens (60_roc_token.sh) |

> "as a Service" (aaS): provider manages hardware/patching/HA; you just request & use.
> CaaS, LBaaS, DBaaS all follow this.

---

## Section 2 — What is Infrastructure as Code (IaC)?

### Problem it solves (clicking buttons in a console)
- Not repeatable (2nd datacenter = re-click everything)
- No record of current config
- No peer review
- Drift (manual change → reality ≠ docs)
- No disaster recovery (rebuild from memory?)

### The IaC idea
Write infrastructure as TEXT FILES, store in Git, apply with a tool. Files = source of truth.

| Without IaC | With IaC |
|-------------|----------|
| Click buttons | Write config files (JSON/YAML/HCL) |
| Config lives in the cloud | Config lives in Git (versioned) |
| Hope you remember steps | Re-run tool → identical result |
| No history | Git history of every change |
| Manual, error-prone | Automated, repeatable |

Benefits: repeatable, versioned, reviewable (PRs), self-documenting, recoverable.

> **rcash_api-roc IS an IaC repo.** Every LB, DNS entry, server group = a JSON file
> applied by shell scripts. README: "holds the BCP components and One Cloud setup."

---

## Section 3 — Two styles of IaC

**Declarative — "what I want"** (modern, preferred)
Describe desired end state; tool figures out how. → Terraform, K8s YAML, your GSLB JSON.

**Imperative — "step by step what to do"**
Write exact commands in order. → shell scripts, partly Ansible.

> Your repo is HYBRID: JSON files are declarative (desired LB/GSLB state),
> shell scripts are imperative (get token, call ROC API to apply JSON).
> Very common: declarative config + imperative glue scripts.

---

## Section 4 — Terraform (industry-standard IaC tool)

Office uses ROC, but Terraform is the must-know tool for the job market. Concepts map directly.

### Core workflow
```
1. WRITE   → .tf files (HCL language) describing desired infra
2. PLAN    → terraform plan  → preview what WILL change (no action)
3. APPLY   → terraform apply → make changes real
4. STATE   → terraform.tfstate → record of what it created
```

### Example (a load balancer — same idea as your LBaaS JSON)
```hcl
provider "aws" {
  region = "ap-northeast-1"          # Tokyo
}

resource "aws_lb" "rcash_api" {
  name               = "rcash-api-lb"
  internal           = true
  load_balancer_type = "application"
  health_check {
    path     = "/haproxy_status"     # same idea as your GSLB JSON!
    port     = 80
    protocol = "HTTP"
  }
}
```

### 4 key concepts
| Concept | What |
|---------|------|
| Provider | Plugin for a cloud (AWS/Azure/GCP). Translates code → API calls |
| Resource | A piece of infra (VM, LB, DNS record, DB) |
| State | terraform.tfstate — memory of what it created. How it knows create vs modify vs delete |
| Module | Reusable bundle of resources (like a function — define once, use for jpe2b AND jpw1a) |

### Why State matters most (and is dangerous)
```
Code: "I want 5 servers"
State: "I previously created 3 (a,b,c)"
terraform plan → compares → "I will ADD 2"
```
Lose the state file → Terraform forgets what it manages → chaos.
Teams store state remotely + locked (so two applies don't collide).

---

## Section 5 — Real IaC files explained

### GSLB file (DNS-based global load balancing) — defined as code!
`roc/prod/gslb/gslb-rcash-api-prod_jpe_pri.json`:
```json
{
  "name": "rcash-api",
  "domain": "rcash-api.gslb.dcnw.rakuten",   // DNS name users hit
  "internet": "private",                      // internal network only
  "ttl": 60,                                  // DNS cache 60s → fast failover
  "failover_list": [
    { "domain": "lb-...-93.lbaas.jpe2b..." },  // jpe2b LB (primary — file = jpe_pri)
    { "domain": "lb-...-110.lbaas.jpw1a..." }  // jpw1a LB (backup)
  ],
  "health_check_http": {
    "method": "GET", "path": "/haproxy_status",
    "port": 80, "expected_status_code": 200
  }
}
```
- This IS the GSLB from Chapter 2, written as code.
- `failover_list` = the multi-datacenter failover. jpe2b primary, jpw1a backup.
- `health_check_http`: GSLB checks /haproxy_status. If jpe2b stops returning 200 →
  auto-failover to jpw1a.
- `ttl: 60`: DNS cached 60s only → clients re-resolve fast → failover is quick.
- Filename `jpe_pri` = jpe2b primary; matching `jpw_pri` = jpw1a primary.

### Apply script (imperative glue)
`roc/prod/lbaas/db/80_apply_lbaas.sh`:
```bash
#!/bin/bash
set -euo pipefail                  # -e exit on error, -u error on undefined var, pipefail catch pipe errors
this="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # this script's dir
. "$this/SETUP.sh"                 # source shared vars/functions
apply_lbaas $TENANT_NAME $LB_NAME "$this" "db-lb-*.json"  # apply matching JSON files
```
- `set -euo pipefail` = production bash safety. Best practice for any script.

### Numbered script workflow (= Terraform's plan/apply loop)
| Script | Role | Terraform equivalent |
|--------|------|---------------------|
| 60_roc_token.sh | get auth token (IAM) | credentials/auth |
| 10_get_*.sh | READ current state | terraform plan |
| 80_apply_*.sh | APPLY desired state | terraform apply |

---

## Section 6 — Mapping ROC ↔ generic cloud / Terraform
```
ROC (your office)              ≈  Generic cloud / Terraform
─────────────────────             ──────────────────────────────
GSLB JSON files                ≈  DNS + global LB (Route53)
LBaaS JSON files               ≈  Load balancer resources (aws_lb)
servergroups JSON              ≈  Target groups / backend pools
60_roc_token.sh                ≈  IAM auth / credentials
10_get_*.sh                    ≈  terraform plan (read state)
80_apply_*.sh                  ≈  terraform apply (enforce state)
JSON files in Git              ≈  .tf files in Git (versioned infra)
```
Skills transfer fully. You already DO IaC at work — Terraform is just a universal syntax for it.

---

## Hands-on (personal PC) — try Terraform
```bash
brew install terraform
terraform version

# make a folder, create main.tf with the aws_lb example above (or use a local provider)
terraform init       # download provider plugins
terraform plan       # preview changes
terraform apply      # make them real
terraform destroy    # tear everything down
terraform state list # see what Terraform manages
```
> Tip: to learn without a cloud account, use the `local` or `random` provider, or
> the `docker` provider (manage local Docker containers via Terraform) — ties back to Ch.1.

---

## Chapter 4 Summary
| Concept | One line |
|---------|---------|
| Cloud | Rent compute/storage/network/DB/IAM instead of buying servers |
| aaS | Provider manages the hard parts; you request & use (CaaS/LBaaS/DBaaS) |
| IaC | Infra written as text files in Git, applied by a tool = source of truth |
| Declarative | Describe desired state; tool figures out how (Terraform, K8s, GSLB JSON) |
| Imperative | Exact step-by-step commands (shell scripts) |
| Terraform workflow | write → plan → apply → state |
| Provider | Cloud plugin (translates code → API calls) |
| Resource | A piece of infrastructure |
| State | Terraform's memory of what exists — most critical concept |
| Module | Reusable resource bundle (one def → jpe2b + jpw1a) |
| Your repo | Real IaC: GSLB/LBaaS JSON (declarative) + apply scripts (imperative) |