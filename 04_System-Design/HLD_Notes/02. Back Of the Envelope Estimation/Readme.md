# Chapter 2: Back-of-the-Envelope Estimation

## Introduction
Back-of-the-envelope estimation is a crucial skill in system design interviews. It involves making quick, rough calculations to assess system capacity or performance. According to Jeff Dean, Google Senior Fellow, these estimates help evaluate whether designs meet requirements through thought experiments and common performance benchmarks.

This chapter covers key concepts, methodologies, and examples to build proficiency in scalability and estimation.

---

## Section 1: Key Concepts

### Power of Two
Understanding data volume in terms of powers of two is fundamental:

<img src="./images/power-of-two.png" alt="power-of-two" width="500" />

This knowledge helps in performing accurate storage and bandwidth calculations.

---

### Latency Numbers Every Programmer Should Know
Latency numbers represent the time taken for various operations in computing systems. These provide insights into relative performance:

| Operation                | Latency (2020) |
|--------------------------|----------------|
| L1 Cache Access          | 0.5 ns         |
| L2 Cache Access          | 7 ns           |
| Main Memory Access       | 100 ns         |
| SSD Random Read          | 150 µs         |
| HDD Random Seek          | 10 ms          |
| Round-Trip in Data Center| 500 µs         |
| Inter-Region Data Center | 150 ms         |

**Key Insights:**
- Memory is fast, disk is slow.
- Avoid disk seeks whenever possible.
- Compress data before transmitting over the internet to save bandwidth.


---

### Availability Numbers
High availability (HA) ensures minimal downtime. Availability is expressed in **nines**:
- **99% (Two Nines):** ~3.65 days/year of downtime
- **99.9% (Three Nines):** ~8.8 hours/year of downtime
- **99.99% (Four Nines):** ~52 minutes/year of downtime
- **99.999% (Five Nines):** ~5.3 minutes/year of downtime
- **99.9999% (Six Nines):** ~31.56 seconds/year of downtime


Cloud providers like Amazon, Google, and Microsoft aim for SLAs (Service Level Agreements) of **99.9% or higher**.

---

## Section 2: Example Estimation - Twitter QPS and Storage Requirements

### Assumptions
- **300 million monthly active users (MAU).**
- **50% daily active users (DAU).**
- **Average tweets/user/day:** 2.
- **10% of tweets contain media.**
- **Data retention:** 5 years.

### Estimations
1. **Query Per Second (QPS):**
   - DAU = \( 300M x 50\% = 150M \)
   - Tweets QPS = \( 150M x 2 tweets / 24 hour / 3600 seconds = ~3500 )
   - Peak QPS = \( 2 x 3500 = ~7000 \)

2. **Media Storage:**
   - **Tweet Size Components:**
     - `tweet_id`: 64 bytes
     - `text`: 140 bytes
     - `media`: 1 MB
   - **Daily Media Storage:** \( 150M x 2 x 10\% x 1MB = 30TB per day \)
   - **5-Year Storage:** \( 30TB x 365 x 5 = ~55PB \)

---

## Section 3: Tips for Effective Estimation

### 1. Rounding and Approximation
Precision is not critical; focus on the process. Simplify complex calculations using round numbers. For example:
- \( 99987 / 9.1 \) can be approximated as \( 100,000 / 10 = 10,000 \).

### 2. Write Down Assumptions
Document assumptions clearly for future reference.

### 3. Label Units
Avoid ambiguity by labeling units (e.g., `5 MB` instead of `5`).

### 4. Common Estimation Scenarios
- **QPS (Queries Per Second):** Measure traffic intensity.
- **Peak QPS:** Account for traffic spikes.
- **Storage Requirements:** Estimate total data needs.
- **Cache Requirements:** Evaluate memory requirements for caching.
- **Number of Servers:** Calculate hardware needs based on workload.

---

## Section 4: Example Estimation - Facebook QPS, Storage, Cache, and Servers

### Assumptions
- **Total Users:** 1 billion (\(10^9\)).
- **Daily Active Users (DAU):** 25% = 250 million.
- **User Activity per Day:** 5 read queries + 2 write queries = 7 queries/user/day.
- **Write frequency:** 2 posts per day.
- **Post Size:** 250 characters (~1 KB per post).
- **Image uploads:** 10% of users upload images daily.
- **Average image size:** ~300 KB.
- **Data retention:** 5 years.

### Estimations

1. **Queries Per Second (QPS):**
   - **Daily Queries:** \( 250\text{M users} \times 7\text{ queries/day} = 1.75\text{ billion queries/day} \)
   - **Seconds per Day:** ~100,000 (approximated from 86,400 for simplicity)
   - **QPS:** \( \frac{1.75 \times 10^9}{10^5} = 17,500\text{ QPS} \) (approximated to **18,000 QPS**)

2. **Post Data Storage:**
   - **Daily Post Storage:** \( 250\text{M users} \times 1\text{ KB/day} = 250\text{ GB/day} \)
   - **5-Year Post Storage:** \( 250\text{ GB/day} \times 365 \times 5 \approx 250\text{ GB} \times 2000 \text{ days} = 500\text{ TB} \)

3. **Image Storage:**
   - **Daily Image Uploads:** \( 10\% \text{ of DAU} = 25\text{ million images/day} \)
   - **Daily Image Storage:** \( 25\text{M} \times 300\text{ KB} = 7.5\text{ TB/day} \approx 8\text{ TB/day} \)
   - **5-Year Image Storage:** \( 8\text{ TB/day} \times 2000 \text{ days} = 16\text{ PB} \)

4. **RAM Cache Requirements:**
   - **Assumption:** Cache the last 5 posts per user.
   - **Cache Size per User:** \( 5 \text{ posts} \times 500\text{ bytes} = 2.5\text{ KB} \)
   - **Total Cache Size:** \( 250\text{M users} \times 2.5\text{ KB} = 625\text{ GB} \) (approximated to **750 GB** for overhead/padding)
   - **Cache Servers:** If one machine handles 75 GB of cache, we need \( \frac{750\text{ GB}}{75\text{ GB}} = 10\text{ cache servers} \).

5. **Server Requirements (Request Handling):**
   - **Latency per Request:** 500 ms.
   - **Single Server Capacity:** 50 threads \(\rightarrow\) 100 requests per second.
   - **Total Servers Needed:** \( \frac{18,000\text{ QPS}}{100\text{ req/sec}} = 180\text{ servers} \).

### Summary of Resource Requirements

| Resource | Estimated Value |
|---|---|
| **Queries per second (QPS)** | 18,000 QPS |
| **Storage for posts (5 years)** | 500 TB |
| **Storage for images (5 years)** | 16 PB |
| **RAM required for caching** | 750 GB |
| **Cache servers required** | 10 (each 75 GB) |
| **Servers needed to handle requests** | 180 servers |

### CAP Theorem Considerations for Facebook
- **Partition Tolerance (P):** Essential, as data is distributed across global datacenters.
- **Availability (A):** Critical, users must be able to view their feeds anytime.
- **Consistency (C):** Can be relaxed in favor of availability. Eventual consistency is perfectly acceptable (e.g., a friend's post doesn't need to appear instantly on every feed).


