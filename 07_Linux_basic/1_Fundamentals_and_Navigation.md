# Chapter 1 — Getting Started, Linux Fundamentals & File System Navigation

> **Status:** ✅ Completed
> **Goal:** Understand what Linux is, its directory tree structure, the differences between VMs and containers, absolute vs. relative paths, folder navigation tools, and basic command-line orientation.

---

## 📝 Notes

### 1. What is Linux?
* Linux is an **operating system** — the software mediator between your applications and the physical hardware (CPU, RAM, disk, networking). Your code asks the OS to open files, bind ports, or allocate memory; the OS executes these operations securely.
* Strictly speaking, **Linux** is only the **kernel** (the core engine managing resources and hardware).
* What you interact with is a **distribution (distro)** — the Linux kernel bundled with utility tools, libraries, and a package manager:
  * **Ubuntu / Debian**: Extremely common on servers, beginner-friendly, uses the `apt` package manager.
  * **RHEL / CentOS / Rocky / Amazon Linux**: Standard in enterprise systems and AWS environments, uses `yum`/`dnf` package managers.
  * **Alpine**: Ultra-lightweight (often < 5MB), standard for building optimized Docker container images.

> [!NOTE]
> **Backend Developer Context:** Almost all production databases, API gateways, message queues, and application servers run on Linux. Even if you develop on macOS or Windows, your code will ultimately compile and run in a Linux environment in production.

---

### 2. The Shell — Your Command Interface
* A **shell** is a command-line interpreter that reads your keystrokes, executes programs, and outputs results in a loop.
* Popular shells include **Bash** (`bash`) and **Zsh** (`zsh` - standard on macOS).
* **Anatomy of a shell prompt** (e.g., `somesh@prod-server:~$`):
  * `somesh` = Current user.
  * `prod-server` = Hostname of the machine.
  * `~` = Shortcut for the current user's home directory.
  * `$` = Regular user shell prompt (`#` indicates you are logged in as the superuser `root`).
* **Command Syntax:** `command [options] [arguments]`
  ```
  ls -l /var/log
  │   │   └── argument (what to act on)
  │   └────── option/flag (changes behavior, usually prefixed with - or --)
  └────────── the command
  ```

---

### 3. The Directory Tree Hierarchy
Unlike Windows which uses drive letters (`C:`, `D:`), Linux represents the filesystem as a single tree starting at `/` (the **root** directory). Every physical disk, partition, network share, and device hangs off `/`.

| Directory | Contents | Developer Relevance |
|:---|:---|:---|
| `/etc` | System configuration files | Edit files here to configure Nginx, SSH, databases, etc. |
| `/var/log` | Application and system logs | **First place to look when an application crashes.** |
| `/home` | User home directories | Default environment for user files (e.g., `/home/somesh`). |
| `/root` | Home directory of the `root` administrator | Restricted access, requires root privileges to read. |
| `/tmp` | Ephemeral scratch space | Place for temporary files; typically wiped on reboot. |
| `/usr/bin`, `/bin`, `/sbin` | Built-in commands and binaries | Where default system programs reside. |
| `/opt` | Optional/Third-party software | Often where self-contained vendor applications are installed. |
| `/proc`, `/sys` | Virtual kernel statistics directories | Real-time views into system memory, CPU, and device drivers (not real disk files). |

---

### 4. "Where Am I?" — Machine vs. Container vs. Pod vs. Cluster
Modern backend deployments leverage virtualization. It is critical to know what level of host you are logged into.

```
Cluster (The entire fleet of nodes)
  └── Node (A physical machine or Virtual Machine (VM))
        └── Pod (A Kubernetes scheduling unit)
              ├── Container: primary-api       ← Your application code runs here
              ├── Container: redis-cache       ← In-memory database sidecar
              └── Container: log-collector     ← Ships logs to Elastic/Splunk
```

1. **Machine / VM**: A virtualized computer with its own kernel, systemd services, and storage. Logging in via SSH (`ssh user@ip`) drops you into the VM's filesystem.
2. **Container**: A isolated namespace running on the host kernel. It has its own `/` directory, libraries, and process namespace, but shares the underlying host's kernel. Built using **Docker**.
3. **Pod (Kubernetes)**: A group of one or more tightly coupled containers sharing network and storage resources inside a cluster.
4. **Cluster**: The group of nodes managed by an orchestrator like Kubernetes.

> [!WARNING]
> **Filesystem Ephemerality:** Changes made to a container's local filesystem (e.g., running `npm install` directly inside a running container) **will not survive a restart** unless written to a persistent Volume. The container is destroyed and recreated fresh from its base Image.

#### How to detect if you are inside a container:
* Run `hostname`. If it returns a random alphanumeric string (e.g., `api-service-7d9c8f62`), you are likely inside a container.
* Check `/proc/1/cgroup`. If it references `docker` or `kubepods`, you are running in a container.

---

### 5. Working Directory & Path Resolution
At any time, your shell is stationed in a **current working directory (CWD)**.
* **Absolute Path:** Always starts with the root `/`. It is unambiguous and resolves correctly regardless of where you are standing.
  * *Example:* `/var/log/nginx/access.log`
* **Relative Path:** Does not start with `/`. It resolves relative to your current location (CWD).
  * *Example:* If you are in `/var/log`, the relative path `nginx/access.log` points to `/var/log/nginx/access.log`.

---

### 6. Path Shortcuts & Directory Chaining
* `.` = The current directory ("here"). Used to run files in place, e.g., `./start.sh`.
* `..` = The parent directory (one level up).
* `~` = The current user's home directory.
* `-` = The previous directory you visited.

