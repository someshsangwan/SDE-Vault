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

> "I enjoy solving hard backend problems around money and data consistency, and there are two reasons this role is a strong fit.
>
> The **first reason is the technical challenge.** PayPay Securities connects a payment app with a stock investment service — and making these two systems work together smoothly is a really hard problem. For example, when a user buys stock, we need to take money from their PayPay wallet and update their investment account at **exactly the same time**. If anything fails in the middle, we need to **roll it back safely**. This kind of problem — using things like **Kafka and the Saga pattern** — is exactly what I enjoy working on.
>
> The **second reason is the product and its scale.** I think it is really meaningful to help regular people start investing. Most people in Japan keep their money in a bank account earning almost no interest. PayPay Securities lets you start investing with just **100 yen** — that is a simple but powerful idea. And the timing is perfect: the government started the **New NISA program**, which gives people a tax-free way to invest, so the number of investors is going to keep growing. On top of that, PayPay already has a **70 million user ecosystem**, and many of these everyday shoppers can become investors. So the user base is only going to get bigger. That is exactly the kind of challenge I want to work on — **how do we scale the system to handle this much traffic and still keep every transaction smooth and correct**, especially at busy times like when the market opens.so i woiuld like to be the part of team who will be dpoing these kind of scaling"

**~210 words ≈ 85–90 sec.** If told "briefly": the two one-line reasons only (technical challenge + meaningful product at scale).

---

## Q3: What do you understand by Fintech in Japan?

> "I think Japan's Fintech industry is going through a big change right now. In the past, Japan was very much a **cash-first country** — most people kept their money in a bank account or as cash at home. But two big things are happening at the same time now.
>
> First, the government is trying to push everyone toward **cashless payments**. Apps like PayPay have already helped bring the cashless rate up to nearly **60%**. Second, the government launched the **New NISA program in 2024**, which gives people a tax-free way to invest money in stocks. This is making a lot of people try investing for the first time.
>
> For me as a backend engineer, this is a really exciting time. Fintech is not just about moving money — it's about making sure the **data is always correct** between the payment app and the investment account, handling **millions of users** at the same time, and making the system **fast and reliable** even when the stock market opens and traffic spikes. PayPay Securities is in a great position because it already has **70 million users** from the PayPay app, and it can turn those everyday shoppers into investors."

**Beats:** cash-first past → two shifts: (1) cashless push, PayPay → ~60% · (2) New NISA 2024, first-time investors → as an engineer: data correctness across payment↔investment, millions of users, reliability at market-open spikes → PPSEC moat: 70M users, shoppers → investors.

---

## Q4: Where do you see yourself in 3 years?

> "In 3 years, I want to be someone the team **trusts for important system decisions**. I want to **own a key service** — maybe the transaction ledger or the order processing service — and keep improving it over time. I also want to **help new engineers**, especially people from outside Japan, join the team and feel comfortable quickly."

**Beats:** trusted for system decisions → own a key service (ledger / order processing) → mentor new engineers, esp. non-Japan hires.

---

## Q5: How does security matter in fintech (backend engineer's view)?

> "Security is the most important thing in fintech because we handle real people's money. As a backend engineer, I focus on four things: making sure **only the right people can use the API**, keeping **sensitive user data like KYC information safe and encrypted**, encrypting **all data both when it moves over the network and when it sits in the database**, and keeping a **complete record of every transaction** so we can always check and fix anything that goes wrong."

**Beats:** money = highest stakes → 4 things: (1) authN/authZ on the API · (2) protect KYC/PII, encrypted · (3) encryption in transit + at rest · (4) full audit trail of every transaction.

