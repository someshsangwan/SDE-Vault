# Chapter 4 — Networking, Automation & Scenario Troubleshooting

> **Status:** ✅ Completed
> **Goal:** Master networking commands (`ssh`, `curl`, `ss`), write robust automation scripts using bash strict mode, schedule tasks with `cron`, navigate terminal tools (`tmux`, `vim`), and execute production troubleshooting drills.

---

## 📝 Notes

### 1. Host Networking & Remote Access

#### The Client-Server and Port Binding Model
When a backend service starts (e.g., Express, Spring Boot, PostgreSQL), it binds to a specific IP address and port (e.g., port `8080` or `5432`) to listen for incoming connections.
* **`127.0.0.1` (localhost)**: The loopback address. Only processes running on the *same machine* can connect to it.
* **`0.0.0.0`**: Tells the system to listen on *all* network interfaces (wired, Wi-Fi, docker virtual bridge). Essential if you want external machines to connect.

#### SSH (Secure Shell) & Keys
SSH is the protocol used to securely connect to a remote shell.
* **Key-Based Authentication**: Safer than passwords. Uses asymmetric cryptography (public key + private key).
  ```bash
  ssh-keygen -t ed25519 -C "admin@domain.com" # Generate modern keys
  ssh-copy-id user@remote-ip                 # Copy public key (~/.ssh/authorized_keys) to server
  ssh -i ~/.ssh/my_private_key user@remote-ip # SSH using specific key
  ```
> [!CAUTION]
> **Private Key Safety:** Your private key (`id_rsa` or `id_ed25519`) must be kept secret. Linux enforces strict file permissions on keys; if your private key is readable by others (`chmod 644`), SSH will refuse to run. Keep it locked down with **`chmod 600`**.

#### File Transfers: Local (`cp`, `mv`) & Remote (`scp`, `rsync`)

Managing applications requires copying config templates, moving logs, and shipping built artifacts between servers.

##### A. Local File Actions (`cp` & `mv`)
* **`cp` (Copy)**: Copies files or directories.
  * *Copy file*: `cp app.config app.config.bak`
  * *Copy directory*: Use `-r` (recursive) to copy a folder and all its contents:
    ```bash
    cp -r ./src ./src_backup
    ```
* **`mv` (Move / Rename)**: Moves a file or directory to a different path, or renames it in place.
  * *Rename*: `mv config.env.template config.env`
  * *Move*: `mv log.txt /var/log/myapp/`

##### B. Secure Remote Copy (`scp`)
`scp` uses SSH to transfer files securely between hosts. 

🎯 **Basic Syntax**
```bash
scp [options] <source> <destination>
```

* 🔸 **Local to Remote (Upload)**:
  ```bash
  # Copy local-app.jar to /opt/app/ on remote server
  scp local-app.jar user@remote-ip:/opt/app/
  ```
* 🔸 **Remote to Local (Download)**:
  ```bash
  # Copy remote /var/log/nginx/error.log to your local current folder (.)
  scp user@remote-ip:/var/log/nginx/error.log .
  ```
* 🔸 **Recursive Directory Copy**: Use `-r` to transfer entire folders.
  ```bash
  # Upload local static asset folder to remote server
  scp -r ./static user@remote-ip:/var/www/html/
  ```

##### C. Incremental Remote Sync (`rsync`)
Unlike `scp` which copies the entire file, `rsync` only copies the *differences* between source and destination files. It is faster, can show progress, and can resume aborted transfers.
```bash
# Sync local src/ directory to remote opt/src/
rsync -avz --progress ./src/ user@remote-ip:/opt/src/
```
* 👉 **Means:**
  * `-a` (archive) ── preserves permissions, owners, and modification times recursively.
  * `-v` (verbose) ── displays transferring file lists.
  * `-z` (compress) ── compresses data during transmission to save bandwidth.
  * `--progress` ── displays real-time transfer speeds and percentages.

#### Terminal HTTP Requests: `curl` & `wget`
Essential for testing REST endpoints and verifying microservice communication.
```bash
curl http://localhost:8080/health                  # Make GET request
curl -i http://localhost:8080/health                # Include HTTP headers in output
curl -X POST -H "Content-Type: application/json" -d '{"id": 1}' http://api.internal/items
wget -O setup.sh http://domain.com/install.sh       # Download URL target directly to custom file name
```

#### Network Statistics & Port Auditing
When troubleshooting socket conflicts or checking if an app is listening:
* **`ss` / `netstat`**: Lists open sockets and port mappings.
  * **`ss -tlnp`**: **T**CP, **L**istening, **N**umeric port values, showing **P**rocess ownership.
* **`lsof -i :<port>`**: Lists **O**pen **F**iles mapping to the specified port.
  ```bash
  sudo ss -tlnp | grep :8080  # Check if port 8080 is bound
  sudo lsof -i :5432          # Find process blocking Postgres port
  ```

---

### 2. Shell Scripting & Automation

A shell script is a plain-text file containing sequential commands.

