# Chapter 3 — System Permissions, Ownership & Server Administration

> **Status:** ✅ Completed
> **Goal:** Master user/group access control, privilege management (`sudo`), process life cycles, real-time resource tracking (CPU, memory, disk), and systemd service management.

---

## 📝 Notes

### 1. Filesystem Access Control

#### Owner, Group, and Everyone Else
Every file and directory in Linux has an **owning user**, an **owning group**, and permissions defined for three classes of actors:
* **`u` (User)**: The account that owns the file.
* **`g` (Group)**: The group of users that owns the file.
* **`o` (Others)**: Everyone else on the system.

#### Reading `ls -l` Permissions
```
-rw-r--r--  1  somesh  staff  2055  Jun 18  config.env
│└──┬──┘    │  └──┬─┘  └─┬─┘
│   │       │     │      └── owning group
│   │       │     └───────── owning user
│   │       └─────────────── hard links count
│   └─────────────────────── permissions (3 triplets of rwx)
└─────────────────────────── file type (- = file, d = directory, l = symlink)
```

The 9 permission characters represent three `rwx` triplets:
```
-     rw-      r--      r--
│      │        │        │
type  user   group    others
     (owner)
```
* **`r` (Read)**: Value = 4
* **`w` (Write)**: Value = 2
* **`x` (Execute)**: Value = 1
* **`-` (Disabled)**: Value = 0

> [!IMPORTANT]
> **Files vs. Directories Gotcha:**
> * On a **File**: `r` = read text; `w` = edit content; `x` = run as executable.
> * On a **Directory**: `r` = list entries (`ls`); `w` = create, rename, or delete files inside; **`x` = enter the directory (`cd`) or access files inside.**
> * *Interview Gotcha:* To **delete** a file, you need **write permissions (`w`) on its parent directory**, not on the file itself!

#### Octal vs. Symbolic Permission Syntax
Permissions can be set numerically (octal) or symbolically using `chmod`.

##### Octal Cheat Sheet:
* **`755`** (`rwxr-xr-x`): Owner can do anything; others can read/enter/run. Standard for directories and executables.
* **`644`** (`rw-r--r--`): Owner can read/write; others can only read. Standard for configuration and source files.
* **`600`** (`rw-------`): Owner only. Standard for private SSH keys, `.env` files, and database passwords.
* **`777`** (`rwxrwxrwx`): Full access for everyone. **Avoid this in production**; it is a major security vulnerability.

```bash
chmod 600 secret.key       # Set secret key to owner-only read/write
chmod +x start.sh          # Add executable bit for all users (symbolic)
chmod -R 755 /var/www      # Recursively apply permissions to folder contents
```

#### Changing Ownership
Use `chown` to reassign the user and group owning a file.
```bash
chown alice file.txt             # Change owner to user 'alice'
chown alice:devs file.txt        # Change owner to 'alice' and group to 'devs'
chown -R www-data:www-data /app  # Change ownership recursively (common for web servers)
```

---

### 2. System Identity & Privilege Escalation

* **`root`**: The superuser account. It has **UID 0** and bypasses all filesystem permission checks.
* **Identity Files:**
  * `/etc/passwd`: List of user accounts, UIDs, home directories, and login shells (world-readable).
  * `/etc/shadow`: Encrypted user password hashes (root-readable only).
  * `/etc/group`: Defines system groups and member users.
* **`sudo` (Superuser Do)**: Allows permitted users to execute a command with root privileges.
  * *Audit Trail:* `sudo` logs every command run under its escalation, providing trace logs.
  * *Least Privilege:* Elevates privilege temporarily for one command rather than logging in as root.

#### Special Permission Flags
* **SetUID (`s`)**: When an executable has SetUID set (e.g., `-rwsr-xr-x`), any user who runs it executes the program with the privileges of the *file owner* (often root). Used by `/usr/bin/passwd` to let users modify system files.
* **Sticky Bit (`t`)**: Applied to shared directories (like `/tmp`). It prevents users from deleting or renaming files owned by other users, even if they have write access to the directory. Shown as `drwxrwxrwt`.

---

### 3. Process Management & Job Control

A **process** is a running instance of a program. Every process has a unique **Process ID (PID)** and a parent process.

#### Checking Running Processes
* `ps aux` / `ps -ef`: Captures a snapshot of all active processes.
* `top` / `htop`: Interactive real-time process monitoring (resource consumption sorted by CPU/Memory).

