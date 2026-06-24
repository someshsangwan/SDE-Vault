# Chapter 2 — CPU Scheduling

> Interview-focused notes. Covers: why scheduling exists, key metrics, preemptive vs
> non-preemptive, all scheduling algorithms with examples and Gantt charts, starvation,
> aging, convoy effect, and hands-on macOS commands to inspect priorities live.

---

## PART A — Why Do We Need Scheduling?

The CPU can run **only one process per core at a time**. But many processes sit in the
**Ready state** simultaneously, all wanting the CPU.

Someone must decide: **"Which ready process gets the CPU next?"**

That decision-maker = **CPU Scheduler** (short-term scheduler).
The strategy it uses = **Scheduling Algorithm**.

```
   READY QUEUE (waiting for CPU)
   ┌────┐ ┌────┐ ┌────┐ ┌────┐
   │ P1 │ │ P2 │ │ P3 │ │ P4 │  ────►  [ CPU ]  ────►  running
   └────┘ └────┘ └────┘ └────┘
              ▲
       Scheduler picks who goes next
```

> **Backend connection:** A thread pool executor / task queue does the same thing —
> picks the next task from a queue of waiting tasks. Same algorithms (FIFO, priority)
> you've used in `ExecutorService` or message queues are CPU scheduling concepts.

---

## PART B — Key Metrics (must know for numerical questions)

Imagine a process arrives, waits, runs, finishes. We measure:

| Term | Meaning | Formula |
|---|---|---|
| **Arrival Time (AT)** | when process enters the ready queue | — |
| **Burst Time (BT)** | how much CPU time the process needs | — |
| **Completion Time (CT)** | when the process finishes | — |
| **Turnaround Time (TAT)** | total time from arrival to completion | **CT − AT** |
| **Waiting Time (WT)** | time spent sitting idle in the queue | **TAT − BT** |
| **Response Time (RT)** | time from arrival until it FIRST gets the CPU | first\_run − AT |

```
AT       first runs              CT
│            │                    │
▼            ▼                    ▼
├────wait────┼──────run (BT)──────┤
│◄─Response Time─►                │
│◄────── Turnaround Time ────────►│
     Waiting Time = TAT − BT
```

### Goal of a good scheduler:
- ⬆️ Maximize: **CPU utilization**, **throughput** (processes completed per unit time)
- ⬇️ Minimize: **waiting time**, **turnaround time**, **response time**

---

## PART C — Preemptive vs Non-Preemptive (critical distinction)

| | **Non-Preemptive** | **Preemptive** |
|---|---|---|
| Once a process gets CPU... | keeps it until it finishes or blocks | OS can **forcibly take** CPU away mid-run |
| Switch happens when | process finishes or goes to Waiting | also on timer expiry or higher-priority arrival |
| State transition | Running → Waiting/Terminated only | also Running → **Ready** (the preemption arrow!) |
| Risk | one long job blocks everyone | more context switching overhead |

> Remember **Running → Ready** from Chapter 1's state diagram? That arrow ONLY exists in
> preemptive scheduling. It's the OS yanking the CPU back mid-execution.

> Modern OSes (Linux, macOS, Windows) are all **preemptive** — otherwise one infinite loop
> would freeze your whole machine forever.

---

## PART D — Scheduling Algorithms

### 1. FCFS — First Come First Served
**Idea:** Whoever arrives first, runs first. Plain FIFO queue. **Non-preemptive.**

**Example:**
| Process | AT | BT |
|---|---|---|
| P1 | 0 | 24 |
| P2 | 0 | 3  |
| P3 | 0 | 3  |

```
Gantt Chart:
│        P1        │ P2 │ P3 │
0                 24   27   30

Waiting Times:  P1=0,  P2=24, P3=27
Average Waiting Time = (0+24+27)/3 = 17
```

- ✅ Simple, no starvation (everyone eventually runs).
- ❌ **Convoy Effect** — one long process holds up all short ones behind it.
  (Like one slow truck on a single-lane road blocking all fast cars behind it.)

