# Chapter 6 — Top Interview Questions (Computer Networks)

[← Protocols](./05-protocols-tcp-udp-dns-http.md) | [Index](./00-index.md)

> The 11 questions that actually get asked in SWE / SDE2 rounds, with answers written to be **spoken out loud**. Each answer starts with a one-line "if you only say one thing" summary, then depth if the interviewer digs.
> Related deep-dives: [[01-layers-and-devices]], [[02-addressing-ip-mac-ports]], [[03-your-network-dhcp-nat]], [[05-protocols-tcp-udp-dns-http]]

---

## Q1. What happens when you type `google.com` and press Enter?

> **One-liner:** The browser turns a name into an IP (DNS), opens a reliable connection (TCP), secures it (TLS), asks for the page (HTTP), and renders what comes back.

Walk it as a story — this is the single most-asked question because it touches every layer:

1. **URL parse & cache check** — browser checks its own cache, then the OS cache, for `google.com`.
2. **DNS resolution** — if not cached, a recursive resolver walks Root → `.com` TLD → Google's authoritative server to get the IP.
3. **ARP / routing** — to send the first packet, your machine needs the **MAC of the next hop** (your router). ARP resolves that on the local link.
4. **TCP 3-way handshake** — SYN → SYN-ACK → ACK opens a reliable pipe to the server on port 443.
5. **TLS handshake** — client and server agree on a shared secret key and the server proves its identity with a certificate. Now the channel is encrypted.
6. **HTTP request** — browser sends `GET / HTTP/2` with headers (cookies, user-agent, etc.).
7. **Server responds** — `200 OK` + HTML (often via a load balancer → app server → DB behind the scenes).
8. **Render** — browser parses HTML, fires more requests for CSS/JS/images, builds the DOM, and paints the page.

> **Aha:** Name → IP → connect → secure → ask → render. If you can say those six words in order, you own this question.

⚠️ **Trap:** Don't forget the **caching** at step 1 and the **TLS** step for HTTPS. Interviewers love when you mention both.

---

## Q2. TCP vs UDP — difference and when to use each?

> **One-liner:** TCP is reliable and ordered but slower; UDP is fast and fire-and-forget with no guarantees.

**First, what are they?** TCP and UDP are the two main **transport-layer protocols** — the rules your computer uses to actually **send and exchange data over a network/internet**. When one app (your browser, a game, a video call) wants to talk to another machine, it hands its data to *either* TCP *or* UDP, which chops it into packets and delivers them. They sit **on top of IP**: IP handles *where* the packet goes (addressing/routing), while TCP/UDP handle *how* it's delivered (reliably or not).

So the real question isn't "TCP or the internet" — it's "which delivery style do I want?"

| Aspect | TCP | UDP |
|--------|-----|-----|
| Connection | Handshake first | Just send |
| Reliable / ordered | ✅ Yes (acks + retransmit) | ❌ No |
| Speed | Slower (overhead) | ⚡ Faster |
| Header | 20 bytes | 8 bytes |
| Flow / congestion control | Yes | No |
| Used by | HTTP(S), FTP, SSH, email | DNS, video calls, gaming, streaming |

**Use TCP** when every byte matters (web pages, APIs, files, payments). **Use UDP** when speed beats perfection — in a video call, a late re-sent frame is useless, so you'd rather skip it.

> **Aha:** TCP = registered post (signed for, resent if lost). UDP = a postcard (fast, no guarantee).

---

## Q3. Explain the TCP 3-way handshake (and why not 2?)

> **One-liner:** SYN, SYN-ACK, ACK — three messages so **both sides confirm they can send AND receive** before any data is sent.

**What is it?** Before TCP sends real data, it does a quick "hello" to set up the connection — this setup is the **3-way handshake**. It makes sure both machines are alive, reachable, and agree on where to start counting bytes.

**What do SYN and ACK mean?**
- **SYN = Synchronize** — "I want to start a connection, and here's my starting byte-counter (sequence number)." TCP numbers every byte so it can detect loss and reorder packets; both sides must agree on where each other's numbering **starts** — that agreement *is* the "synchronize."
- **ACK = Acknowledge** — "I received what you sent."

