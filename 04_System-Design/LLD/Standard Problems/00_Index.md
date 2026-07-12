# LLD — Standard Problems

> Practice ladder for FAANG SDE2 LLD rounds. Work **top-to-bottom** — each rung adds one new challenge. Do the design *yourself* first (requirements → use cases → class diagram → patterns), then compare with notes.

Prerequisites (theory): [[01_OOPS_Basics]] · [[02_SOLID_Principles]] · [[03_UML&ClassDiagrams]] · [[04_Design_patterns]]

## The Ladder

| # | Problem | New skill it forces | Status |
|---|---|---|:---:|
| 01 | [[01_Parking_Lot]] | Canonical starter — classes, enums, inheritance, basic Strategy | ⬜ |
| 02 | [[02_Vending_Machine]] | **State** pattern (idle → hasMoney → dispensing) | ⬜ |
| 03 | [[03_Splitwise]] | Money splitting, balances, graph settle-up (your domain) | ⬜ |
| 04 | [[04_ATM_Wallet]] | State + composition + transactions | ⬜ |
| 05 | [[05_LRU_Cache]] | Data-structure-heavy LLD (HashMap + doubly linked list) | ⬜ |
| 06 | [[06_BookMyShow]] | **Concurrency** — seat locking, double-booking | ⬜ |
| 07 | [[07_Notification_System]] | Observer + Strategy + Factory together | ⬜ |
| 08 | [[08_Rate_Limiter]] | Algorithms + concurrency (direct Rakuten Pay relevance) | ⬜ |

Legend: ⬜ not started · 🟡 in progress · ✅ done

## How to attempt each problem
1. **Clarify requirements** — functional + non-functional, ask scoping questions.
2. **List core use cases / actors.**
3. **Identify classes** (nouns) and **methods** (verbs).
4. **Draw the class diagram** — relationships + multiplicity.
5. **Spot the patterns** — where does behavior vary? (Strategy/State/Factory/Observer…)
6. **Walk a flow** — trace one end-to-end scenario, note concurrency points.