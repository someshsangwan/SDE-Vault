# Chapter 5 — Protocols: TCP, UDP, DNS, HTTP/HTTPS (+ ARP, ICMP)

[← Subnetting](./04-subnetting-and-cidr.md) | [Index](./00-index.md)

---

## 5.1 TCP vs UDP (Transport layer)

> **TCP** = reliable, ordered, slower. **UDP** = fast, fire-and-forget, no guarantees.

| Aspect | TCP | UDP |
|--------|-----|-----|
| Connection | Connection-oriented (handshake first) | Connectionless (just send) |
| Reliable | ✅ Yes (acks + retransmission) | ❌ No |
| Ordered | ✅ Yes | ❌ No |
| Speed | Slower (more overhead) | ⚡ Faster (minimal overhead) |
| Header size | 20 bytes minimum | 8 bytes fixed |
| Flow / congestion control | Yes | No |
| Used by | HTTP(S), FTP, SMTP, SSH | DNS, DHCP, video/VoIP, games |

**Use TCP** when every byte matters (web, APIs, files, email, DB, SSH).
**Use UDP** when speed beats perfection (video calls, gaming, live streaming, DNS) — a resent old packet is useless in real time.

> 💡 **Aha:** TCP = registered post (signed for, resent if lost, slower). UDP = a postcard (fast, no guarantee). In a video call a lost frame should be skipped, not resent late.

⚠️ **Trap:** UDP is *not* "a worse TCP." It's the right choice for low latency. And UDP gives **no** delivery/ordering/duplicate guarantees at all.

---

## 5.2 TCP connection: 3-way handshake + 4-way teardown

### Open (3-way)
```
Client → SYN (seq=x)          "I want to talk, my seq starts at x"
Server → SYN-ACK (seq=y, ack=x+1)  "Heard x, my seq starts at y"
Client → ACK (ack=y+1)        "Got your y" — connection open, data flows
```
**Why 3, not 2?** Both sides must confirm they can **send AND receive**. Two messages would only prove one direction works.

### Close (4-way)
```
FIN  → from the side done sending
ACK  ← other side acknowledges
FIN  ← other side, when it's also done
ACK  → first side acknowledges
```
**Why 4?** A TCP connection is full-duplex; each direction closes **independently**. In the handshake the server can combine SYN+ACK; in teardown the ACK and FIN usually can't be combined (the other side may still have data).

**TIME_WAIT:** after the final ACK, the closer waits (~2× max segment lifetime) so any late/duplicate packets die before the port is reused.

> 💡 **Aha:** Handshake = a phone call start: "Hello, can you hear me?" / "Yes, can you hear me?" / "Yes." Three messages so both directions are confirmed.

---

## 5.3 Flow control vs Congestion control *(good-to-know depth)*

| | Flow control | Congestion control |
|---|--------------|--------------------|
| Protects | The **receiver** | The **network** (routers/links) |
| Signal | Receiver's advertised window (rwnd) | Packet loss / delay |
| Mechanism | Sliding window sized by receiver | cwnd: slow start + AIMD |

- **Slow start:** start small, **double** cwnd each RTT until a threshold.
- **AIMD** (Additive Increase, Multiplicative Decrease): then +1 per RTT; on loss, **cut sharply** and climb again.
- The sender actually sends `min(rwnd, cwnd)` — whichever limit is tighter.

**Sliding-window protocols** *(rare/deep):* Stop-and-Wait (1 at a time), Go-Back-N (resend lost + everything after), Selective Repeat (resend only the lost one — closest to TCP).

---

## 5.4 ARP — IP → MAC

> You know the next hop's **IP** but need its **MAC** to build the frame on the local link.

