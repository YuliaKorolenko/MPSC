# Multi producer single concumer

---

### FAAQueue

I implemented the `FAAQueue<E>` (Fetch-and-Add Queue) as part of a course on parallel programming at ITMO. This queue structure is designed for efficient parallel data processing and supports multiple producers and consumers.

* **File:** `FAAQueue.kt`
---

### MPSC Jiffy

It was brave attempt to implement a Multiple Producer, Single Consumer (MPSC) queue, inspired by algorithms from the research article [“Wait-Free Algorithms for Circular Buffers”](https://arxiv.org/pdf/2010.14189). However, at the moment, tests do not always take place without deadlocks 

* **Project File:** `Main.kt`
---



