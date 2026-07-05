# CLAUDE.md — SDE-Vault Context

> Context for Claude Code when working in this folder. Auto-loaded.

---

## 👤 About Me

- **Name:** Somesh
- **From:** India, currently working in Japan
- **Current Role:** Backend Software Engineer at **Rakuten Pay**
- **Tech Stack:** Java, Spring Boot
- **Experience:** 2 years
- **Goal:** Crack **FAANG SDE2**

---

## 🎯 Prep Approach

- **Topic-driven, not date-driven.** I work on a topic when I have time — no fixed daily schedule, no "Day N" tracking.
- **Depth over cadence.** Each topic note must be a fundamental I can return to months later (not a summary).
- **Java-first.** All LeetCode solutions in Java. OS/CN/SD examples grounded in Java/JVM/Spring Boot context where possible.

### Skill Levels
- ✅ Decent at DSA
- ⚠️ Weak at System Design
- 📚 Want to learn Operating Systems & Computer Networks from scratch

### Target Companies
Open to all FAANG+. Most realistic given profile: **Amazon, Microsoft, Google L4**.

---

## 📁 Vault Structure

```
SDE-Vault/
├── 01_DSA/
│   └── NN_<Pattern>/             ← e.g. 01_Two-Pointer (Arrays)/, 08_Tree/
│       ├── Notes.md              ← Pattern fundamentals
│       └── Leetcode/             ← Java solutions (one .java per problem)
├── 02_Computer-Network/          ← 00-index.md + numbered topic notes
├── 03_Operating-System/
├── 04_System-Design/
│   ├── HLD_Notes/                ← Numbered topic folders (00–29), each with Readme.md
│   └── LLD/                      ← OOP basics, SOLID, design patterns
├── 05_AI_Agent_KT/               ← (empty — planned)
├── 06_Dev_OPS/                   ← Docker, K8s, CI/CD, cloud, monitoring
├── 07_Linux_basic/
├── 08_Behavioral/                ← (empty — planned) STAR stories from Rakuten Pay work
├── 09_Interview_Experience/      ← (empty — planned)
├── 10_springboot/
├── 11_Paypay_Interview/          ← Interview experience, resume Q&A, design round
└── 12_Java_/                     ← Core Java: collections, concurrency, JVM, streams
```

**File naming:** numbered prefixes (`01_`, `02_`, …) keep folders/files in the order I want them displayed. Inside DSA pattern folders, the main note is `Notes.md`.

**LeetCode solutions:** named `NN_<problem_name>_<leetcode_number>.java` (e.g. `06_DiameterOfTree_543.java`). Each file is a snippet-style `class Solution` with the LeetCode problem URL as a header comment — not standalone compilable code. There is no build system, test runner, or lint setup in this repo; it is a notes vault, not a Java project.

---

## 🛠️ Tooling

- **Note-taking:** Obsidian (this folder is the vault) + GitHub (public repo after prep is done)
- **IDE:** VS Code with Claude Code extension
- **Language:** Java for LeetCode (free tier)
- **Gitignored:** PDFs/ebooks (copyrighted reference material), `.idea/`, `.claude/`, Java build artifacts — never commit these
- **Commits:** short messages in the style `Added <Topic> <what>` (see `git log`)

---

## 💡 How Claude Should Help Me

1. **Topic notes:** When I ask for a topic, write a comprehensive Java-friendly fundamental note. Include pattern recognition triggers, templates, complexity analysis, common pitfalls, and self-check questions. Not a summary.
2. **Code review:** Review my Java solutions for time/space complexity, idiomatic Java, edge cases. Be honest.
3. **Concept explanation:** Use real-world analogies, especially payments / Rakuten Pay context.
4. **Note assistance:** Markdown, Obsidian-friendly, use `[[wikilinks]]` for backlinks. Diagrams always in Mermaid blocks (Obsidian renders them) — never ASCII art.
5. **Mock interviews:** Run mock rounds when I ask.
6. **Honest feedback:** Don't sugarcoat. If a solution is bad, say so and explain why.

### Tone
- Friendly but professional
- Direct, actionable
- Treat me as a peer engineer, not a beginner
- No fluff, no excessive emojis in technical content