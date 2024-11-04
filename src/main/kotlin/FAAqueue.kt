package org.example

import kotlinx.atomicfu.*

class FAAQueue<E> {
    private val head: AtomicRef<Segment>
    private val tail: AtomicRef<Segment>
    private val enqIdx = atomic(0L)
    private val deqIdx = atomic(0L)

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(element: E) {
        while (true) {
            var curTail = tail.value
            val i = enqIdx.getAndIncrement()
            var s = findSegment(curTail, i / SEGMENT_SIZE)
            moveTailForward(s, curTail)
            if (s.cas((i % SEGMENT_SIZE).toInt(), null, element)) {
                return
            }
        }
    }

    fun findSegment(cur: Segment, index: Long): Segment {
        var curSegment = cur
        while (index > curSegment.getId()) {
            val next = curSegment.next.value
            if (next === null) {
                val fSegment = Segment()
                fSegment.setId(curSegment.getId() + 1)
                curSegment.next.compareAndSet(null, fSegment)
            }
            curSegment = curSegment.next.value!!
        }
        return curSegment
    }

    fun moveTailForward(curSegment: Segment, curTail: Segment) {
        if (curSegment.getId() > curTail.getId()) {
            tail.compareAndSet(curTail, curSegment)
        }
    }

    fun moveHeadForward(curSegment: Segment, curHead: Segment) {
        if (curSegment.getId() > curHead.getId()) {
            head.compareAndSet(curHead, curSegment)
        }
    }

    /**
     * Retrieves the first element from the queue and returns it;
     * returns `null` if the queue is empty.
     */
    fun dequeue(): E? {
        while (true) {
            if (isEmpty) {
                return null
            }
            val curHead = head.value
            val i = deqIdx.getAndIncrement()
            val s = findSegment(curHead, i / SEGMENT_SIZE)
            moveHeadForward(s, curHead)
            if (s.cas((i % SEGMENT_SIZE).toInt(), null, DONE)) {
                continue
            }
            return s.get((i % SEGMENT_SIZE).toInt()) as E?
        }
    }

    /**
     * Returns `true` if this queue is empty, or `false` otherwise.
     */
    val isEmpty: Boolean
        get() {
            return (deqIdx.value >= enqIdx.value)
        }
}

class Segment {
    val next = atomic<Segment?>(null)
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)
    private var id: Long = 0L

    fun get(i: Int) = elements[i].value
    fun cas(i: Int, expect: Any?, update: Any?) = elements[i].compareAndSet(expect, update)
    private fun put(i: Int, value: Any?) {
        elements[i].value = value
    }

    fun setId(value: Long) {
        this.id = value
    }

    fun getId(): Long {
        return this.id
    }
}

private val DONE = Any()
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS
