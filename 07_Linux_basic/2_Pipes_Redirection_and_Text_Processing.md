# Chapter 2 — Pipes, Redirection & Text Processing

> **Status:** ✅ Completed
> **Goal:** Master input/output redirection, standard streams, command piping, and the essential text-processing tools (`grep`, `find`, `awk`, `sed`, etc.) to search code and analyze production logs.

---

## 📝 Notes

### 1. The Unix Philosophy: Everything is a Text Stream
In Linux, programs are designed to do **one thing well** and communicate with each other using plain text streams. This allows developers to chain simple utilities together to solve complex processing tasks.

---

### 2. Standard Streams & Redirection
Every process starts with three default input/output channels (file descriptors):
* **`stdin` (Standard Input - File Descriptor 0)**: Where the process reads input (default: keyboard).
* **`stdout` (Standard Output - File Descriptor 1)**: Where the process writes normal output (default: terminal).
* **`stderr` (Standard Error - File Descriptor 2)**: Where the process writes error messages (default: terminal).

#### Redirection Operators:
* `>`: Overwrite standard output to a file.
* `>>`: Append standard output to a file.
* `<`: Feed a file into standard input of a command.

```bash
echo "starting server" > server.log   # Overwrites server.log
echo "port bound to 8080" >> server.log # Appends to server.log
mysql db_name < backup.sql             # Feeds backup file into MySQL
```

---

### 3. Redirecting Standard Error (stderr)
By default, redirecting with `>` or `>>` only redirects `stdout`. Errors will still leak onto your screen because they are written to `stderr` (FD 2).
* **Redirecting stderr only (`2>`)**: Useful to isolate errors to a separate log.
  ```bash
  npm run start 2> error.log
  ```
* **Merging stderr into stdout (`2>&1`)**: Merges both streams so they can be written to the same file or piped together.
  ```bash
  npm run start > output.log 2>&1   # Redirects BOTH stdout and stderr to output.log
  ```
* **Discarding output (`/dev/null`)**: The system's "black hole." Anything written here is discarded instantly.
  ```bash
  find / -name "config.json" 2> /dev/null  # Suppresses "Permission Denied" errors
  ```

---

### 4. Pipelines (`|`)
The pipe operator `|` takes the `stdout` of the command on its left and feeds it directly into the `stdin` of the command on its right.
```
[command_1] ──stdout──> [ | ] ──stdin──> [command_2]
```
```bash
cat access.log | grep "500"  # Read file, stream contents to grep, grep filters for "500"
```

---

### 5. Splitting Streams with `tee`
Sometimes you want to save log output to a file *and* monitor it live on the screen at the same time. The `tee` command acts as a T-splitter for text streams:
```bash
npm run start | tee server.log  # Output is printed to screen AND saved to server.log
```

---

### 6. Passing Arguments with `xargs`
Some commands (like `rm`, `kill`, or `mkdir`) do not accept stream input (`stdin`); they require arguments typed directly on the command line. `xargs` bridges this gap by converting standard input into command-line arguments.
```bash
# Find all temp files and delete them:
find /tmp -name "*.tmp" | xargs rm
```

---

### 7. Logical Chains & Command Substitution
* **`&&` (AND)**: Run the second command *only* if the first command succeeds (exit code 0).
  ```bash
  npm run build && npm run start  # Start only if build succeeds
  ```
* **`||` (OR)**: Run the second command *only* if the first command fails (non-zero exit code).
  ```bash
  ping -c 1 server || echo "Server is offline"
  ```
* **`;` (Semicolon)**: Run commands sequentially, regardless of success or failure.
* **`$()` (Command Substitution)**: Run a command inside another command and insert its output.
  ```bash
  kill $(pgrep node)  # Find the Process IDs of Node, and kill them
  ```

---

### 8. The Text Processing Toolkit

#### A. Reading Files
Before processing text, you must view it:
* `cat <file>`: Dumps the entire file to the screen (best for small files).
* `head -n <N>`: View the first N lines of a file.
* `tail -n <N>`: View the last N lines.
  * **`tail -f <file>`**: Follow file additions in real-time (essential for watching live logs).

