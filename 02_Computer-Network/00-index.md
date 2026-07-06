# 🌐 Computer Networks — Interview Notes

> Built from our learning conversation + the *CN Mastery* reference sheet.
> Goal: walk into a fresher/dev networking round and answer cold.

## 📚 Chapters

| # | Chapter | Covers |
|---|---------|--------|
| 1 | [Layers & Devices](./01-layers-and-devices.md) | OSI vs TCP/IP, encapsulation, the request journey, Hub/Switch/Router/Modem/AP |
| 2 | [Addressing: IP, MAC & Ports](./02-addressing-ip-mac-ports.md) | IP vs MAC, IPv4/IPv6, ports & sockets, well-known ports |
| 3 | [Your Network: DHCP & NAT](./03-your-network-dhcp-nat.md) | Public vs private IP, DHCP/DORA, NAT/PAT, client-server (WhatsApp vs Chrome) |
| 4 | [Subnetting & CIDR](./04-subnetting-and-cidr.md) | Network/host split, subnet mask, the AND check, CIDR math |
| 5 | [Protocols: TCP, UDP, DNS, HTTP/S](./05-protocols-tcp-udp-dns-http.md) | TCP vs UDP, handshake, DNS, HTTP/HTTPS/TLS, ARP/ICMP, full google.com journey |
| 6 | [Top Interview Questions](./06_Interview.md) | The 11 most-asked SWE/SDE2 CN questions with spoken-out-loud answers (incl. L4 vs L7 load balancing) |

## 🎯 The 6 must-knows (learn these cold)
1. **OSI layers + TCP/IP model** — order + one job per layer.
2. **TCP vs UDP** — connection-oriented+reliable vs connectionless+fast, and which apps use each.
3. **TCP 3-way handshake** — SYN, SYN-ACK, ACK — and why 3 not 2.
4. **IP addressing + a /26 subnet calculation** — hosts, private ranges.
5. **DNS** — how google.com becomes an IP.
6. **"What happens when you type google.com"** — the end-to-end story (the single most-asked question).

## ⏱️ If you have only 30 minutes
Jump to the **Revision Card** at the end of [Chapter 5](./05-protocols-tcp-udp-dns-http.md#-30-minute-revision-card).

## 🔢 Numbers to know cold
- TCP header **20 bytes**, UDP **8 bytes**
- IPv4 **32 bits**, IPv6 **128 bits**
- Ports: HTTP **80**, HTTPS **443**, DNS **53**, DHCP **67/68**, SSH **22**, SMTP **25**
- Usable hosts = **2^(32 − prefix) − 2**
- Private ranges: **10.0.0.0/8**, **172.16.0.0/12**, **192.168.0.0/16**