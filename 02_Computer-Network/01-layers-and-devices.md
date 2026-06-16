# Chapter 1 — Network Layers & Devices

[← Index](./00-index.md) | [Next: Addressing →](./02-addressing-ip-mac-ports.md)

---

## 1.1 Why layers?

Sending data across the internet is split into **layers**, each with **one job**. Data goes **down** the layers on the sender, crosses the wire, and goes **up** the layers on the receiver.

> 🔑 Layers describe **how data is wrapped (encapsulated)** — *not* a time sequence of events.

---

## 1.2 The OSI Model (7 layers) — the teaching map

| Layer | Job (one line) | PDU (unit) | Examples |
|-------|----------------|------------|----------|
| 7 Application | Protocols the user's app speaks | Data | HTTP, DNS, SMTP, FTP |
| 6 Presentation | Encrypt, compress, format | Data | TLS, JPEG, ASCII |
| 5 Session | Open / manage / close conversations | Data | Sockets, RPC |
| 4 **Transport** | End-to-end delivery + ports | **Segment** | TCP, UDP |
| 3 **Network** | Logical addressing + routing | **Packet** | IP, ICMP (Router) |
| 2 **Data Link** | Local delivery via MAC + framing | **Frame** | Ethernet, ARP (Switch) |
| 1 Physical | Raw bits on the medium | Bits | Cables, Wi-Fi, hubs |

**Mnemonic (top→bottom):** *All People Seem To Need Data Processing.*

> The 4 that carry real weight in a fresher round: **Transport, Network, Data Link, Application**.

---

## 1.3 TCP/IP Model (4 layers) — what the internet actually runs

| TCP/IP layer | Maps to OSI | Does what | Protocols |
|--------------|-------------|-----------|-----------|
| Application | 5+6+7 | App protocols + format + sessions | HTTP, DNS, FTP, SMTP |
| Transport | 4 | End-to-end delivery + ports | TCP, UDP |
| Internet | 3 | Addressing + routing across networks | IP, ICMP, ARP |
| Link / Network access | 1+2 | Local delivery + physical medium | Ethernet, Wi-Fi |

### OSI vs TCP/IP

| Aspect | OSI | TCP/IP |
|--------|-----|--------|
| Layers | 7 | 4 |
| Role | Reference / teaching model | What the internet actually runs |
| Top layers | Session + Presentation + Application | One Application layer |

> 💡 **Aha:** OSI is the textbook's detailed 7-layer map; TCP/IP is real life's compact 4-layer map. Same work, just grouped.

---

## 1.4 Encapsulation — message → frame

As data goes **down** the stack, each layer **wraps** the chunk above in its own header (Data Link also adds a trailer). The receiver peels one header off per layer on the way up. That's why the same data is a **segment** at Transport, a **packet** at Network, a **frame** at Data Link.

```
[ Frame hdr [ IP hdr [ TCP hdr [  Data  ] ] ] Trailer ]
   Link        IP        TCP       App
```

> 💡 **Aha:** Encapsulation is gift-wrapping. The gift (data) stays the same; each layer adds a wrapper. The receiver unwraps one layer at a time.

### ⭐ Common doubt: "TCP comes before IP — how can the handshake happen if we don't know the IP yet?"
- The **IP is resolved FIRST**, at the Application layer, via **DNS** — *before* any TCP handshake.
- Layers are about **wrapping**, not **timing**. Every packet (even a SYN) flows down through all layers to be sent.
- So when TCP sends its first SYN, the destination IP is **already known** (from DNS) and the SYN rides *inside* an IP packet.

```
Step 0: DNS → "google.com is 142.250.x.x"   ✅ IP known here
Step 1: TCP handshake (SYN, SYN-ACK, ACK)
Step 2: HTTP request
```

---

## 1.5 Devices — by the layer they understand

> 🔑 A device's "intelligence" = the highest layer it understands.

```
   PC A ─┐
         ├─ [Switch L2: MAC] ── [Router L3: IP] ── Internet
   PC B ─┘   forwards by MAC      forwards by IP
             within a LAN          between networks
```

| Device | Layer | Forwards by | Smart? |
|--------|-------|-------------|--------|
| **Hub / Repeater** | 1 Physical | Nothing — copies bits to every port | No, a dumb broadcaster |
| **Switch / Bridge** | 2 Data Link | MAC address (within one LAN) | Learns which MAC is on which port |
| **Router** | 3 Network | IP address (between networks) | Runs routing, connects networks |

### The rest of the cast
- **Modem** — Modulator/Demodulator. Converts your digital signals ↔ the ISP's line signal (cable/DSL/fiber). Connects your home to the ISP.
- **Access Point (AP)** — lets wireless devices join the wired network (this is the "WiFi" part).
- **NIC** — Network Interface Card; the hardware in your device that connects to the network (holds the MAC).
- **Gateway** — translates between different protocols (works up to Application layer).

> 💡 **Aha:** Hub = a guy shouting to the whole room. Switch = a mailroom clerk delivering to the exact desk (MAC). Router = the post office sending between buildings (IP).

### "What is a router vs the WiFi box?"
Your home "WiFi box" from the ISP is **Modem + Router + Switch + Access Point in ONE device**. "Router" names its traffic-directing job; "WiFi" names its wireless-signal (AP) job. Same physical box.

---

## 🎤 Interview-ready answers

**Q: Walk me through the OSI model.**
> 7 layers, each with one job. Top-down: Application (HTTP/DNS), Presentation (encryption/format), Session (connections), Transport (TCP/UDP + ports), Network (IP + routing), Data Link (MAC + frames), Physical (bits). Mnemonic: *All People Seem To Need Data Processing.*
> *Follow-up — which layer is a router/switch?* Router = L3 (Network, IP); switch = L2 (Data Link, MAC).

**Q: OSI vs TCP/IP?**
> OSI is the 7-layer teaching reference; TCP/IP is the 4-layer model the internet actually runs on. TCP/IP merges OSI's top 3 into Application and bottom 2 into Link.

**Q: What is encapsulation?**
> Each layer wraps the data from the layer above in its own header (Data Link adds a trailer). Segment → Packet → Frame as it goes down.

**Q: Hub vs switch vs router?**
> Hub (L1) floods bits to every port. Switch (L2) forwards a frame by MAC to the right port. Router (L3) forwards packets between different networks by IP.
> *Follow-up — separate collision domains?* A switch gives each port its own; a hub puts everyone in one.

---

## ⚠️ Traps
- Don't say layers run in time order — they're **wrappers**, and the IP is known (via DNS) before TCP starts.
- Switch = MAC = within a LAN. Router = IP = between networks. **Don't mix these up.**

[← Index](./00-index.md) | [Next: Addressing →](./02-addressing-ip-mac-ports.md)