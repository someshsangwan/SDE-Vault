# 🐧 Linux Learning — for Backend Developers

> My personal Linux course. Goal: become fluent on the command line, comfortable debugging a live server, and ready for interviews + a job switch.

## 📚 Chapters

| # | Chapter | Focus | Status |
|---|---------|-------|--------|
| 1 | [Getting Started, Fundamentals & File System Navigation](1_Fundamentals_and_Navigation.md) | What Linux is, filesystem layout, VM/Container/Pod model, Navigation (`ls`, `cd`, `pwd`), Tab completion, and `vi`/`vim` survival kit. | ✅ Completed |
| 2 | [Pipes, Redirection & Text Processing](2_Pipes_Redirection_and_Text_Processing.md) | Stream redirection (`>`, `>>`, `2>&1`), Pipelines (`|`), `tee`, `xargs`, and text filters (`grep`, `find`, `sed`, `awk`, `cut`, `sort`, `uniq`, `wc`). | ✅ Completed |
| 3 | [System Permissions, Ownership & Server Administration](3_Permissions_and_System_Administration.md) | Permissions (`chmod`, `chown`, `sudo`, SUID, Sticky Bit), Processes, Resource monitoring (`top`, `free`, `df`, `du`), and Systemd services/logs. | ✅ Completed |
| 4 | [Networking, Automation & Scenario Troubleshooting](4_Networking_Automation_and_Troubleshooting.md) | Networking (`ssh`, `scp`, `curl`, `ss`, `lsof`), Shell scripting (`set -euo pipefail`), `cron` scheduling, Outage playbooks, `tmux` & `vim`. | ✅ Completed |

## 🗺️ How this works
- **Ch 1** = Core mental model & filesystem orientation.
- **Ch 2** = Pipelining & processing logs/files (the developer's superpower).
- **Ch 3** = Security, privilege, and live server performance troubleshooting.
- **Ch 4** = Network probing, automation pipelines, and resolving system outages.

## 🎯 Practice resources
- **OverTheWire: Bandit** — https://overthewire.org/wargames/bandit/ (free Linux puzzle game)
- A real Linux box: cloud VM (AWS/GCP/DigitalOcean) or Ubuntu in Docker — SSH in daily.

## ✅ Progress log
- **2026-06-20** — Restructured learning material from 10 micro-chapters into 4 comprehensive operational chapters. Improved formatting with alert styles, added backend developer context boxes, and streamlined production troubleshooting drills.