```
Client → SYN (seq=x)              "Let's talk, my sequence starts at x"
Server → SYN-ACK (seq=y, ack=x+1) "Got your x, and mine starts at y"
Client → ACK (ack=y+1)            "Got your y" → connection open, data flows
```

**Why 3 not 2?** Two messages only prove **one** direction works. The third message confirms the reverse direction (client's ack of the server's SYN). Both sides must know the other can hear them before trusting the pipe.

**Teardown is 4-way** (FIN → ACK → FIN → ACK) because a TCP connection is **full-duplex** — each direction closes independently, and the side receiving the FIN may still have data left to send.

> **Aha:** Phone call: "Can you hear me?" / "Yes, can you hear me?" / "Yes." Three turns confirm both directions.

---

## Q4. How does DNS resolution work?

> **One-liner:** DNS is the internet's phonebook — it turns `google.com` into an IP by walking a hierarchy, and caches the answer.

**What is it?** Computers don't route by names like `google.com` — they route by **IP addresses** (numbers). **DNS (Domain Name System)** is the global lookup service that translates the human-friendly name you type into the machine-friendly IP needed to actually reach the server. Without it you'd have to memorize `142.250.x.x` for every website.

If not cached, a **recursive resolver** (e.g. your ISP or `8.8.8.8`) does the legwork:

```
1. You → Resolver:       "IP of google.com?"
2. Resolver → Root:      "ask the .com servers →"
3. Resolver → .com TLD:  "ask Google's authoritative server →"
4. Resolver → Authoritative: "142.250.x.x ✅"
5. Resolver → You:       caches it for the TTL, returns it
```

- **Recursive** = the resolver does all the work and returns a final answer. **Iterative** = each server just points you to the next one.
- **Records:** A (→IPv4), AAAA (→IPv6), CNAME (alias), MX (mail), NS (nameserver).
- **Port 53**, mostly **UDP** (small, fast), falls back to TCP for large responses.
- **Caching + TTL** is why DNS changes take time to propagate.

> **Aha:** You remember the name; the resolver looks up the number and remembers it for next time.

---

## Q5. HTTP vs HTTPS — and how does the TLS handshake work?

> **One-liner:** HTTPS = HTTP wrapped in TLS, giving **encryption + integrity + authentication**. HTTP is plaintext on port 80; HTTPS is encrypted on port 443.