---

### 2. SJF — Shortest Job First
**Idea:** Pick the process with the **smallest burst time** next. **Non-preemptive.**
Goal = minimize average waiting time.

**Example (same processes, but pick shortest first):**
```
Gantt Chart:
│ P2 │ P3 │        P1        │
0    3    6                  30

Waiting Times:  P2=0,  P3=3,  P1=6
Average Waiting Time = (0+3+6)/3 = 3   ← much better than FCFS's 17!
```

- ✅ **Provably optimal** — gives the minimum possible average waiting time.
- ❌ **Starvation** — a long job may wait forever if short jobs keep arriving.
- ❌ **Impractical** — you can't know burst time in advance (would need to predict future).
  Real systems *estimate* it from past behavior.

---

### 3. SRTF — Shortest Remaining Time First
**Idea:** Preemptive version of SJF. If a new process arrives with **shorter remaining
time** than the current running one → **preempt** (kick it out), run the new shorter one.

**Example:**
| Process | AT | BT |
|---|---|---|
| P1 | 0 | 8 |
| P2 | 1 | 4 |
| P3 | 2 | 2 |

```
Gantt Chart:
│P1│P2│P3│P2│  P1  │
0  1  2  4  6      14

At t=1: P2 arrives (BT=4), P1 has 7 left → P2 shorter, preempt P1 → run P2
At t=2: P3 arrives (BT=2), P2 has 3 left → P3 shorter, preempt P2 → run P3
At t=4: P3 done, P2 has 3 left, P1 has 7 left → P2 resumes
At t=6: P2 done → P1 resumes, runs to completion

Waiting Times:
  P1: (t=1 preempted wait) + (t=6 to 6 = 0) → TAT=(14-0)=14, WT=14-8=6
  P2: TAT=(6-1)=5, WT=5-4=1
  P3: TAT=(4-2)=2, WT=2-2=0
Average Waiting Time = (6+1+0)/3 = 2.33
```

- ✅ Better average waiting time than SJF.
- ❌ **Starvation** (same as SJF), high context-switching overhead, needs burst estimate.

---

### 4. Round Robin (RR) ★ Most important practically
**Idea:** Each process gets a fixed **time quantum** (e.g., 4ms). Run for that quantum →
**preempt** → send to back of queue → pick next. Cycle through everyone. **Preemptive.**

**Example (quantum = 4):**
| Process | AT | BT |
|---|---|---|
| P1 | 0 | 10 |
| P2 | 0 | 4  |
| P3 | 0 | 5  |

```
Gantt Chart:
│ P1 │ P2 │ P3 │ P1 │P3│P1│
0    4    8   12   16 17 19

Round 1: P1 runs 4 (6 left), P2 runs 4 (done), P3 runs 4 (1 left)
Round 2: P1 runs 4 (2 left), P3 runs 1 (done), P1 runs 2 (done)

Waiting Times:
  P1: 19-10 = 9 wait
  P2: 4-4 = 0 wait
  P3: 12-5 = 7 wait  → TAT=(17-0)=17, WT=17-5=12... etc.
Average Waiting Time = (9+0+6)/3 = 5 (approx)
```

- ✅ **Fair** — every process gets a turn, no starvation.
- ✅ Great **response time** — nobody waits too long for first turn.
- ⚠️ **Quantum size is critical:**
    - Too **large** → behaves like FCFS (convoy effect returns).
    - Too **small** → too many context switches (overhead eats CPU!).
    - Rule of thumb: quantum should be slightly larger than a typical interaction/burst.

> **Backend connection:** Round Robin load balancing (nginx, Kubernetes distributing
> requests evenly across servers) = exact same idea applied to servers instead of CPU.

---

### 5. Priority Scheduling
**Idea:** Each process gets a **priority number**. Highest priority runs first.
Can be preemptive or non-preemptive.