#### Signals and Termination
Linux communicates with processes using signals. The most common are:
* **`SIGTERM` (15)**: Request graceful shutdown. The process is allowed to clean up connections, save state, and exit. (Default signal of `kill`).
* **`SIGKILL` (9)**: Force immediate termination. The kernel terminates the process instantly; it cannot catch or ignore this signal, preventing cleanup.

```bash
kill 1234        # Send SIGTERM (15) to PID 1234
kill -9 1234     # Send SIGKILL (9) to force kill PID 1234
killall node     # Terminate all processes named 'node'
```

#### Job Control
You can manage jobs directly inside a single shell session:
* `&`: Run a command in the background.
  ```bash
  node app.js &
  ```
* `jobs`: List background jobs associated with the current shell.
* `ctrl+z`: Suspend the current foreground process and send it to the background in a paused state.
* `bg`: Resume a suspended job in the background.
* `fg %1`: Bring background job #1 to the foreground.

---

### 4. Storage & Memory Diagnostics

#### Memory (RAM)
Use `free -h` to read system memory allocations:
```
              total        used        free      shared  buff/cache   available
Mem:          7.7Gi       3.1Gi       1.2Gi       200Mi       3.4Gi       4.1Gi
```
> [!TIP]
> **Understanding Memory Availability:** Do not panic if `free` is low. Linux uses unused memory for disk cache (`buff/cache`) to speed up execution. The kernel frees this cache instantly if applications demand it. Focus on the **`available`** column—this represents memory actually open for new processes.

#### Disk Storage
* `df -h`: Displays remaining disk space per partition.
* `du -sh <dir>`: Displays the total space occupied by a directory.
* `du -ah <dir> | sort -rh | head -n 10`: Recursively lists the 10 largest files in a directory.

---

### 5. Services & System Logs (`systemd`)

On modern Linux, **`systemd`** manages services (daemons).

#### Controlling Services with `systemctl`
* `systemctl start nginx`: Start the service immediately.
* `systemctl enable nginx`: Configure the service to start automatically during system boot.
* `systemctl status nginx`: Check if the service is running, exited, or crashed.
* `systemctl restart nginx`: Stop and start the service.

#### Reading Logs with `journalctl`
`systemd` routes service console outputs to a unified binary logging engine queried using `journalctl`.
* `journalctl -u nginx`: View log history for the Nginx service.
* `journalctl -u nginx -f`: Follow Nginx logs in real-time.
* `journalctl --since "1 hour ago"`: Query recent log entries.
* `journalctl -p err`: View error and critical logs only.

#### Classic `/var/log` and Rotation
Non-systemd logs write plain text to `/var/log`.
* **Log Rotation (`logrotate`)**: Prevents logs from filling the disk. It renames active files daily/weekly (e.g., `app.log` becomes `app.log.1`) and compresses older files (e.g., `app.log.2.gz`).
* *Reading compressed logs:* Use `zcat` or `zless` to inspect compressed logs without extracting them first.
  ```bash
  zless /var/log/nginx/access.log.2.gz
  ```

---

## 🛠️ Incident Troubleshooting Playbooks

### 🚒 Playbook A: "The Server is Slow"
If a live production system is lagging or unresponsive:

1. **Check System Load & Resource Culprits:**
   Run `top` or `htop`. Look at CPU load averages (1, 5, 15 minutes) and sort processes by CPU (`P`) or Memory (`M`).
2. **Check RAM Consumption:**
   Run `free -h`. Inspect the `available` memory. If Swap space is being heavily written to, the system is out of physical RAM, triggering disk-thrashing.
3. **Check Storage Capacity:**
   Run `df -h`. If root `/` is at 100% capacity, apps cannot write temporary files, lock files, or session logs, causing them to freeze.
4. **Identify Disk I/O Bottlenecks:**
   Run `vmstat 1` or `iostat`. Look at `wa` (CPU waiting for I/O). If this is high, slow database disk read/write cycles are choking the CPU.

---

### 🚒 Playbook B: "My Application Service is Down"
If your backend application stops responding to traffic:

1. **Verify Service Daemon Status:**
   ```bash
   sudo systemctl status my-app
   ```
   *Look for `Active: active (running)` or `Active: failed (Result: exit-code)`.*
2. **Inspect Service Logs:**
   If failed, extract the crash stack trace:
   ```bash
   sudo journalctl -u my-app -n 50 --no-pager
   ```