```bash
cd ../..       # Navigate up two levels
cd ../../etc   # Navigate up two levels, then down into the etc directory
cd -           # Toggle back to the directory you were in before the last 'cd'
```

---

### 7. Directory Navigation & Peeking
To inspect files without moving your working directory:
* `ls` lists files. Use it to peek at paths: `ls /etc` lets you inspect configuration files without leaving `/home`.
* **Tab Completion:** Pressing `Tab` tells the shell to auto-complete path names. Pressing it twice lists all available matches. Lean on this to avoid typos.

---

### 8. Common Error Analysis
If you run `cd conf` and see:
`sh: cd: can't cd to conf: No such file or directory`
This indicates the shell looked for a directory named `conf` relative to your current `pwd` and found nothing.
> [!TIP]
> **Debugging Habit:** Whenever you encounter a "No such file or directory" error, immediately run `pwd` to confirm where you are, followed by `ls` to check what files actually exist in your current location.

---

### 9. Survival Modal Editing: `vi` / `vim`
Many production servers only have raw terminal access, making `vi` or `vim` the default editor. It is modal:
* **Normal Mode (Default)**: Keys map to commands. You cannot write text. Press `Esc` to return to this mode.
* **Insert Mode**: Used for typing text. Enter this mode by pressing `i` while in Normal Mode.

#### Essential Save & Exit Sequences (Type from Normal Mode):
* `:w` — Write (save) changes.
* `:q` — Quit.
* `:wq` — Write (save) and quit.
* `:q!` — Quit immediately **without saving** (your escape hatch when you make a mistake).

---

## 🔑 Key Commands

| Command | Action | Real-World Use Case |
|:---|:---|:---|
| `whoami` | Displays current logged-in user | Check if you are operating as `root` or a service user. |
| `pwd` | Print Working Directory | Identify where you are on the filesystem. |
| `ls -la` | List all files in long format including hidden files | Check file sizes, owners, and hidden configs (like `.env`). |
| `ls -lh` | List in human-readable sizes (K, M, G) | Quickly spot oversized log files. |
| `ls -ltr` | Sort by modification time, oldest first | List newest files at the bottom (ideal for active logs). |
| `cd <path>` | Change working directory | Navigate across directories. |
| `cd -` | Switch to the previous directory | Toggle quickly between two directories. |
| `tree -L 2` | Draw filesystem directory up to 2 levels | Map out an unfamiliar project directory structure. |
| `cat <file>` | Output file contents to standard output | Quickly dump small configuration files or scripts. |
| `less <file>` | Page through a large text file | Read large files (use arrow keys to scroll, press `q` to exit). |
| `hostname` | Print system hostname | Determine if you are on a container or bare metal VM. |

---

## 💡 Interview Points

* **What is the difference between the Linux Kernel and a Distribution?**
  The kernel handles hardware resource allocation (memory, scheduling, IO). The distribution adds user-space tools, shell environments, and package managers (e.g. Debian, CentOS).
* **Where do application logs live by convention?**
  `/var/log` (e.g., `/var/log/nginx`, `/var/log/syslog`).
* **What is the difference between `/` and `~`?**
  `/` is the system root directory (highest point in the tree). `~` is the current user's personal home directory (usually `/home/username`).
* **How does `cd logs` differ from `cd /logs`?**
  `cd logs` is a relative path navigation (looks for `logs` inside your current directory). `cd /logs` is an absolute path navigation (looks for `logs` directly under root `/`).
* **What does `ls -la` show?**
  It lists all directory entries (including hidden files starting with `.`) with detail columns: permissions, link count, owner, group, size, and modification date.

---

## 🧪 Worked Examples

### Troubleshooting Container Navigation
You exec into a Kubernetes pod to check config files:
```bash
# Get into the container's shell
kubectl exec -it web-service-abc12 -- sh

# View current location
$ pwd
/app

# List files (including hidden environment configs)
$ ls -la
drwxr-xr-x 1 node node 4096 Jun 19 12:00 .
drwxr-xr-x 1 root root 4096 Jun 19 12:00 ..
-rw-r--r-- 1 node node  220 Jun 19 12:00 .env
-rw-r--r-- 1 node node  412 Jun 19 12:00 package.json
drwxr-xr-x 1 node node 4096 Jun 19 12:00 src

# Check root filesystem structure
$ cd /
$ ls -l
drwxr-xr-x   2 root root 4096 May 13 14:02 bin
drwxr-xr-x   2 root root 4096 Apr  9 09:12 etc
drwxr-xr-x   2 root root 4096 May 13 14:02 home
drwxr-xr-x   2 root root 4096 Jun 19 12:00 opt
drwxr-xr-x   2 root root 4096 May 13 14:02 var
```

---

## 🧪 Exercises to Try

Perform these exercises inside a safe container (`docker run -it --rm ubuntu bash`):
1. Run `pwd` to note your starting directory.
2. Navigate to root `cd /` and view the tree structure using `ls -l`.
3. Navigate to `/var/log` and check modification times of logs using `ls -ltr`.
4. Run `cd` with no arguments. Confirm you returned to your home directory (`~`) using `pwd`.
5. Run `cd -` to return to `/var/log`.
6. Run `ls -la ~` to view hidden startup config files (e.g. `.bashrc`).
7. Enter a deep nested directory, then jump back up three directories using `cd ../../..`.
8. Create a scratch text file `vi /tmp/scratch.txt`, write "hello world", save and exit. Then read it using `cat /tmp/scratch.txt`.