- ✅ Important jobs go first (matches real-world needs).
- ❌ **Starvation** — low-priority processes may NEVER run if high-priority ones keep arriving.
- ✅ **Fix: Aging** — gradually increase priority of long-waiting processes over time.
  Eventually even the lowest-priority job rises high enough to run.

> SJF is actually just priority scheduling where **priority = 1 / burst-time**.

---

### 6. Multilevel Queue Scheduling
**Idea:** Multiple **separate queues** for different process types, each with its own algorithm.

```
┌─ System processes        (highest priority) ── Round Robin
├─ Interactive processes                        ── Round Robin
├─ Interactive editing processes               ── Round Robin
├─ Batch processes                             ── FCFS
└─ Student processes       (lowest priority)  ── FCFS
```

- Process is **permanently assigned** to one queue (based on process type).
- Higher queues always preempt lower queues.
- ❌ Rigid — a process stuck in a low-priority queue can starve.

---

### 7. Multilevel Feedback Queue (MLFQ) — The Realistic One
**Idea:** Like multilevel queue, but processes can **move between queues** based on their behavior:
- Uses too much CPU (CPU-bound, long bursts) → **demote** to a lower-priority queue.
- Waits a lot / interactive (short bursts) → **promote** to higher-priority queue.
- A process waiting too long in a low queue → **aging** bumps it up.

```
Q0 (quantum=8ms)   ──► if not done, move down
Q1 (quantum=16ms)  ──► if not done, move down
Q2 (FCFS)          ──► run to completion
```

- ✅ Adapts to process behavior — no need to know burst time in advance.
- ✅ Interactive jobs get quick response, batch jobs run in background.
- ✅ Prevents starvation via aging.
- ✅ **Closest to what real OS schedulers actually do.**
- ❌ Complex to configure (how many queues? what quantum? aging threshold?).

---

## PART E — The Three "Gotcha" Terms

### Starvation
A process waits **indefinitely** because others keep getting picked ahead of it.
- Happens in: SJF (long jobs starve), Priority (low-priority starves).
- A low-priority process may wait for **hours or days** in theory.

### Aging (the fix for starvation)
Gradually **increase the priority** of a process that has been waiting a long time.
- Eventually, even the lowest-priority job rises high enough to run.
- Example: every 15 minutes of waiting → +1 priority bump.

### Convoy Effect (FCFS's flaw)
One **long process** at the front of the queue holds up all **short processes** behind it.
- Like one slow truck on a single-lane road blocking all fast cars.
- Average waiting time balloons (we saw this: FCFS gave avg wait=17, SJF gave 3!).

---

## PART F — Solved Numerical Example (for written tests)

**Problem:** Given these 4 processes, compute avg waiting time for FCFS and SJF:

| Process | AT | BT |
|---|---|---|
| P1 | 0 | 6 |
| P2 | 1 | 4 |
| P3 | 2 | 2 |
| P4 | 3 | 1 |

### FCFS Solution (non-preemptive, arrival order):
```
Gantt:  │  P1  │ P2 │P3│P4│
        0      6   10  12  13

CT:  P1=6, P2=10, P3=12, P4=13
TAT: P1=6-0=6, P2=10-1=9, P3=12-2=10, P4=13-3=10
WT:  P1=6-6=0, P2=9-4=5,  P3=10-2=8,  P4=10-1=9

Average WT = (0+5+8+9)/4 = 5.5
```

### SJF Solution (non-preemptive, pick shortest burst from arrived processes):
```
t=0: only P1 arrived → run P1 (BT=6)
t=6: P2,P3,P4 all arrived → pick shortest = P4 (BT=1)
t=7: pick next shortest = P3 (BT=2)
t=9: pick next = P2 (BT=4)

Gantt:  │  P1  │P4│P3│ P2 │
        0      6  7  9   13

CT:  P1=6, P4=7, P3=9, P2=13
TAT: P1=6-0=6, P4=7-3=4, P3=9-2=7, P2=13-1=12
WT:  P1=6-6=0, P4=4-1=3, P3=7-2=5, P2=12-4=8

Average WT = (0+3+5+8)/4 = 4   ← better than FCFS's 5.5
```