**What are they?** **HTTP (HyperText Transfer Protocol)** is the language browsers and servers use to exchange web pages — request goes out, response comes back. The problem: plain HTTP sends everything as **readable text**, so anyone between you and the server (a hacker on public WiFi, your ISP) can read or tamper with it. **HTTPS** fixes that by running HTTP inside a secure tunnel called **TLS (Transport Layer Security)**. The "S" = Secure. So HTTPS gives you three things: **encryption** (nobody can read it), **integrity** (nobody can change it undetected), and **authentication** (you're really talking to the real Google, not a fake).

**TLS handshake** — how two strangers agree a secret over an open wire:

```
Client → ClientHello (supported ciphers + random)
Server → ServerHello + certificate (contains public key)
Client → verifies cert against a trusted CA, sends key material
Both   → derive the same shared symmetric session key
→ all further data uses fast symmetric encryption
```

The clever part: **asymmetric** crypto (slow, public/private key) is used **only** to safely exchange the secret; then both switch to **symmetric** crypto (fast) for the actual data. The **certificate** (signed by a Certificate Authority) proves you're really talking to Google and not an impostor.

> **Aha:** Asymmetric = expensive armored truck used once to deliver a shared key. Symmetric = the cheap fast lock both sides use afterward.

---

## Q6. OSI model vs TCP/IP model — the layers and their jobs

> **One-liner:** OSI is a 7-layer teaching model; TCP/IP is the 4-layer model the internet actually runs on. Data gets wrapped (encapsulated) going down and unwrapped going up.

**What is it?** Networking is complicated, so engineers split the whole "get data from my app to yours" job into **layers**, where each layer has one clear responsibility and only talks to the layer above and below it. This makes the system modular — you can swap WiFi for Ethernet (bottom layer) without touching your web app (top layer). The **OSI model** is the 7-layer reference used for teaching; the **TCP/IP model** is the practical 4-layer version the real internet uses.

| OSI (7) | Job | Example |
|---------|-----|---------|
| 7 Application | User-facing protocols | HTTP, DNS, SMTP |
| 6 Presentation | Encryption, encoding | TLS, JPEG |
| 5 Session | Sessions/connections | — |
| 4 Transport | End-to-end delivery | **TCP, UDP** |
| 3 Network | Routing across networks | **IP**, routers |
| 2 Data Link | Local link, MAC | Ethernet, switches |
| 1 Physical | Bits on the wire | cables, radio |

**TCP/IP (4-layer):** Application (7-5) → Transport (4) → Internet (3) → Link (2-1).

> **Mnemonic (top→bottom):** **A**ll **P**eople **S**eem **T**o **N**eed **D**ata **P**rocessing.

⚠️ **Trap:** Remember **routers = Layer 3** (IP), **switches = Layer 2** (MAC). Mixing these up is the classic slip.

---

## Q7. Difference between IP, MAC, and Port? What are NAT and DHCP?

> **One-liner:** MAC identifies a device on the local link, IP identifies it across networks, and a port identifies the specific app/process on it.

**What are they?** These are **three different kinds of address**, each answering a different question when data travels. To deliver a message you need to know *which building* (IP), *which exact device* (MAC), and *which app on that device* (port). They work together, not instead of each other.

- **MAC** — permanent hardware address, Layer 2, only matters on the local link (next hop).
- **IP** — logical address, Layer 3, used to route across the internet; can change.
- **Port** — Layer 4 number identifying an app (HTTP 80, HTTPS 443, SSH 22).

> **Aha:** IP = the building's street address. MAC = the specific person. Port = the apartment/room the message is for.

- **DHCP** — auto-assigns your device an IP when it joins a network (the **DORA** dance: Discover → Offer → Request → Ack).
- **NAT (Network Address Translation)** — lets many private devices share one public IP; the router rewrites addresses and remembers who asked what, so replies come back to the right device.

---

## Q8. How does HTTP work? GET vs POST, status codes, HTTP/1.1 vs 2 vs 3

> **One-liner:** HTTP is a stateless request/response language — client sends a method + path, server replies with a status code + body.

**What is it?** **HTTP (HyperText Transfer Protocol)** is the application-layer protocol that powers the web. Every time you load a page, click a link, or an app calls an API, it's speaking HTTP: the **client** (browser/app) sends a **request** ("give me `/home`"), and the **server** sends back a **response** (a status code + the content). It's the request/response conversation underneath everything you do online.

**GET vs POST:** GET reads data (safe, idempotent, params in URL); POST creates/submits (not idempotent, body carries data).

**Status codes:** 2xx success (200 OK), 3xx redirect (301), **4xx = client's fault** (404 Not Found, 401 Unauthorized), **5xx = server's fault** (500, 503).

**Versions:**
- **HTTP/1.1** — persistent connections, but one request at a time per connection (head-of-line blocking).
- **HTTP/2** — **multiplexing** (many requests over one connection) + header compression.
- **HTTP/3** — runs over **QUIC** (built on UDP) to kill TCP-level head-of-line blocking.

**Stateless** = each request stands alone; that's why **cookies/tokens** carry identity between requests.

---

## Q9. Router vs Switch vs Hub?

> **One-liner:** Hub is dumb (Layer 1, copies to everyone), switch is smart within a network (Layer 2, uses MAC), router connects different networks (Layer 3, uses IP).

**What are they?** These are the three classic **devices that move data around a network** — the physical boxes traffic passes through. The difference is *how smart* each one is about where it sends data: a hub blindly copies to everyone, a switch delivers precisely to the right device inside your local network, and a router connects your network to *other* networks (like the internet).

| Device | Layer | Forwards by | Smart? |
|--------|-------|-------------|--------|
| **Hub** | 1 (Physical) | Copies bits to **all** ports | ❌ Dumb — one big collision domain |
| **Switch** | 2 (Data Link) | MAC address → only the right port | ✅ Learns a MAC table |
| **Router** | 3 (Network) | IP address → between networks | ✅ Routes across the internet |

> **Aha:** Hub = shouting in a room so everyone hears. Switch = whispering directly to the right person. Router = the post office connecting different cities.

---

## Q10. HTTP is stateless — so how do cookies, sessions, and CORS work?

> **One-liner:** Since HTTP forgets you between requests, the server issues a **cookie** the browser sends back each time, mapping you to a **session** (state) on the server.

**What's the problem being solved?** HTTP is **stateless** — the server treats every request as brand new and remembers nothing about you from the last one. But real websites *need* memory: "this user is logged in," "here's what's in their cart." Cookies and sessions are the trick that adds that memory on top of a forgetful protocol.

- **Cookie** — a small piece of data the server sets (`Set-Cookie`); the browser attaches it to every subsequent request to the same site.
- **Session** — server-side state (e.g. "user 42 is logged in") keyed by a session ID stored in the cookie. Alternative: a self-contained **JWT token**.
- **CORS** (Cross-Origin Resource Sharing) — a browser security rule. By default a page at `a.com` can't call `b.com` via JS; the server at `b.com` must send `Access-Control-Allow-Origin` headers to permit it. It protects users from malicious cross-site requests.

> **Aha:** A cookie is a coat-check ticket — the server hands you a number, you show it each time, and the server looks up who you are.

---

## Q11. Layer 4 vs Layer 7 Load Balancing ⭐

> **One-liner:** A **Layer 4** load balancer routes on IP + port (it doesn't read the message); a **Layer 7** load balancer reads the actual HTTP request (URL, headers, cookies) and routes on content — smarter but slower.

A **load balancer** spreads incoming traffic across multiple backend servers so no single one is overwhelmed. Where it makes that decision matters:

| | **Layer 4 (Transport)** | **Layer 7 (Application)** |
|---|-------------------------|----------------------------|
| Decides on | IP address + TCP/UDP **port** | HTTP content: **URL, headers, cookies, method** |
| Sees the data? | ❌ No — just forwards packets | ✅ Yes — reads/terminates the HTTP request |
| Speed | ⚡ Very fast, low overhead | Slower (has to parse the request) |
| Smart routing | Basic (round-robin, least-connections) | Content-based: `/api` → API servers, `/images` → CDN, sticky sessions by cookie |
| TLS | Passes encrypted traffic through | Can **terminate TLS** (decrypt, inspect, re-encrypt) |
| Example | AWS Network Load Balancer (NLB), HAProxy (TCP mode) | AWS Application Load Balancer (ALB), Nginx, HAProxy (HTTP mode) |

**How to explain it in one breath:**
- **Layer 4** works like a **mail sorter that only reads the envelope** — it sees the destination IP and port and forwards the whole sealed packet fast, without opening it. Great for raw throughput.
- **Layer 7** is like a **receptionist who opens the letter, reads it, and routes it based on the content** — "this is a login request → auth service; this is an image request → image service." Slower, but it enables smart routing, A/B testing, path-based routing, and cookie-based **sticky sessions**.

**When to use which:**
- **L4** — when you need raw speed, low latency, or you're balancing non-HTTP traffic (databases, game servers, any TCP/UDP).
- **L7** — when routing depends on *what's inside* the request: microservices behind one domain, path-based routing, header/cookie rules, or you want the LB to handle TLS termination.

> **Aha:** L4 reads the envelope; L7 reads the letter. L4 is faster; L7 is smarter.

⚠️ **Trap:** Interviewers love the follow-up **"which is faster and why?"** → L4, because it never parses the payload. And **"can it do TLS termination?"** → that's an L7 feature (you have to decrypt to read the request).

---

## 🎯 30-Second Cheat Sheet

| Q | Nail it with |
|---|--------------|
| Type a URL | Name → IP → connect → secure → ask → render |
| TCP vs UDP | Reliable+ordered vs fast+fire-and-forget |
| 3-way handshake | SYN/SYN-ACK/ACK — confirm both directions |
| DNS | Phonebook: recursive resolver walks Root→TLD→Authoritative |
| HTTPS/TLS | Asymmetric to swap a key, symmetric for the data; cert = identity |
| OSI/TCP-IP | 7 layers; router=L3, switch=L2 |
| IP/MAC/Port | Street address / person / room |
| HTTP | Stateless; 4xx you, 5xx server; H2 multiplexes, H3 on QUIC |
| Router/Switch/Hub | Networks / MAC / dumb-copy |
| Cookies/CORS | Coat-check ticket; CORS lets cross-origin calls through |
| **L4 vs L7 LB** | **Envelope vs letter — faster vs smarter** |