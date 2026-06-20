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

🧠 **What is grep?**

👉 `grep` searches text files or streams for lines that match a specific pattern or regular expression.

🎯 **Basic Syntax**
```bash
grep [options] "pattern" [file]
```

Example:
```bash
grep "ERROR" production.log
```

👉 **Means:**
* `grep` ── the search command
* `"ERROR"` ── the condition/pattern to search for
* `production.log` ── where to search

🔍 **Step-by-Step Understanding**

1. **📂 Inputs**
   * Search a single file: `grep "pattern" file.txt`
   * Search recursively inside a directory: `grep -r "pattern" ./src`
   * Search a standard input stream: `cat file.txt | grep "pattern"`

2. **🔎 Options (filters)**
   * 🔸 **Case-insensitive**: `grep -i "error"` (matches ERROR, error, Error)
   * 🔸 **Invert match (exclude)**: `grep -v "INFO"` (shows lines that *do not* contain "INFO")
   * 🔸 **Count occurrences**: `grep -c "ERROR"` (outputs the number of matching lines)
   * 🔸 **Line numbers**: `grep -n "NullPointer"` (displays the matching line number)
   * 🔸 **Regex search**: `grep -E "[0-9]{3}-[0-9]{3}"` (enables Extended Regular Expressions)

3. **⚡ Context Controls (Debugging Lifesaver)**
   * 🔸 **Show lines AFTER**: `grep -A 3 "ERROR"` (shows the matching line and 3 lines after it)
   * 🔸 **Show lines BEFORE**: `grep -B 2 "ERROR"` (shows the matching line and 2 lines before it)
   * 🔸 **Show lines BOTH**: `grep -C 2 "ERROR"` (shows the matching line, 2 lines before, and 2 lines after)

---

#### C. Find Files (`find`)

🧠 **What is find?**

👉 `find` searches for files and directories within a directory hierarchy based on user-defined conditions (name, type, size, time, etc.).

🎯 **Basic Syntax**
```bash
find <path> <conditions> <actions>
```

Example:
```bash
find . -name "file.txt"
```

👉 **Means:**
* `.` ── search in the current directory and all subdirectories
* `-name` ── the condition/filter type
* `"file.txt"` ── what name pattern to search for

🔍 **Step-by-Step Understanding**

1. **📂 Where to search (path)**
   * `find .` ── search in the current directory
   * `find /home` ── search in a specific path
   * `find /` ── search the entire filesystem (slow!)

2. **🔎 Conditions (filters)**
   * 🔸 **By name**: `find . -name "app.js"`
   * 🔸 **Case-insensitive name**: `find . -iname "app.js"`
   * 🔸 **Wildcard name matching**: `find . -name "*.log"`
   * 🔸 **By type**:
     * `find . -type f` ── find files only
     * `find . -type d` ── find directories only
     * `find . -type l` ── find symbolic links only
   * 🔸 **By size**:
     * `find . -size +10M` ── files larger than 10 Megabytes
     * `find . -size -1M` ── files smaller than 1 Megabyte
   * 🔸 **By modification time**:
     * `find . -mtime -1` ── modified in the last 24 hours
     * `find . -mtime +7` ── older than 7 days
   * 🔸 **By permissions**: `find . -perm 644`

3. **⚡ Combine Conditions (AND / OR)**
   * 🔸 **AND (default)**: `find . -type f -name "*.log"` (files AND ending with .log)
   * 🔸 **OR**: `find . \( -name "*.js" -o -name "*.ts" \)` (files ending in .js OR .ts)

4. **🔥 Actions (what to do after finding)**
   * 🔸 **Default**: Just print matching paths.
   * 🔸 **Delete**: `find . -name "*.log" -delete` ⚠️ (deletes matching files instantly)
   * 🔸 **Execute command**: `find . -name "*.log" -exec rm {} \;`
     * `{}` ── placeholder representing each found file path
     * `\;` ── marks the end of the `-exec` command
   * 🚀 **Faster batch execution**: `find . -name "*.log" -exec rm {} +` (executes in batches, much better performance for large lists)

---

#### D. Extraction & Deduplication (`cut`, `sort`, `uniq`)

🧠 **What are cut, sort, and uniq?**

👉 `cut` extracts sections of lines from files. `sort` orders lines of text. `uniq` filters out or counts duplicate lines.

🎯 **Basic Syntax**
```bash
cut -d"<delimiter>" -f<column_number> [file]
sort [options] [file]
uniq [options] [file]
```

🔍 **Step-by-Step Understanding**

1. **📂 Column Slicing (`cut`)**
   * `cut -d',' -f2 file.csv`
     * `-d','` ── delimiter is a comma (breaks line by commas)
     * `-f2` ── select field/column 2