#### Script Structure & Shebang (`#!`)
The first line of a script is the **shebang** (e.g., `#!/bin/bash`). It tells the kernel which interpreter to use to run the script.
Make the script executable with `chmod +x script.sh` and run it using `./script.sh`.

#### Variables & Command Substitution
* Variables are case-sensitive. **Do not put spaces around the `=` sign!**
* Use `$()` to capture command outputs and save them to variables.
  ```bash
  #!/bin/bash
  BACKUP_DIR="/opt/backups"
  CURRENT_DATE=$(date +%Y-%m-%d)
  echo "Starting backup in $BACKUP_DIR on $CURRENT_DATE"
  ```

#### Conditionals & Exit Codes
Every Linux command returns an **exit code** (between `0` and `255`).
* **`0`**: Success.
* **Non-Zero (e.g., 1, 127)**: Error. Look up the code with `$?` (holds the status of the last executed command).

```bash
#!/bin/bash
if [ -f "/etc/nginx/nginx.conf" ]; then
    echo "Nginx config found"
else
    echo "Nginx config missing!" >&2  # Redirect message to stderr
    exit 1                            # Terminate script with error code
fi
```

#### Loops & Functions
```bash
# Loop through values:
for host in db-01 cache-01 api-01; do
    ping -c 1 "$host" || echo "$host is unreachable!"
done

# Function declaration:
log_message() {
    local level=$1
    local msg=$2
    echo "[$(date +%T)] [$level] - $msg"
}
log_message "INFO" "Database sync initiated"
```

#### Bash Strict Mode: `set -euo pipefail`
To prevent scripts from silent failures, declare these flags at the top of your scripts:
```bash
set -euo pipefail
```
* **`-e` (exit on error)**: Terminates the script immediately if any command fails (non-zero exit).
* **`-u` (nounset)**: Terminates the script if it references an unassigned, empty variable.
* **`-o pipefail`**: If any command in a pipeline fails (e.g., `command_1 | command_2`), the whole pipeline returns the error code, rather than hiding it under `command_2`'s success.

#### Automation Scheduling with `cron`
`cron` runs jobs in the background at regular intervals.
* Manage cron entries: `crontab -e` (edit) or `crontab -l` (list).
* **Format Structure**:
  ```
  *   *   *   *   *   Command to run
  │   │   │   │   │
  │   │   │   │   └─── Day of week (0 - 6, 0=Sunday)
  │   │   │   └─────── Month of year (1 - 12)
  │   │   └─────────── Day of month (1 - 31)
  │   └─────────────── Hour of day (0 - 23)
  └─────────────────── Minute (0 - 59)
  ```
  * *Example:* `0 3 * * * /opt/scripts/backup.sh` (Run every day at 3:00 AM).

---

### 3. Developer Power Tools

#### Persistent Terminal Sessions: `tmux`
`tmux` keeps shell sessions alive on a remote server even if your SSH connection drops.
* **Create Session**: `tmux new -s <session_name>`
* **Detach**: Press `Ctrl+b` then `d`. (Leaves programs running in the background).
* **Reattach**: `tmux attach -t <session_name>`
* **Layout Splitting**:
  * Vertical: `Ctrl+b` then `%`
  * Horizontal: `Ctrl+b` then `"`
  * Navigate: `Ctrl+b` then `Arrow Keys`

#### Terminal Text Editing: `vim`
`vim` is modal. Learn these commands for remote edits:
* **`i`**: Enter Insert mode.
* **`Esc`**: Return to Normal mode.
* **`:wq`**: Save and quit.
* **`:q!`**: Quit without saving.
* **`/pattern`**: Search for `pattern` in document. Press `n` for next occurrence.
* **`dd`**: Delete (cut) current line.
* **`yy`**: Yank (copy) current line.
* **`p`**: Paste line below cursor.

#### Package Managers
* **Ubuntu / Debian**: `sudo apt update && sudo apt install -y tmux`
* **CentOS / RHEL**: `sudo dnf install -y tmux`

---

## 🛠️ Outage Troubleshooting Drills

### 🚨 Drill A: "Disk space is full!"
You receive a pager alert that a server's disk usage has hit 100%.

1. **Find which partition is full:**
   ```bash
   df -h
   ```
2. **Find the culprit directory containing large files:**
   ```bash
   # Sort and find the top 10 largest folders under root /
   sudo du -ah / 2>/dev/null | sort -rh | head -n 10
   ```
3. **If large logs were deleted but space was not released:**
   Linux holds files in memory if a running process (like Nginx) still has an open file handle to them.
   ```bash
   # List deleted files that processes are holding open:
   sudo lsof | grep deleted
   ```
4. **Fix:** Restart the process holding the deleted file handle to release disk space.
   ```bash
   sudo systemctl restart nginx
   ```

---

### 🚨 Drill B: "Cannot connect to remote service"
Your app cannot connect to a database or downstream microservice. Run down the network connectivity ladder:

```
[Local App] ──1. Resolves DNS?──> [2. Ping IP?] ──3. Port Open?──> [4. Allowed by Firewall?]
```

1. **DNS Resolution Test:** Check if the host name resolves.
   ```bash
   host db-prod.internal   # or nslookup
   ```