**How to solve any scheduling problem:**
1. Draw a timeline / Gantt chart step by step.
2. Fill in CT for each process from the chart.
3. Compute TAT = CT − AT for each.
4. Compute WT = TAT − BT for each.
5. Average the WTs.

---

## PART G — Wait, Does the Scheduler Pick PROCESSES or THREADS? (my question)

**Short answer: Modern OSes schedule THREADS, not processes.**

Textbooks (and this chapter) say "process" because historically, before threads existed,
processes were the only unit of execution. But the precise truth is:

> **The CPU scheduler picks a THREAD to run. The process is just a container
> (memory, resources). The thread is the actual unit of work that gets scheduled.**

When a process has only **one thread** (old-style programs), saying "scheduler picks the
process" and "scheduler picks the thread" mean the same thing — there's only one thread
inside. But modern programs have many threads, and the scheduler picks **individual threads**:

```
┌─────────── Process: java (PID 90884) ───────────┐
│  Thread 1  │  Thread 2  │  Thread 3  │  ...218  │
└─────────────────────────────────────────────────┘
       ▼             ▼
  [CPU Core 1]  [CPU Core 2]   ← scheduler picked Thread1 AND Thread2
                                  NOT the whole process
```

### What the scheduler actually sees
The scheduler maintains a **ready queue of THREADS** (not processes).
Each thread has its own state (Ready/Running/Waiting), priority, stack, registers, and PC
— exactly what we covered in Chapter 1. The process itself never "runs" — its threads do.

```
READY QUEUE (threads waiting for CPU):
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│Chrome    │ │Java      │ │IntelliJ  │ │Zoom      │
│Thread 4  │ │Thread 12 │ │Thread 7  │ │Thread 2  │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
                    ▼  scheduler picks
              [ CPU Core ]
```

### Context switch cost: same-process threads are cheaper
A context switch between two threads of the **same process** is **cheaper** than between
threads of **different processes** — because memory (heap, code, data) is already shared,
so no memory context needs to change. Only stack + registers + PC swap out.

### Why textbooks still say "process"
1. **Historical** — threads came later; early OS theory only had processes.
2. **Simplification** — for teaching scheduling algorithms (FCFS, RR etc.), the math/logic
   is identical whether you call it a process or thread.
3. **Linux internally** — Linux calls everything a `task_struct` internally. Both processes
   and threads use the same structure. `fork()` (new process) and creating a new thread
   both create a `task_struct` — the only difference is whether they share memory or not.
   The scheduler just sees a flat list of tasks/threads.

### Updated mental model

| What textbooks say | More precise truth |
|---|---|
| "OS schedules processes" | OS schedules **threads** (1-thread process = same thing) |
| "Process goes to Ready queue" | Process's **threads** go to the ready queue |
| "Context switch between processes" | Switch between **threads** (heavier if diff process) |
| "CPU picks next process" | CPU picks next **thread** to execute |

### Sanity check: threads on your machine right now
```
Processes: 904 total, 2 running, 902 sleeping, 4955 threads

PID    #TH    COMMAND
0      871    kernel_task
90884  221    java         ← 221 threads = 221 independent scheduler entries
90818  130    idea
72536  68     zoom.us
```
> 4955 threads across 904 processes — that's how many schedulable units the OS manages,
> spreadacross 12 cores. The "java" process has 221 threads: 221 entries in the ready
> queue, not 1.

**One-liner for the interview:**
> *"Strictly speaking, the CPU scheduler schedules kernel threads, not processes. A process
> is a container of resources — its threads are the actual execution units. When people say
> 'the scheduler picks a process,' they mean one of that process's threads."*

---

## PART H — Algorithm Comparison (interview cheat-sheet)

