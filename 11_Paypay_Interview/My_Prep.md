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
> The **second reason is the product itself.** I think it is really meaningful to help regular people start investing. Most people in Japan keep their money in a bank account earning almost no interest. PayPay Securities lets you start investing with just **100 yen**. That is a simple but powerful idea, and I want to be part of building the system that makes it work."

**~180 words ≈ 75–80 sec.** If told "briefly": the two one-line reasons only (technical challenge + meaningful product).

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

## Cheat sheet (memorize this, not the scripts)

**Intro beats:** Java/Spring Boot + PHP (their stack) → Rakuten Cash = PayPay Money → cash rail @ Seven/Lawson ATMs, exactly-once → recovery batch hours→3min → on-call root cause → IIT EE, 1000+ problems → bridge.

**Why-PPSEC beats:** I enjoy hard money + data-consistency backend → **(1) technical challenge:** payment app ↔ stock service, buy stock = debit PayPay wallet + update investment account at exactly the same time, roll back safely on failure → Kafka + Saga pattern → **(2) the product:** meaningful to help regular people invest, savings earn ~0%, start with 100 yen — want to build the system behind it.

**JD words to hit naturally (they score against these):** *root cause* · *high throughput* · *large scale* · *founding member / new organization* · *people who haven't started asset management*.

**Numbers:** 70M PayPay users · 28M NISA accounts · ¥2,000兆 savings · 1M Rakuten Cash investing users in 9 months · 100-yen investing · hours→3min · 45min→5min rollback (if infra comes up).

**If asked "the JD says 5+ years, you have 2":**
> "In calendar years, two. But those two years were on the money path of a payment company — bank integrations, idempotency, on-call, incident response — with end-to-end ownership of systems I designed, built, and operate. I'd put my depth in payments backend against most five-year resumes, and what I don't have yet — the brokerage domain, your scale — is exactly why I want to be here."

**Never do:** recite resume chronologically · list tech without problems · badmouth Rakuten · claim brokerage knowledge you don't have · run over time.