2. **Network Connection Test:** Send ping signals to check if host is reachable.
   ```bash
   ping -c 3 db-prod.internal
   ```
3. **Port Binding Audit:** Verify if the remote service is actively listening on the target port.
   ```bash
   nc -zv db-prod.internal 5432  # Netcat port test (z = scan, v = verbose)
   # Or curl health endpoint
   curl -I http://api-server:8080/health
   ```
4. **Local Network Socket Check:** Verify if your server is blocked by a local firewall rule.
   ```bash
   sudo iptables -L -n  # List active firewall rules
   ```

---

### 🚨 Drill C: "Find a config string across all project files"
You need to find where a specific token is hardcoded in your project.
```bash
# Recursively search all files, show line numbers, ignore binary files:
grep -rn "API_TOKEN" ./src/

# Using find + xargs to inspect only JS/TS files:
find ./src -type f \( -name "*.js" -o -name "*.ts" \) | xargs grep -n "API_TOKEN"
```

---

## 🔑 Key Commands

| Command | Action | Use Case |
|:---|:---|:---|
| `cp <file> <dest>` | Copy local files | Create local file duplicates or backups. |
| `cp -r <dir> <dest>` | Copy local directories recursively | Duplicate complete asset folders. |
| `mv <src> <dest>` | Move or rename local files/dirs | Reorganize project paths or rename config files. |
| `ssh -i <key> <user>@<ip>`| Secure connection using private key | Remote server administrative login. |
| `scp <file> user@ip:/path`| Secure remote copy from local to remote (upload)| Ship compiled .jar or .bin files to host. |
| `scp user@ip:/file <local>`| Secure remote copy from remote to local (download)| Fetch crash dumps or log files from server. |
| `ss -tlnp` | List TCP sockets actively listening | Verify if app bound successfully to port. |
| `lsof -i :8080` | List processes binding port 8080 | Find process causing "Address in use" error. |
| `curl -i <URL>` | GET request showing response headers | Debug HTTP API return codes (e.g., CORS, Content-Type). |
| `rsync -avz <src> <dest>`| Efficiently sync files over network | Transfer project assets or backups. |
| `set -euo pipefail` | Enable strict execution error rules | Prevent scripts from failing silently. |
| `crontab -e` | Edit user background schedule rules | Configure recurring cron jobs. |
| `nc -zv <host> <port>` | Netcat probe to test host port access | Check network firewall blocks. |
| `tmux attach -t <name>`| Reconnect to a tmux terminal session | Resume developer workflows after disconnect. |
| `host <domain>` | Check IP associated with host name | Check DNS configurations. |

---

## 💡 Interview Points

* **What is the difference between `0.0.0.0` and `127.0.0.1`?**
  `127.0.0.1` (localhost) is the loopback interface, allowing connection only from *inside* the same host. `0.0.0.0` binds to all available network adapters, allowing remote hosts to connect.
* **Why does SSH complain if a private key file has `775` permissions?**
  SSH mandates that private keys remain secure. Permissive flags (`775`) let group members and other users read the private key. SSH will refuse connection until permissions are restricted (e.g., `600`).
* **What are the three core flags in Bash Strict Mode?**
  * `set -e`: Exit if any command fails.
  * `set -u`: Exit if an unassigned variable is used.
  * `set -o pipefail`: Propagate pipeline command errors.
* **How do you find which process is listening on port `80`?**
  Use `sudo ss -tlnp | grep :80` or `sudo lsof -i :80`.
* **What is `tmux` and why is it useful for remote administration?**
  `tmux` is a terminal multiplexer. It allows running shell processes to survive network disconnects (since the tmux process runs independently of the SSH session). It also enables screen tiling.

---

## 🧪 Exercises to Try

### 1. Networking Sockets
1. Start a simple Python dummy server on port 9000:
   ```bash
   python3 -m http.server 9000 &
   ```
2. Verify it is listening using `ss -tlnp | grep :9000`.
3. Check process binding using `lsof -i :9000`.
4. Kill the server using `kill $(pgrep -f "http.server")`.

### 2. Shell Scripting and Strict Mode
Create a script named `backup.sh`:
```bash
#!/bin/bash
set -euo pipefail

# Create a backup folder
TARGET_DIR="/tmp/backup_target"
mkdir -p "$TARGET_DIR"

# Simulating a failing command. This should terminate the script due to set -e
ls /nonexistent_folder

echo "This message should NOT print because of strict mode!"
```
Make it executable (`chmod +x backup.sh`), run it, and verify that it terminates immediately at the error without printing the final message. Run `echo $?` to check the exit code.

### 3. Scheduling a Cron Task
1. Open your cron editor: `crontab -e`.
2. Add a line that runs every minute to write the current timestamp:
   ```cron
   * * * * * echo "heartbeat: $(date)" >> /tmp/heartbeat.log
   ```
3. Save, exit, and wait 2 minutes. Check `/tmp/heartbeat.log` to confirm execution.
4. Open `crontab -e` and remove the line to prevent storage bloat.