#### B. Pattern Search (`grep`)
Finds lines in a file or stream that match a pattern.
* `grep "pattern" file`: Simple search.
* `grep -i "pattern"`: Case-insensitive search.
* `grep -v "pattern"`: Invert match (shows lines that *do not* contain the pattern).
* `grep -rn "pattern" dir/`: Recursively search all files in directory and show line numbers.
* `grep -E "regex"`: Search using Extended Regular Expressions.
* `grep -A 3 -B 1 "pattern"`: Show 3 lines *After* and 1 line *Before* the match (adds context to errors).

#### C. Find Files (`find`)
Locate files in the directory tree based on meta-information.
```bash
find /var/log -name "*.log"            # Find files ending in .log
find . -type f -mtime -1                # Find files modified in the last 24 hours
find . -type f -size +100M              # Find files larger than 100MB
```

#### D. Extraction & Deduplication (`cut`, `sort`, `uniq`)
* `cut -d',' -f2`: Extract columns from structured text. `-d` sets the delimiter, `-f` selects the column index.
* `sort`: Sorts lines of text alphabetically or numerically (`-n`).
* `uniq`: Removes duplicate adjacent lines.
  * **`uniq -c`**: Counts occurrences of each unique line. **Note:** `uniq` only checks consecutive lines; you must `sort` the input first!
  ```bash
  cat ips.txt | sort | uniq -c | sort -nr  # Count unique IPs and sort by frequency descending
  ```

#### E. Text Counting (`wc`)
Counts statistics of lines, words, and characters.
* `wc -l`: Counts lines (very common for counting matching occurrences).
  ```bash
  grep "ERROR" production.log | wc -l  # Count how many errors occurred
  ```

#### F. Stream Editor (`sed`)
Used for quick search-and-replace transformations in a stream.
* Syntax: `sed 's/find/replace/g' file`
  ```bash
  cat config.env | sed 's/DB_PORT=5432/DB_PORT=5433/g' > config.new.env
  ```

#### G. Report Writer / Column Processor (`awk`)
A powerful line-oriented language for scanning columns. It breaks each line into columns delimited by whitespace (by default), accessed via `$1`, `$2`, etc. `$0` represents the whole line.
```bash
# Print only the 1st and 4th columns of process list:
ps aux | awk '{print $1, $4}' 

# Print lines where the 9th column (HTTP status code) is 500:
cat access.log | awk '$9 == 500 {print $0}'
```

---

## 🔑 Key Commands & Operators

| Command/Operator | Action | Use Case |
|:---|:---|:---|
| `\|` | Pipe: passes stdout of left command as stdin to right | `ps aux \| grep node` |
| `>` | Redirect stdout, overwriting file | `echo "reset" > flag.txt` |
| `>>` | Redirect stdout, appending to file | `echo "log entry" >> audit.log` |
| `2>&1` | Merge stderr (2) into stdout (1) | `cmd > all.log 2>&1` |
| `tee <file>` | Split output to console and file | `make build \| tee build.log` |
| `xargs <cmd>` | Turn stdin stream into command arguments | `find . -name "*.log" \| xargs rm` |
| `grep -rn <str> <dir>`| Search directory recursively for string with line numbers | `grep -rn "DB_CONNECTION" ./src` |
| `tail -f <file>` | Monitor additions to a file in real-time | `tail -f /var/log/nginx/error.log` |
| `find <dir> -size +50M`| Locate files larger than 50MB | `find /var/log -size +50M` |
| `cut -d' ' -f1` | Slice columns out of text using space delimiter | Extract IP addresses from access logs. |
| `sort -rn` | Sort lines numerically in reverse order | Sort top counts descending. |
| `uniq -c` | Group consecutive lines and print counts | Find unique items and their counts. |
| `wc -l` | Count number of lines | `ls /var/log \| wc -l` (count files) |
| `sed 's/old/new/g'`| Replace all instances of 'old' with 'new' | Replace localhost links in dynamic configurations. |
| `awk '{print $1}'` | Print specified columns of stream | Print IP (first column) of access logs. |