2. **🔎 Ordering (`sort`)**
   * `sort file.txt` ── alphabetical sort
   * `sort -n file.txt` ── numeric sort (correctly orders numbers, e.g. 2 before 10)
   * `sort -r file.txt` ── reverse order sort
   * `sort -nr file.txt` ── numeric reverse sort (largest values at top)

3. **⚡ Duplicate Management (`uniq`)**
   * `uniq file.txt` ── removes adjacent duplicate lines
   * `uniq -c file.txt` ── prefixes lines with their occurrence count
   * `uniq -d file.txt` ── prints ONLY duplicate lines
   * ⚠️ **WARNING:** `uniq` only checks **adjacent consecutive lines**. You must always sort the data first!
     ```bash
     cat ips.txt | sort | uniq -c | sort -nr  # Count unique IPs and sort by frequency descending
     ```

---

#### E. Text Counting (`wc`)

🧠 **What is wc?**

👉 `wc` (Word Count) prints newline, word, and byte counts for files or text streams.

🎯 **Basic Syntax**
```bash
wc [options] [file]
```

🔍 **Step-by-Step Understanding**

1. **🔎 Options (counters)**
   * 🔸 **Count lines**: `wc -l file.txt` (the most commonly used flag for pipelines)
   * 🔸 **Count words**: `wc -w file.txt`
   * 🔸 **Count bytes**: `wc -c file.txt`
   * 🔸 **Count characters**: `wc -m file.txt`

2. **⚡ Common Pipe Combinations**
   * `grep "ERROR" app.log | wc -l` ── counts how many error events occurred
   * `ls /var/log | wc -l` ── counts how many files/directories are in the logs folder

---

#### F. Stream Editor (`sed`)

🧠 **What is sed?**

👉 `sed` is a stream editor used to perform basic text transformations (like search and replace) on input streams or files.

🎯 **Basic Syntax**
```bash
sed 's/<search>/<replace>/<flags>' [file]
```

Example:
```bash
sed 's/localhost/127.0.0.1/g' config.env
```

👉 **Means:**
* `s` ── substitute action
* `/localhost/` ── pattern to find
* `/127.0.0.1/` ── string to replace it with
* `/g` ── global flag (replace all matches on a line, not just the first one)

🔍 **Step-by-Step Understanding**

1. **📂 In-Place File Editing**
   * By default, `sed` outputs changes to stdout. To save changes back to the original file:
     `sed -i 's/foo/bar/g' config.env`  # -i = in-place edit (overwrites the file!)

2. **🔎 Multiple Transformations**
   * Use `-e` to chain operations:
     `sed -e 's/foo/bar/g' -e 's/apple/orange/g' config.env`

3. **⚡ Target Specific Lines**
   * Substitute only on line 5: `sed '5s/foo/bar/g' file.txt`
   * Substitute only on lines 1 through 10: `sed '1,10s/foo/bar/g' file.txt`

---

#### G. Report Writer & Column Parser (`awk`)

🧠 **What is awk?**

👉 `awk` is a powerful column processing language. It treats each line as a record and splits it into columns (fields) delimited by whitespace by default.

🎯 **Basic Syntax**
```bash
awk 'pattern { action }' [file]
```

Example:
```bash
awk '{print $1, $3}' access.log
```

👉 **Means:**
* `{print $1, $3}` ── the action (no pattern specified, so it runs on all lines)
* `$1` ── column 1
* `$3` ── column 3
* columns are split by spaces or tabs

🔍 **Step-by-Step Understanding**

1. **📂 Built-in Variable References**
   * `$0` ── the entire line
   * `$1`, `$2`, `$3` ── columns 1, 2, and 3
   * `NF` ── Number of Fields (columns) in the current line. `$NF` points to the *last* column!
   * `NR` ── Number of Records (line number of the current line)

2. **🔎 Custom Separators**
   * Use `-F` to define a custom field separator (e.g. comma, colon):
     * `awk -F',' '{print $2}' file.csv` (extract 2nd column of CSV)
     * `awk -F':' '{print $1}' /etc/passwd` (extract first column of username list)

3. **⚡ Conditional Parsing**
   * Run action only if column matches value:
     `awk '$9 == 500 {print $0}' access.log` (print line if 9th column is status code 500)
   * Run action if column matches regex:
     `awk '$3 ~ /ERROR/ {print $1}' app.log` (print 1st column if 3rd column contains "ERROR")

4. **🔥 Calculations and Accumulations**
   * Sum up a column:
     `awk '{sum += $5} END {print sum}' data.txt` (adds up values in column 5, prints total at the end)

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