```
📱 (broadcast) "Who has 192.168.1.1? Tell me your MAC!"
📡 (unicast reply) "Me! MAC is BB:BB:BB:BB:BB:BB"
→ sender caches it in its ARP table
```
- Request = broadcast; reply = unicast.
- For **remote** traffic, ARP resolves the **router's** MAC (not the destination's) — because MAC = next hop only.
- vs DNS: **DNS maps name → IP; ARP maps IP → MAC.** Different layers, different jobs.

---

## 5.5 DNS — turning a name into an IP

> DNS = the internet's phonebook. `google.com` → `142.250.x.x`. Machines route by IP; humans remember names.

A **recursive resolver** (your ISP's, or `8.8.8.8`) walks the hierarchy when not cached:
```
1. You → Resolver:   "IP of google.com?"
2. Resolver → Root:  "ask .com →"
3. Resolver → .com TLD: "ask google's authoritative server →"
4. Resolver → Authoritative: "142.250.x.x ✅"
5. Resolver → You:   caches it for the TTL, hands it back
```

### Record types
| Record | Maps | Example |
|--------|------|---------|
| A | name → IPv4 | google.com → 142.250.0.0 |
| AAAA | name → IPv6 | google.com → 2001:db8::1 |
| CNAME | name → another name (alias) | www → root domain |
| MX | domain → mail server | where email goes |
| NS | domain → nameservers | who is authoritative |
| PTR | IP → name | reverse lookup |

- **Port 53.** Mostly **UDP** (small, fast, retry-able); falls back to **TCP** for zone transfers or responses > 512 bytes.
- **Caching** (browser → OS → router → resolver) makes repeat lookups instant. **TTL** controls cache lifetime → why DNS changes take time to propagate.

> 💡 **Aha:** DNS = phonebook. You remember the name (google.com); you need the number (IP). The resolver looks it up for you and remembers it for next time.

---

## 5.6 HTTP + HTTPS

> HTTP = the request/response language of the web (Application layer). Client sends a method + path; server replies with a status code + body. HTTP is **stateless** — each request stands alone (hence cookies/tokens carry identity).

### Methods
| Method | Does | Safe? | Idempotent? |
|--------|------|-------|-------------|
| GET | Read | Yes | Yes |
| POST | Create / submit | No | No |
| PUT | Replace | No | Yes |
| PATCH | Partial update | No | Not guaranteed |
| DELETE | Remove | No | Yes |

*Idempotent* = doing it twice = same effect as once (DELETE twice → still gone). *Safe* = no side effects.

### Status codes
| Class | Means | Common |
|-------|-------|--------|
| 1xx | Informational | 100 Continue |
| 2xx | ✅ Success | 200 OK, 201 Created, 204 No Content |
| 3xx | ↪️ Redirection | 301 Moved Permanently, 302 Found, 304 Not Modified |
| 4xx | ❌ Client error | 400, 401 Unauthorized, 403 Forbidden, 404 Not Found |
| 5xx | 💥 Server error | 500, 502 Bad Gateway, 503 Unavailable |

> Memory: **4xx = you (client) messed up; 5xx = server messed up.**

### HTTP versions
- **HTTP/1.1** — one request at a time per connection, but connections stay open (persistent).
- **HTTP/2** — multiplexing (many requests over one connection) + header compression.
- **HTTP/3** — runs over **QUIC** (built on **UDP**) to avoid TCP head-of-line blocking.

### HTTP vs HTTPS
| | HTTP | HTTPS |
|---|------|-------|
| Encryption | ❌ Plaintext | ✅ TLS |
| Port | 80 | 443 |
| Identity | None | Certificate (proves the real server) |

HTTPS = HTTP wrapped in **TLS**, giving **encryption** + **integrity** + **authentication**.

---

## 5.7 TLS handshake *(good-to-know)*

How two strangers agree a secret over an open wire:
```
Client → ClientHello (ciphers, random)
Server → ServerHello + certificate (public key)
Client → verifies cert (trusted CA?), key exchange (encrypted pre-secret)
Both   → agree a shared symmetric session key
Finished → all further data uses the fast