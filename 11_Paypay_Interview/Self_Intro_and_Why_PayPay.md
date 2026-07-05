# Self-Introduction & "Why PayPay Securities" — Polished Answers

> The two questions that open almost every round, plus a JD ↔ experience mapping (§3) to weave into both. First impressions are formed in these 3 minutes — everything after is the interviewer confirming or correcting that impression. Related: [[Interview_Experience]] · [[My_Resume_Q&A]]

---

## Q1: "Please introduce yourself"

### What they're actually evaluating
- Can you **communicate clearly and concisely**? (They'll extrapolate: "this is how he'll explain a design in a meeting.")
- Do you **frame your experience as relevant to them** — or just recite your resume?
- Do you sound like someone who **owns things**, or someone who "was assigned tasks"?

### The structure (Present → Past → Proof → Bridge)
1. **Present** — who you are, one line.
2. **Past** — how you got here, one line.
3. **Proof** — 2–3 concrete things you've built (with the hard parts named).
4. **Bridge** — why you're sitting in *this* interview.

### 🎤 Main answer (~90 seconds, spoken)

> "Hi, I'm Somesh. I'm a backend engineer at Rakuten Pay in Japan, where I've spent the last two years building and operating payment systems in Java and Spring Boot.
>
> My background is Electrical Engineering from IIT Ropar in India, but I fell in love with software early — I spent my university years solving over a thousand algorithm problems and building full-stack projects, and that's what brought me into fintech.
>
> At Rakuten Pay, my main work has been on the money-movement path. I built the API integration that lets users charge their wallet with cash at Lawson Bank and Seven Bank ATMs — that taught me the hard parts of payments: idempotency when ATMs retry on network dropouts, mutual TLS and request signing with the banks, and coordinating timeouts when the bank gives you only two to three seconds to respond.
>
> I also designed an automated recovery system for stuck gift-payment transactions — it reconciles pending transactions against the payment provider and brought recovery time from hours of manual work down to under three minutes. And on the infrastructure side, I redesigned our rollback pipeline from a 45-minute code rebuild to a 5-minute container image swap. I'm also part of the on-call rotation, so I've debugged real production incidents — connection pool exhaustion, locked ledger tables — under pressure.
>
> So my two years have really been about one thing: making money move correctly and reliably at scale. That's exactly the problem PayPay Securities works on — just with investments instead of payments — which is why I'm excited to be here today."

### ⚡ Short version (~30 seconds — for rounds where they say "briefly")

> "I'm Somesh, a backend engineer at Rakuten Pay with two years of experience in Java and Spring Boot, working on payment systems — ATM cash-charge integrations with partner banks, an automated transaction recovery system, and production on-call. My whole experience is about moving money correctly and reliably, and I want to bring that to the investment side at PayPay Securities."

### Key beats (memorize these, not the script)
| Beat | The one detail that makes it credible |
|---|---|
| Who | Backend @ Rakuten Pay, Java/Spring Boot, 2 years |
| Origin | EE at IIT Ropar → 1000+ problems → fintech |
| Proof 1 | ATM cash charge (Lawson/Seven Bank) — idempotency, mTLS, 2–3s timeouts |
| Proof 2 | Gift recovery batch — hours → under 3 minutes |
| Proof 3 | Rollback pipeline 45 min → 5 min; on-call incidents |
| Bridge | "Money moving correctly at scale → same problem at PPSEC" |

### Pitfalls
- **Don't recite the resume chronologically** ("I joined in 2024, then in my first project…"). Lead with what you *are*, then prove it.
- **Don't list technologies without problems.** "I used Kafka and Redis" is noise; "ATMs retry on dropouts, so I enforced idempotency with unique constraint keys" is signal.
- **Don't run past 2 minutes.** Every sentence after ~90 seconds costs you more than it earns. End on the bridge and stop.
- **Don't undersell with "I helped with / I was involved in."** Say "I built," "I designed," "I was paged and I fixed it."
- Every proof point here is one they can drill into — and you already have deep answers prepared in [[My_Resume_Q&A]] (Q1, Q2, Q6, Q7). Never mention something you can't go three levels deep on.

---

## Q2: "Why PayPay Securities?"

### What they're actually evaluating
- Did you **do your homework**, or is this one of 30 applications?
- Is your reason **specific to PPSEC** — would the same answer work for Rakuten Securities? If yes, it's a bad answer.
- Does your motivation **survive contact with reality** (they want people who'll stay and care)?

### The structure (Mission → Moment → Moat → Me)
1. **Mission** — what PPSEC is doing and why it matters.
2. **Moment** — why *now* is the interesting time (New NISA, cashless shift).
3. **Moat** — what only PPSEC can do (the 70M-user PayPay super-app).
4. **Me** — why your specific experience fits, and what you gain.

### 🎤 Main answer (~90 seconds, spoken)

> "Three reasons — the mission, the timing, and the fit with my experience.
>
> First, the mission. Japan has over 2,000 trillion yen sitting in household savings earning almost nothing, and most people have never invested because traditional brokerages feel complicated and intimidating. PayPay Securities removes that barrier completely — you can start with 100 yen, or even invest the PayPay points you earned from daily shopping, without opening a separate account or transferring money. I find that genuinely meaningful: it's not building another tool for experienced traders, it's turning everyday shoppers into first-time investors.
>
> Second, the timing. The New NISA program launched in 2024, and there are already around 28 million NISA accounts. There's a once-in-a-generation shift happening from savings to investment, and PayPay Securities has an advantage no other broker can copy: it lives inside the PayPay app with over 70 million users. SBI or Rakuten Securities serve experienced investors — but nobody else can reach the person who's never invested, at the moment they're already holding their wallet app.
>
> Third, the fit. At Rakuten Pay I've spent two years making payments correct and reliable — idempotency, transaction integrity, reconciliation with external banks, surviving traffic spikes. A brokerage has exactly those problems, plus new ones I really want to work on: order execution, market-hours traffic spikes, keeping cash and holdings consistent between the payment app and the securities account. And the stack — Java and Spring Boot on Kubernetes, Kafka, TiDB — is where I'm strong on the fundamentals but excited to go deeper at PPSEC's scale.
>
> So it's the one company where my payments experience transfers directly, but the problem space is new enough that I'll grow — and the product is one I'd honestly want my own friends in Japan to use."

### ⚡ Short version (~30 seconds)

> "PayPay Securities is turning 70 million PayPay users into first-time investors, right as the New NISA wave is bringing millions of Japanese people into investing — and no other broker has that distribution. My two years at Rakuten Pay were about making money movement correct and reliable, which transfers directly to a brokerage, while order execution and market-data scale give me new problems to grow into. That combination — direct fit plus real growth — is exactly what I'm looking for."

### Key beats
| Beat | The specific fact that proves homework |
|---|---|
| Mission | 100-yen fractional shares; Point Investment (Point Unyo); buy directly from PayPay balance (Omatase-Konyu) |
| Moment | New NISA (2024); ~28M NISA accounts; ¥2,000兆 in savings; cashless rate ~58% |
| Moat | Mini-app inside PayPay's 70M-user super-app — SBI/Rakuten can't replicate the distribution |
| Me | Payments correctness (idempotency, reconciliation) transfers; order execution / TiDB / Kafka at scale is the growth edge |

### Follow-ups they will ask (be ready)
- **"SBI and Rakuten Securities are much bigger — why is PPSEC competitive?"**
  → Different market. They serve people who already invest; PPSEC serves people who never have. 100 yen + points + no separate account = an onboarding funnel no incumbent can copy, because none of them own a daily-use payment app with 70M users. (Full answer in [[Interview_Experience]] §Fintech Q&A.)
- **"Why leave Rakuten Pay? Isn't Rakuten also fintech?"**
  → Never badmouth Rakuten. "Great experience, learned secure high-concurrency payments — now I want more ownership in a smaller, faster team, on the investment side, which is the part of fintech I'm most excited about." (See [[My_Resume_Q&A]] Q31.)
- **"But Rakuten also has Rakuten Securities — why not move internally?"**
  → The product philosophy is different: Rakuten Securities is a full-featured broker for experienced investors; PPSEC is investing-for-everyone embedded in a payment app. The embedded/mini-app model is the engineering problem I find most interesting — consistency between wallet and brokerage account across two systems. Plus PayPay's environment — English-first, engineers from 50+ countries, startup speed with SoftBank backing.
- **"What do you know about our tech stack?"**
  → Java/Spring Boot, Kotlin, Scala across microservices; AWS + Kubernetes with Argo CD; TiDB, Aurora MySQL, DynamoDB, Redis; Kafka for inter-service events. Mention you're especially curious about TiDB — distributed SQL for the ledger is a deliberate correctness-plus-scale choice.

### Pitfalls
- **Generic praise** ("PayPay is a great company, growing fast") — instant red flag. Every claim must carry a number or a feature name.
- **Only talking about what you get** (salary, brand, visa). Frame it as fit: what you bring **and** what you'll grow into.
- **Overpromising domain knowledge.** You know payments, not brokerage. Saying "order execution is new to me and that excites me" is stronger and safer than pretending.
- **Comparing companies negatively.** Position PPSEC's difference, never Rakuten's weakness — the interviewer notes how you talk about your current employer.

---

## Delivery notes (both questions)

- **Practice out loud, 5+ times each.** The scripts above are written for the ear, not the eye — if a sentence trips your tongue, rewrite it in your own words. You're memorizing the *beats tables*, not the paragraphs.
- **First 10 seconds slow.** Nerves make you rush the opening; a deliberate first sentence sets the pace for the whole interview.
- **End with an upward hook.** Both answers end pointing at PPSEC — that invites the interviewer's natural next question and hands you control of the conversation flow.
- **PayPay values to weave in naturally** (don't name-drop the list): ownership ("I volunteered to build…"), speed ("instead of waiting for full specs, I…"), no ego ("I shared the draft contract with the frontend team early…").