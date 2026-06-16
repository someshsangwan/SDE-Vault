# Chapter 2 — Addressing: IP, MAC & Ports

[← Layers & Devices](./01-layers-and-devices.md) | [Index](./00-index.md) | [Next: DHCP & NAT →](./03-your-network-dhcp-nat.md)

---

## 2.1 IP vs MAC — the most important distinction

> **MAC = WHO you are** (fixed identity). **IP = WHERE you are** (current location).

| Feature | MAC Address | IP Address |
|---------|-------------|------------|
| Full form | Media Access Control | Internet Protocol |
| Layer | Data Link (L2) | Network (L3) |
| Looks like | `AA:BB:CC:11:22:33` | `192.168.1.5` |
| Assigned by | Manufacturer (burned into NIC) | Network / DHCP / ISP |
| Changes? | **Never** (fixed to hardware) | **Yes** (per network) |
| Scope | Local network (one hop) | Across the whole internet |
| Used by | Switch | Router |
| Identifies | The physical *device* | The device's *position* |

### Why we need BOTH
- **IP** is hierarchical (Country → City → Street) → enables **routing** across the internet.
- **MAC** is flat (just a unique ID) → delivers to the **exact device** on the local wire.

> 📦 **IP gets the packet to the right neighbourhood. MAC delivers it to the right doorstep.**

### 🔑 The killer insight (hop-by-hop)
```
IP (destination)  → stays the SAME the entire journey   ("final destination")
MAC (destination) → CHANGES at every hop                ("next device only")
```
At each router, the IP stays fixed (e.g. Google's IP), but the MAC is rewritten for the next hop. The MAC of the next hop is found via **ARP** ([Chapter 5](./05-protocols-tcp-udp-dns-http.md#54-arp--ip--mac)).

> 💡 **Aha:** IP = your name's *home address* (changes if you move). MAC = your *name* (always you). Move from home to a café → address changes, name doesn't.

---

## 2.2 IPv4 vs IPv6

| Aspect | IPv4 | IPv6 |
|--------|------|------|
| Size | 32 bits | 128 bits |
| Notation | Decimal, dotted (`192.168.1.1`) | Hex, colons (`2001:db8::1`) |
| Address space | ~4.3 billion | Effectively unlimited |
| Header | 20 bytes min, variable | 40 bytes fixed |

- **Public IP** = unique on the whole internet. **Private IP** = reused inside home/office networks, never routed publicly.
- **Loopback** = `127.0.0.1` (your own machine, localhost).
- ⚠️ **IPv6 exists because IPv4 ran out (~4.3 billion), NOT for speed.**

### IPv4 classes (older, but still asked)
| Class | First octet | Default mask | Use |
|-------|-------------|--------------|-----|
| A | 0–127 | /8 | Very large networks |
| B | 128–191 | /16 | Medium networks |
| C | 192–223 | /24 | Small networks |
| D | 224–239 | — | Multicast |
| E | 240–255 | — | Experimental |

### Private ranges (RFC 1918) — memorize these 3
```
10.0.0.0/8     172.16.0.0/12     192.168.0.0/16
(plus 127.0.0.1 loopback and 169.254.0.0/16 link-local / APIPA)
```

---

## 2.3 Ports — which app on the device?

> **IP = which device. Port = which app/service on that device.**

- A port is a number **0–65535**, written after the IP with a colon: `192.168.1.5:443`.
- **IP + Port = a socket.** Every connection is `[Your IP:Port] ↔ [Server IP:Port]`.

Your phone runs many network apps on ONE IP. When data arrives, the **port** tells the OS which app gets it (Chrome vs WhatsApp vs Spotify).

### Why multiple browser tabs don't mix up
```
Tab 1: 192.168.1.5:54321 → google.com:443
Tab 2: 192.168.1.5:54322 → google.com:443   (same IP, different source ports ✅)
```

### Port ranges
| Type | Range | Used by |
|------|-------|---------|
| Well-known | 0–1023 | Standard services (server side) |
| Registered | 1024–49151 | Registered apps |
| Ephemeral / dynamic | 49152–65535 | Temporary, picked by the client per connection |

> 👉 **Server** listens on a **fixed known port**; **client** uses a **random temporary port** as its return address.

---

## 2.4 Common port numbers (memorize)

| Service | Port | Transport |
|---------|------|-----------|
| HTTP | 80 | TCP |
| HTTPS | 443 | TCP |
| DNS | 53 | UDP (TCP for large / zone transfer) |
| DHCP | 67 server, 68 client | UDP |
| SSH | 22 | TCP |
| SMTP | 25 | TCP |
| FTP | 20 data, 21 control | TCP |
| Telnet | 23 | TCP |

---

## 🎤 Interview-ready answers

**Q: Difference between MAC and IP?**
> A MAC is a fixed hardware address used for local delivery (L2). An IP is a logical address used for routing across networks (L3). IP is the final destination and stays constant; MAC changes at each hop.
> *Follow-up — how is ARP related?* ARP maps a known IP to the MAC needed to deliver on the local link.

**Q: Explain IP addressing — public vs private, IPv4 vs IPv6.**
> IPv4 is 32 bits, ~4.3 billion addresses that ran out. Private ranges (10/8, 172.16/12, 192.168/16) are reused locally; public addresses are globally unique. IPv6 is 128 bits with practically unlimited space.
> *Follow-up — how do private addresses reach the internet?* Through NAT at the router.

**Q: What is a port / socket?**
> A port (0–65535) identifies which app on a device a connection belongs to. IP + port together = a socket, pinpointing the exact app on the exact machine. Servers use fixed well-known ports; clients use ephemeral ports.

---

## ⚠️ Traps
- IPv6 exists due to **address exhaustion**, not speed.
- MAC **never** changes; IP **does**. Don't say the MAC changes per network.

[← Layers & Devices](./01-layers-and-devices.md) | [Index](./00-index.md) | [Next: DHCP & NAT →](./03-your-network-dhcp-nat.md)