---

## 💡 Interview Points

* **What are standard streams, and how do you redirect errors?**
  Processes have `stdin` (0), `stdout` (1), and `stderr` (2). To redirect error output to a file, use `2> filename`. To merge it into stdout, use `2>&1`.
* **What is the difference between `>` and `>>`?**
  `>` truncates (overwrites) the target file; `>>` appends new data to the end of the file.
* **Why must you use `sort` before running `uniq`?**
  `uniq` only compares adjacent consecutive lines. If the duplicate lines are spread out, `uniq` will not detect them unless the file is first ordered using `sort`.
* **How do you search for a text string recursively across directories?**
  Use `grep -rn "search_string" /path/to/directory` (where `-r` is recursive, and `-n` prints line numbers).
* **What is `xargs` used for?**
  It reads items from standard input and constructs command lines to execute, allowing commands that don't accept piped input (like `rm` or `kill`) to receive data from standard output streams.

---

## 🧪 Real-World Log Analysis Playbooks

### 🚒 Debugging Production Outages

#### 1. How many errors occurred in today's logs?
```bash
grep -c "ERROR" /var/log/myapp/production.log
```

#### 2. Get the last 20 errors with context to see the stack trace:
```bash
grep -rn -A 5 -B 2 "NullPointerException" /var/log/myapp/production.log | tail -n 20
```

#### 3. Find which config files contain the database address:
```bash
grep -rn "DB_HOST" /etc/myapp/
```

#### 4. Find the top 5 most common error patterns in log:
```bash
# Extract the error message column, sort them, count duplicates, sort by frequency, show top 5
grep "ERROR" /var/log/myapp/production.log | awk -F' - ' '{print $2}' | sort | uniq -c | sort -nr | head -n 5
```

#### 5. Find log files taking up more than 100MB under `/var/log`:
```bash
find /var/log -type f -size +100M -name "*.log"
```

#### 6. Watch the live log stream filtering out noise:
```bash
tail -f /var/log/nginx/access.log | grep -v "status:200"
```

#### 7. Find who is hitting the Nginx API server the most:
```bash
# Nginx log starts with client IP. Extract column 1, count, sort by hit count descending
cat /var/log/nginx/access.log | awk '{print $1}' | sort | uniq -c | sort -nr | head -n 10
```

---

## 🧪 Exercises to Try

Create a test environment (e.g., `docker run -it --rm ubuntu bash`). Set up a dummy log file to practice on:
```bash
mkdir -p /tmp/practice && cd /tmp/practice
cat << 'EOF' > app.log
2026-06-20 10:00:01 INFO - Request received from 192.168.1.50
2026-06-20 10:00:05 ERROR - Database connection timeout (host: db-prod-01)
2026-06-20 10:00:10 INFO - Request received from 192.168.1.52
2026-06-20 10:00:12 ERROR - Out of memory in JVM heap
2026-06-20 10:00:15 INFO - Request received from 192.168.1.50
2026-06-20 10:00:20 WARNING - Slow response from API downstream (took 2.5s)
2026-06-20 10:00:25 ERROR - Database connection timeout (host: db-prod-01)
2026-06-20 10:00:30 INFO - Request received from 192.168.1.50
EOF
```

### Try these commands on the generated `app.log`:
1. Use `grep` to filter only the `ERROR` lines.
2. Count the number of `ERROR` logs using `wc -l`.
3. Filter out all `INFO` logs using `grep -v`.
4. Extract the IP addresses of users hitting the app (hint: `grep "Request received" app.log | awk '{print $NF}'`).
5. Find the unique client IPs that visited, sorted by frequency (hint: pipe IP list to `sort | uniq -c | sort -nr`).
6. Replace all instances of `db-prod-01` with `db-backup-01` using `sed` and write output to `app_debug.log`.
7. List all files inside `/tmp` that are smaller than 10KB (using `find`).
