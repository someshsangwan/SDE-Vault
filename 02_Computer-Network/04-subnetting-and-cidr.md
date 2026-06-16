# Chapter 4 — Subnetting & CIDR

[← DHCP & NAT](./03-your-network-dhcp-nat.md) | [Index](./00-index.md) | [Next: Protocols →](./05-protocols-tcp-udp-dns-http.md)

---

## 4.1 The core idea: an IP has TWO parts

```
192.168.1 . 5
└───┬───┘   └┬┘
 Network    Host
  part      part
"which       "which
 network"    device"
```

> Devices on the **same network** share the **same network part**; they differ only in the **host part**. Like *street name + house number*.

**Subnetting** = splitting one big network into smaller **subnets** by borrowing bits from the host part.

---

## 4.2 The Subnet Mask

The mask tells you where to split network vs host. **255 = network, 0 = host.**

```
IP:    192 .168 .1  .5
Mask:  255 .255 .255.0
        └────┬────┘  └┬┘
       NETWORK part  HOST
   → Network = 192.168.1.x,  hosts = .1 to .254
```

---

## 4.3 CIDR notation (the `/24`)

Instead of `255.255.255.0`, write **`/24`** = "first 24 bits are the network part" (out of 32 total).

```
255.255.255.0
11111111.11111111.11111111.00000000
└──────── 24 network bits ─┘└ 8 host ┘
```

| CIDR | Subnet mask | Host bits | Usable hosts |
|------|-------------|-----------|--------------|
| /24 | 255.255.255.0 | 8 | 2^8 − 2 = 254 |
| /25 | 255.255.255.128 | 7 | 2^7 − 2 = 126 |
| /26 | 255.255.255.192 | 6 | 2^6 − 2 = 62 |
| /27 | 255.255.255.224 | 5 | 2^5 − 2 = 30 |
| /30 | 255.255.255.252 | 2 | 2^2 − 2 = 2 (point-to-point) |

> 🔑 Bigger CIDR number → more network bits → **smaller** network (fewer hosts).

### Why "Classless"?
Old system forced rigid **Class A/B/C** sizes (254, 65k, or 16M — nothing in between), wasting millions of IPs. **CIDR** lets you split at *any* bit boundary (e.g. `/21` = 2046 hosts), fitting the real need. This slowed IPv4 exhaustion. It also enables **route summarization** (one CIDR entry covers many networks → smaller routing tables).

---

## 4.4 ⭐ The one formula to know cold

```
Usable hosts = 2^(32 − prefix) − 2
```

**Subtract 2** because:
- First address (all host bits 0) = **network address** (the name-plate).
- Last address (all host bits 1) = **broadcast address** (announce to all).
- Neither can be assigned to a device.

**Worked example — `192.168.1.0/26`:**
```
Host bits = 32 − 26 = 6
Total addresses = 2^6 = 64
Usable = 64 − 2 = 62
Network address  = 192.168.1.0
Broadcast        = 192.168.1.63
Usable range     = 192.168.1.1 to .62
```

> 💡 **Aha:** Subnetting = splitting a hostel floor into wings. /24 = whole floor; /26 = 4 wings. More network bits → smaller groups, more groups. Each group reserves 2 (name-plate + announcement).

---

## 4.5 🔑 How a device uses the mask: local vs remote (bitwise AND)

Every time your device sends a packet, it asks: *"Is the destination on MY network (local → switch/MAC) or elsewhere (→ router)?"*

It does **`IP AND mask`** on both IPs to extract their network parts, then compares.
Rule: `n AND 255 = n` (keep), `n AND 0 = 0` (erase).

```
My IP 192.168.1.5  AND 255.255.255.0 → my network   = 192.168.1.0

Dest 192.168.1.9   AND mask → 192.168.1.0  → SAME ✅ → LOCAL  (ARP for its MAC, send via switch)
Dest 142.250.1.1   AND mask → 142.250.1.0  → DIFFERENT ❌ → REMOTE (send to router/gateway)
```

```
        Are the two network parts equal?
        ┌───────────┴───────────┐
      SAME                    DIFFERENT
        │                         │
   LOCAL delivery           Send to ROUTER
   (ARP → MAC → switch)     (default gateway → internet)
```

> This is the actual mechanism behind "is it local?" — using the **mask DHCP gave you**, which is why DHCP hands out the mask AND the gateway together.

---

## 🎤 Interview-ready answers

**Q: What is subnetting and CIDR?**
> Subnetting splits one large network into smaller logical networks by borrowing bits from the host part. CIDR writes the split as a slash, like /24 = first 24 bits are network, rest are hosts. A /24 gives 256 total, 254 usable (minus network + broadcast). Benefits: organisation, security, routing efficiency.
> *Follow-up — usable hosts in a /26?* 64 total minus 2 = 62.

**Q: How does a device know if a destination is local?**
> It ANDs both its own IP and the destination IP with the subnet mask to get their network parts, then compares. Same → local, deliver via MAC/switch (ARP). Different → send to the default gateway (router).

---

## ⚠️ Traps
- **Don't forget to subtract 2.** A /26 has 64 addresses but only **62 usable**.
- The network address (all-0 host) and broadcast (all-1 host) can't be assigned.

[← DHCP & NAT](./03-your-network-dhcp-nat.md) | [Index](./00-index.md) | [Next: Protocols →](./05-protocols-tcp-udp-dns-http.md)