| Algorithm | Preemptive? | Picks next by | Main pro | Main con |
|---|---|---|---|---|
| **FCFS** | No | arrival order | simple, no starvation | convoy effect |
| **SJF** | No | shortest burst | optimal avg wait | starvation, needs burst |
| **SRTF** | Yes | shortest remaining | best avg wait | starvation, overhead |
| **Round Robin** | Yes | time quantum (cyclic) | fair, great response | quantum tuning needed |
| **Priority** | Either | priority number | important jobs first | starvation (fix: aging) |
| **Multilevel Queue** | Either | queue class | separates job types | rigid, starvation |
| **MLFQ** | Yes | adaptive behavior | flexible, realistic | complex to tune |

---

## 🖥️ HANDS-ON: Inspecting Scheduling on macOS

### See process priorities live
```bash
ps -axo pid,pri,ni,stat,%cpu,comm | sort -nrk5 | head -10
```

Columns:
- **PRI** = priority (higher number = more important to scheduler).
- **NI** = "nice" value — ranges from **-20** (highest priority, "not nice to others")
  to **+19** (lowest priority, "very nice to others").
- **STAT** — notice `N` in the code (e.g., `SN`) = low-priority / nice process.

### Real output from this machine:
```
WindowServer  PRI=79  NI=0   ← UI must stay responsive → highest priority
claude        PRI=31  NI=0
idea          PRI=4   NI=0
acumbrellaagent PRI=20 NI=10  ← NI=10 → deliberately low priority, STAT shows 'N'
```
> `WindowServer` gets PRI=79 because the OS knows UI responsiveness is critical →
> this is **Priority Scheduling** at work in your real OS.

### Control priority yourself:
```bash
nice -n 10 ./myscript.sh      # start a process with low priority (nice to others)
renice -n -5 -p <PID>         # increase priority of a running process (need sudo)
renice -n 10 -p <PID>         # lower priority of a running process
```

### How many things can truly run in parallel?
```bash
sysctl -n hw.ncpu             # logical cores (12 on your Mac)
sysctl -n hw.physicalcpu      # physical cores (12 on your Mac)
```
> 12 cores = 12 things can run **truly in parallel** (multiprocessing!). The scheduler
> actually manages 12 CPU "lanes" simultaneously, not just one.

### Command cheat-sheet:
| Goal | Command |
|---|---|
| See PRI and NI values | `ps -axo pid,pri,ni,stat,%cpu,comm` |
| Sort by priority | add `\| sort -nrk2` (column 2 = PRI) |
| Number of cores | `sysctl -n hw.ncpu` |
| Start with low priority | `nice -n 10 <command>` |
| Change running process priority | `renice -n 5 -p <PID>` |
| (Linux) interactive view | `htop` — shows PRI/NI columns, press `F6` to sort |

> **Backend connection:** On a busy production server, `nice` a heavy batch job (nightly
> report generation) to avoid stealing CPU from your user-facing API. This is manually
> applying priority scheduling — the same concept, just via a command line knob.

---

## ✅ Chapter 2 Summary (quick revision)

- **Scheduler** = strictly picks next **thread** (not process) from the Ready queue for the CPU.
- Key metrics: **TAT = CT−AT**, **WT = TAT−BT**, **Response = first\_run−AT**.
- **Non-preemptive** (run till done/block) vs **Preemptive** (OS can yank CPU → Running→Ready).
- Algorithms:
    - **FCFS** — arrival order, simple, but **convoy effect**
    - **SJF** — shortest burst, **optimal avg wait**, but **starvation** + needs burst estimate
    - **SRTF** — preemptive SJF, even better wait, more overhead
    - **Round Robin** — time quantum cycling, **fair**, great **response time**, no starvation
    - **Priority** — important first, **starvation** → fix with **aging**
    - **Multilevel Queue** — separate queues per job type, rigid
    - **MLFQ** — adaptive, promotes/demotes based on behavior, **closest to real OS**
- Three terms to nail: **Starvation**, **Aging**, **Convoy Effect**
- Saw it live: PRI/NI columns, `nice`/`renice` commands, 12 cores on your Mac.
- Numerical: draw Gantt → CT → TAT=CT−AT → WT=TAT−BT → average.