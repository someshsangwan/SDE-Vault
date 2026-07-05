# The Two Answers — Self-Intro (90s) & Why PayPay Securities (120s)

> Final scripts. Rakuten Cash framing + JD keywords already baked in. Practice out loud 5+ times each. Related: [[Interview_Experience]] · [[My_Resume_Q&A]]

---

## Q1: Introduce yourself — MAX 90 seconds

> "Hi, I'm Somesh. I'm a backend engineer at Rakuten Pay, where I've spent two years building payment systems in **Java and Spring Boot**, plus **PHP** for our batch systems — which I noticed is exactly your stack here.
>
> My main work is on **Rakuten Cash** — the wallet balance behind Rakuten Pay, the same concept as **PayPay Money**. Users charge that balance through many rails: Rakuten Card with auto-charge, bank accounts, gift cards, and cash. The piece I own is the **cash rail** — the integration that lets users charge at **Seven Bank and Lawson Bank ATMs**. That's the hardest rail to get right, because the ATM has already taken the user's cash, so the credit has to land **exactly once** — so I built idempotency on the banks' transaction IDs, mutual TLS with signed payloads, all inside the banks' two-to-three-second timeout.
>
> I also built an automated recovery batch in PHP that reconciles stuck transactions with our payment provider — it cut recovery from hours of manual work to under three minutes. And I'm on the on-call rotation, so I've debugged real production incidents down to **root cause** — not just restarting servers.
>
> Before this, I did Electrical Engineering at IIT Ropar and solved over a thousand algorithm problems — that's what pulled me into fintech.
>
> So in short: my two years have been about moving money **into** a wallet balance, correctly, at scale. PayPay Securities moves that same balance into investments — and that's exactly why I'm here."

**~230 words ≈ 85–90 sec.** If told "briefly": say paragraph 1 + 2 + last line only.

---

## Q2: Why PayPay Securities? — MAX 120 seconds

> "Honestly — because I've already watched your strategy work, from the other side.
>
> At Rakuten, the wallet balance I work on, Rakuten Cash, can also be used for mutual-fund investing at Rakuten Securities — and that converted **a million users in about nine months**. PayPay is running the same play, but with a structural advantage nobody can copy: the brokerage lives **inside** the payment app itself, in front of **70 million users** — no separate app, no separate account, start with 100 yen or even PayPay points. And the timing is once-in-a-generation: New NISA launched in 2024, there are already around **28 million NISA accounts**, and over **2,000 trillion yen** is still sitting in savings earning nothing. Your job description says the main target is people who haven't started investing yet — and that's exactly the user Rakuten could never fully reach, because the brokerage was a separate app. You don't have that gap.
>
> Second — the fit. My daily work is already what this role describes: Java, Spring Boot and PHP on **MySQL-family databases**, **high-throughput** money movement, idempotency under retries, reconciliation with external banks, and going to **root cause** in production incidents. Moving money into a balance and moving it into an investment are the same correctness problem — the parts that are new to me, like order execution and market-open traffic spikes, are exactly what I want to grow into.
>
> And third — this role is about being a **founding member of a new engineering organization**. That ownership is something I can't get by staying where I am. Plus, honestly — it's a product I'd tell my own friends in Japan to use."

**~290 words ≈ 115–120 sec.** If told "briefly": paragraph 1 (cut the NISA numbers) + last paragraph.

---

## Cheat sheet (memorize this, not the scripts)

**Intro beats:** Java/Spring Boot + PHP (their stack) → Rakuten Cash = PayPay Money → cash rail @ Seven/Lawson ATMs, exactly-once → recovery batch hours→3min → on-call root cause → IIT EE, 1000+ problems → bridge.

**Why-PPSEC beats:** Rakuten Cash→投信積立 = 1M users/9 months (proof the play works) → PPSEC's moat: inside the app, 70M users → New NISA 2024, 28M accounts, ¥2,000兆 savings → my work = their JD verbatim → new org, founding member → I'd recommend it to friends.

**JD words to hit naturally (they score against these):** *root cause* · *high throughput* · *large scale* · *founding member / new organization* · *people who haven't started asset management*.

**Numbers:** 70M PayPay users · 28M NISA accounts · ¥2,000兆 savings · 1M Rakuten Cash investing users in 9 months · 100-yen investing · hours→3min · 45min→5min rollback (if infra comes up).

**If asked "the JD says 5+ years, you have 2":**
> "In calendar years, two. But those two years were on the money path of a payment company — bank integrations, idempotency, on-call, incident response — with end-to-end ownership of systems I designed, built, and operate. I'd put my depth in payments backend against most five-year resumes, and what I don't have yet — the brokerage domain, your scale — is exactly why I want to be here."

**Never do:** recite resume chronologically · list tech without problems · badmouth Rakuten · claim brokerage knowledge you don't have · run over time.
