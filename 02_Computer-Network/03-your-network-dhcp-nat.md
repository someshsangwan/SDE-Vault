# Chapter 3 — Your Network: DHCP & NAT

[← Addressing](./02-addressing-ip-mac-ports.md) | [Index](./00-index.md) | [Next: Subnetting →](./04-subnetting-and-cidr.md)

---

## 3.1 What is a "network"? (clearing the confusion)

> A "network" = all devices connected to the **SAME router/WiFi**. The building/room doesn't matter — only "which router are you on?"

| Scenario | Same network? |
|----------|---------------|
| You + roommate on the **same WiFi** | ✅ Yes — share the same public IP |
| You on WiFi, roommate on **own 4G data** | ❌ No — different networks (different routers) |
| 50 strangers on the **same café WiFi** | ✅ Yes — all one network |

---

## 3.2 Public vs Private IP

| | Private IP | Public IP |
|---|-----------|-----------|
| Example | `192.168.1.5` | `103.22.45.10` |
| Works | Inside your network only | On the whole internet |
| Assigned by | **Your router (DHCP)** | **Your ISP** |
| Reusable? | ✅ Yes (every home reuses `192.168.1.x`) | ❌ No (globally unique) |
| Reachable from internet? | ❌ No (hidden) | ✅ Yes |

### Can two people have the same IP?
- **Private:** ✅ YES — millions of homes reuse `192.168.1.5`. They live in separate bubbles, never collide.
- **Public:** ❌ NO — must be globally unique (guaranteed by **IANA → RIR → ISP** allocation).

### Why private IPs exist (3 uses)
1. 💰 **Save public IPs** — only ~4.3B IPv4 exist; a whole network shares ONE public IP. *(Main reason.)*
2. 🏠 **Local communication** — printer, Chromecast, router settings (`192.168.1.1`) — never touches the internet.
3. 🔒 **Security** — private IPs are hidden behind the router, unreachable directly from outside.

> 💡 **Aha:** Company with one phone number (public IP) and internal extensions (private IPs). Outsiders dial the main number; insiders use extensions. Every company reuses "Ext 101" — no clash.

### Does the private IP change?
Yes — it's a **lease** from DHCP, not permanent. Changes on reconnect, router restart, lease expiry, or joining a new WiFi. Often stays the same day-to-day (renewed). You can **pin** it with a *static IP / DHCP reservation* (router maps a fixed IP to a device's MAC).

> 🔑 **MAC never changes; private & public IP both can.**

---

## 3.3 DHCP — auto-assigns your IP when you join

When you join WiFi, the **router assigns your IP automatically** via DHCP. The 4 steps spell **DORA**:

```
📱 DISCOVER:  "I just joined! Any DHCP server?"     (broadcast)
📡 OFFER:     "Yes! Take 192.168.1.5"
📱 REQUEST:   "Thanks, I'll take 192.168.1.5"
📡 ACK:       "Confirmed. Lease: 24 hrs"
```

DHCP gives you **4 things** (not just an IP):
```
IP address:   192.168.1.5      ← your identity
Subnet mask:  255.255.255.0    ← to decide local vs remote (Chapter 4)
Gateway:      192.168.1.1      ← the router (where remote traffic goes)
DNS server:   8.8.8.8          ← to resolve names (Chapter 5)
```
- **Ports:** 67 (server), 68 (client). Runs over **UDP**.
- If the DHCP server is down → device may self-assign a **link-local APIPA** address (`169.254.x.x`) and can't reach the wider network.

---

## 3.4 NAT — many private devices, one public IP

> NAT (Network Address Translation) lets a whole network share **one** public IP. The router **rewrites** the source IP:port on the way out and **reverses** it on the way back.

```
📱 Phone  192.168.1.5:54321  ┐
💻 Laptop 192.168.1.6:50000  ├→ [Router/NAT] → 103.x.x.x : (new port)
📺 TV     192.168.1.7:48000  ┘

NAT translation table:
   103.x.x.x:61001 ↔ 192.168.1.5:54321  (phone)
   103.x.x.x:61002 ↔ 192.168.1.6:50000  (laptop)
   103.x.x.x:61003 ↔ 192.168.1.7:48000  (TV)
```

- **Out:** router rewrites source private IP:port → public IP:new port.
- **Back:** reply arrives at `103.x.x.x:61002` → router looks up table → "that's the laptop" → rewrites back.
- Using **ports** to tell devices apart is called **PAT** (Port Address Translation) — the common home form.

> 🔑 This is why your whole home (you + roommate) shares one public IP but data still reaches the right device. And why **incoming connections can't reach your phone directly** — there's no table entry until your device starts the connection.

---

## 3.5 Client-Server model (WhatsApp vs Chrome)

You connect to a **server**, not directly to another person. Two patterns:

| | Chrome → Google | WhatsApp message |
|---|----------------|------------------|
| Who opens connection | You | You (friend opens theirs separately) |
| Final recipient | **Google itself** (server = destination) | **Your friend** (server = middleman) |
| Pattern | **Request–reply** (ask → answer → done) | **Push** (line stays open for surprise msgs) |

### Why WhatsApp doesn't store your friend's IP
- Friend's IP changes constantly (WiFi ↔ 4G) and is behind **NAT** (unreachable directly).
- Instead, the **friend's phone keeps a persistent connection OPEN to the server** (phone initiates → NAT allows replies back down it).
- Server identifies the friend by **user ID** (phone number), not IP, and **pushes** the message down that open pipe. If offline → stores it for later.

> 🔑 The phone *must* initiate (NAT blocks the server from reaching in). This is the universal trick behind chat, push notifications, "typing…", and online status.

### Why Chrome doesn't need a persistent pipe
Chrome only needs data **right after it asks** (request–reply). WhatsApp needs to receive messages **any time**, so it holds a line open.

---

## 🎤 Interview-ready answers

**Q: What is DHCP?**
> DHCP auto-assigns IP config when a device joins, via DORA: Discover, Offer, Request, Acknowledge. The lease includes IP, subnet mask, default gateway, and DNS server, and renews over time.
> *Follow-up — if DHCP is down?* Device falls back to a link-local APIPA address (169.254.x.x) and can't reach the wider network.

**Q: What is NAT and why use it?**
> NAT lets many private devices share one public IP. The router rewrites the private source address on outgoing packets and tracks the mapping by port, so replies return to the right device. Main reason: conserve scarce IPv4 addresses (plus it hides the internal network).
> *Follow-up — how does the router know which device a reply belongs to?* A translation table keyed by the public port it assigned — that's PAT.

**Q: Public vs private IP?**
> Public IPs are globally unique and routable on the internet. Private IPs (10/8, 172.16/12, 192.168/16) are reused inside local networks and reach the internet only via NAT.

---

## ⚠️ Traps
- Private IPs **can** repeat across homes; public IPs **cannot** on the internet.
- A server can't initiate a connection to a device behind NAT — the device must reach out first.

[← Addressing](./02-addressing-ip-mac-ports.md) | [Index](./00-index.md) | [Next: Subnetting →](./04-subnetting-and-cidr.md)