3. **Check Port Bindings:**
   If the app crashes with "address already in use", find which process is holding your port:
   ```bash
   sudo ss -tlnp | grep :8080
   ```
4. **Inspect Resource Caps:**
   Check system logs `/var/log/syslog` or `/var/log/messages` for Out-of-Memory (OOM) actions:
   ```bash
   sudo dmesg -T | grep -i "oom-killer"
   ```
   *If the system ran out of RAM, the kernel's OOM Killer will forcibly terminate the heaviest process (usually Java, Node, or Database engines) to keep the system alive.*

---

## 🔑 Key Commands

| Command | Action | Real-World Use Case |
|:---|:---|:---|
| `ls -ld <dir>` | Show directory permissions only, not contents | Check if directory allows listing or entry. |
| `chmod 600 <key>`| Restrict file permissions to owner-only | Protect SSH private keys (`id_rsa`). |
| `chown -R u:g <dir>`| Change file owner and group recursively | Reassign server files to run under `www-data`. |
| `id` | Display user UID, GID, and active groups | Check if your current user has admin groups. |
| `sudo -i` | Open a root interactive shell | Execute multi-command administrative edits. |
| `ps aux` | Capture a snapshot of all active processes | Search for running node or python processes. |
| `kill -15 <PID>`| Request graceful termination (`SIGTERM`) | Normal process shutdown. |
| `kill -9 <PID>` | Force kill process instantly (`SIGKILL`) | Terminate a frozen, locked-up process. |
| `free -h` | Display memory usage in human-readable units | Check system RAM capacity and availability. |
| `df -h` | Check remaining storage capacity per disk | Diagnose full disk crashes. |
| `du -sh <dir>` | Get cumulative disk space of a directory | Find where logs or temporary files are bloating. |
| `systemctl status`| View daemon run states and statuses | Verify if database or web server is running. |
| `journalctl -u <s> -f`| Watch service logs live | Debug API server route calls during runtime. |
| `zless <file.gz>`| Page read gzip compressed log files | Search rotated, historic log archives. |

---

## 💡 Interview Points

* **Explain the columns of `-rwxr-xr-x`.**
  * First char: type (`d`=directory, `-`=file, `l`=symlink).
  * Chars 2-4: Owner permissions (`rwx` = read, write, execute).
  * Chars 5-7: Group permissions (`r-x` = read, execute).
  * Chars 8-10: Others/World permissions (`r-x` = read, execute).
* **If you have read-only access to a file, can you delete it?**
  Yes, if you have write (`w`) permissions on the directory containing the file. Deletion is a directory entry modification, not a file content modification.
* **What is the difference between `kill -9` and `kill -15`?**
  `kill -15` sends `SIGTERM`, allowing the process to handle the signal, shut down gracefully, and clean up resources. `kill -9` sends `SIGKILL`, which cannot be caught or blocked; the OS instantly terminates the process.
* **What does `systemctl enable` do?**
  It configures the service to automatically boot on system startup by creating symlinks in systemd target folders. It does *not* start the service immediately (that requires `systemctl start`).
* **What is the OOM Killer?**
  The Out-Of-Memory Killer is an OS kernel safeguard. When physical RAM is exhausted, it selects and terminates a process (typically the memory-hogging backend application) to prevent system freeze.

---

## 🧪 Exercises to Try

Use a practice sandbox container (`docker run -it --rm ubuntu bash`):

### 1. Permission and Deletion Practice
```bash
# Set up folders
mkdir /tmp/sandbox && touch /tmp/sandbox/secret.txt
chmod 555 /tmp/sandbox        # Read and Enter only, NO Write
ls -ld /tmp/sandbox           # Verify directory permissions

# Attempt deletion
rm /tmp/sandbox/secret.txt    # Expect Failure: "Permission denied"

# Restore permissions and cleanup
chmod 755 /tmp/sandbox
rm /tmp/sandbox/secret.txt
```

### 2. Job Control Practice
```bash
# Start a sleep task in background
sleep 200 &
jobs                          # Verify job status

# Bring to foreground, suspend, and return to background
# Press Ctrl+Z to suspend
bg                            # Resume execution in background
jobs                          # Confirm running
killall sleep                 # Cleanup
```

### 3. Analyzing Memory & Disk Space
* Run `df -h` and identify which mount point corresponds to your main root partition `/`.
* Run `free -h`. Calculate the sum of `free` + `buff/cache` memory, and compare it with the `available` memory metric.
* Run `du -sh /var` to see how much storage space system records are using.