**Likely follow-ups (be ready — they probe the four points):**
- **"How do you make sure only the right people use the API?"** → authentication (who you are) vs authorization (what you're allowed to do); OAuth2 / JWT tokens, short-lived + refresh; role/scope checks per endpoint; mutual TLS for service-to-service (I already do this with Seven/Lawson Bank).
- **"How do you protect KYC / sensitive data?"** → encrypt at rest (AES-256), tokenize or mask PII, principle of least privilege on who can read it, never log raw PII, key management via a KMS/HSM (not keys in code).
- **"In transit vs at rest — what's the difference?"** → in transit = TLS/HTTPS + mTLS between services so nobody can sniff it on the wire; at rest = DB/disk encryption so a stolen dump is useless without keys.
- **"Why the audit trail / how do you build it?"** → append-only, immutable ledger; every money movement is an event you can replay; ties into idempotency + reconciliation (my recovery batch depends on it); also needed for compliance and dispute resolution.
- **"How do you stop the same request running twice (double-charge)?"** → idempotency keys on transaction IDs — bridge straight back to my Rakuten Cash cash-rail work.

---

## Q6: What do you understand by startup culture?

> "For me, startup culture is not about free snacks or a cool office — it's about **how you actually work**. You move quickly, you don't wait for someone to give you all the answers, and you feel **real ownership** over what you build. I really like PayPay's values — especially **'No Ego' and 'Speed'**. At Rakuten Pay, the best results came when the team talked directly, shared ideas openly, and focused on the user's problem instead of a long chain of approvals. At PayPay Securities I'd work with engineers from **50+ countries, all in English** — that's where I do my best work. And because it's backed by **SoftBank**, you also get big-company stability and resources — the best of both worlds."

**One-liner (say first):** startup culture = move fast, take full ownership, communicate openly — no top-down approval for every small decision.

**Beats:** not snacks, it's *how you work* → fast + ownership + open comms → PayPay values: No Ego, Speed, Ownership → 50+ countries, English → SoftBank-backed = startup speed + big-company stability.

**Good to know (drop naturally):**
- **PayPay 5 Senses (values):** Speed first · No Ego (team wins) · Believe in the product · Be sincere & professional (matters in fintech) · Find your purpose (ownership).
- **PPSEC stack:** Java/Spring Boot, Kotlin, Scala · AWS + Kubernetes + Argo CD (GitOps) · TiDB, Aurora MySQL, DynamoDB, Redis · Kafka for events.

**Likely follow-ups:**
- **"How do you move fast without breaking things in a financial app?"** → fast ≠ careless: good tests + automated CI/CD → deploy often and safely, catch bugs before prod; for high-risk changes use **canary releases** — roll out to a small group first, watch for errors, then everyone.
- **"Tell me about a time you dealt with unclear requirements."** → Rakuten Pay, new payment method, product specs not ready. Instead of waiting, I wrote a simple **API contract** from what I understood, shared it so frontend could build a mock, and raised the unclear parts with the PO directly → caught issues early, saved ~2 weeks of rework.

---

## Q7: What is PayPay Securities & what can users do here?

> "PayPay Securities is an **app-only stock brokerage** built for **first-time investors** — people who find traditional brokerages too complicated or expensive. It started in 2016 as **One Tap BUY** (Japan's first app-only brokerage) and was renamed in 2021 to tie into the **PayPay app**. The big advantage is it also runs as a **mini-app inside PayPay**, so those **70M+ users can start investing without opening a new account**. Users can buy **Japanese and US stocks from just 100 yen** — it works because we handle **fractional shares** internally."

**What users can do (know these 5):**
- **Start with 100 yen** — JP or US stocks; possible because PPSEC handles **fractional shares** internally (no need to buy a whole share).
- **Point Investment (Point Unyo)** — put PayPay shopping points into a **virtual fund** to learn the market without spending real money.
- **Leave-and-Buy (Omatase-Konyu)** — buy stocks straight from **PayPay Money / PayPay Bank** balance; no transfer to a separate brokerage account.
- **US stocks 24/7** — buy Apple, NVIDIA etc. anytime, **in yen**, no manual currency exchange.
- **NISA support** — Japan's tax-free investing program, so invest + save tax at the same time.

**Beats:** app-only brokerage for first-time investors → One Tap BUY (2016) → PayPay Securities (2021) → mini-app inside PayPay, 70M users, no new account → 100-yen investing via fractional shares → point investing, buy from wallet (Omatase), US stocks 24/7 in yen, NISA.

**Backend angle if they push ("what's hard here?"):** fractional shares = you own 0.001 of a real share, so the ledger must track fractions exactly and reconcile against the whole shares the broker actually holds; buying from the wallet = the cross-service money move (debit wallet ↔ credit investment) that ties back to my Q2 Kafka/Saga point.

---
