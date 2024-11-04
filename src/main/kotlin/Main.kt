package org.example

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

enum class IsSetStatus {
    EMPTY,
    SET,
    HANDLED
}

class Node<S> {
    var data: S? = null
    var isSet = AtomicReference(IsSetStatus.EMPTY)
}

class BufferList<S>(
    bufferSize: Int,
    val positionInQueue: Int,
    var prev: BufferList<S>?
) {
    var currBuffer: Array<Node<S>> = Array(bufferSize) { Node() }

    var next: AtomicReference<BufferList<S>?> = AtomicReference(null)
    var head: Int = 0
}

class MPSC<S : Any>(private val bufferSize: Int) {
    var headOfQueue: BufferList<S> = BufferList(bufferSize, 1, null)
    val tailOfQueue = AtomicReference<BufferList<S>>(headOfQueue)
    private val tail = AtomicInteger(0)

    fun enqueue(data: S) {
        var location = tail.getAndIncrement()
        var isLastBuffer = true
        var tempTail = tailOfQueue.get()!!
        var numElements = bufferSize * tempTail.positionInQueue

        while (location >= numElements) {
            if (tempTail.next.get() == null) {
                val newArr = BufferList<S>(bufferSize, (tempTail.positionInQueue + 1), tempTail)
                if (tempTail.next.compareAndSet(null, newArr)) {
                    tailOfQueue.compareAndSet(tempTail, newArr)
                }
                // else: delete newArr
            }
            tempTail = tailOfQueue.get()!!
            numElements = bufferSize * tempTail.positionInQueue
        }

        var prevSize = bufferSize * tempTail.positionInQueue.minus(1)
        while (location < prevSize) {
            tempTail = tempTail.prev!!
            prevSize = bufferSize * tempTail.positionInQueue.minus(1)
            isLastBuffer = false
        }

        val n = tempTail.currBuffer[location - prevSize]
        if (n.isSet.get() == IsSetStatus.EMPTY) {
            n.data = data
            n.isSet.set(IsSetStatus.SET)

//            if (location - prevSize == min(bufferSize - 1, 2) && isLastBuffer) {
//                val newArr = BufferList(bufferSize, (tempTail.positionInQueue + 1), tempTail)
//                tempTail.next.compareAndSet(null, newArr)
//                // else: delete newArr
//            }
        }
    }

    fun moveToNextBuffer(): Boolean {
        if (headOfQueue.head >= bufferSize) {
            if (headOfQueue == tailOfQueue.get()) {
                return false
            }
            val next = headOfQueue.next.get() ?: return false
            headOfQueue = next
        }
        return true
    }

    fun dequeue(): S? {
        var n = headOfQueue.currBuffer[headOfQueue.head]

        while (n.isSet.get() == IsSetStatus.HANDLED) {
            headOfQueue.head++
            val res = moveToNextBuffer()
            if (!res) {
                return null
            }
            n = headOfQueue.currBuffer[headOfQueue.head]
        }

        if (headOfQueue == tailOfQueue.get() && headOfQueue.head == tail.get() % bufferSize) {
            return null
        }

        if (n.isSet.get() == IsSetStatus.SET) {
            headOfQueue.head++
            moveToNextBuffer()
            return n.data
        }

        if (n.isSet.get() == IsSetStatus.EMPTY) {
            var tempHeadOfQueue = headOfQueue
            var tempHead = tempHeadOfQueue.head
            var tempN = tempHeadOfQueue.currBuffer[tempHead]
            val res = scan(tempHeadOfQueue, tempHead, tempN)

            if (res == null) {
                return null
            }
            tempHead = res.tempHead
            tempHeadOfQueue = res.tempHeadOfQueue
            tempN = res.tempN
            // rescan
            val data = tempN.data
            tempN.isSet.set(IsSetStatus.HANDLED)
            if (tempHeadOfQueue == headOfQueue && tempHead == headOfQueue.head) {
                headOfQueue.head++
                moveToNextBuffer()
            }
            return data
        }
        return null
    }


//    fun rescan(
//        headOfQueue: BufferList<S>,
//        tempHeadOfQueue: BufferList<S>,
//        tempHead: Int,
//        tempN: Node<S>?
//    ) {
//        var scanHeadOfQueue = headOfQueue
//        var scanHead = scanHeadOfQueue.head
//        var tempHead = tempHead
//        var tempHeadOfQueue = tempHeadOfQueue
//        var tempN = tempN
//
//        while (scanHeadOfQueue != tempHeadOfQueue || scanHead < tempHead - 1) {
//            if (scanHead >= bufferSize) {
//                scanHeadOfQueue = scanHeadOfQueue.next.get() ?: break
//                scanHead = scanHeadOfQueue.head
//            }
//
//            val scanN = scanHeadOfQueue.currBuffer[scanHead]
//
//            // Если найден элемент, который установлен, обновляем `tempHead`, `tempHeadOfQueue` и `tempN`
//            if (scanN.isSet.get() == IsSetStatus.SET) {
//                tempHead = scanHead
//                tempHeadOfQueue = scanHeadOfQueue
//                tempN = scanN
//
//                // Перезапускаем цикл с начала очереди
//                scanHeadOfQueue = headOfQueue
//                scanHead = scanHeadOfQueue.head
//            } else {
//                // Переходим к следующему элементу
//                scanHead++
//            }
//        }
//    }

    class ScanParameters<S>(
        val tempHeadOfQueue: BufferList<S>,
        val tempHead: Int,
        val tempN: Node<S>
    )

    fun scan(
        tempHeadOfQueue: BufferList<S>,
        tempHead : Int,
        tempN: Node<S>
    ) : ScanParameters<S>? {
        var currentBuffer = tempHeadOfQueue
        var currentHead = tempHead
        var currentNode = tempN

        var moveToNewBuffer = false
        var bufferAllHandled = true

        while (currentNode.isSet.get() != IsSetStatus.SET) {
            currentHead++

            if (currentNode.isSet.get() != IsSetStatus.HANDLED) {
                bufferAllHandled = false
            }

            if (currentHead >= bufferSize) {
                if (bufferAllHandled && moveToNewBuffer) {
                    var foldResult = true

                    // start fold
                    if (currentBuffer == tailOfQueue.get()) {
                        foldResult = false
                    } else {
                        val next = currentBuffer.next.get()
                        val prev = currentBuffer.prev

                        if (next == null) {
                            foldResult = false
                        } else {
                            next.prev = prev
                            prev?.next?.set(next)

                            currentBuffer = next
                            currentHead = currentBuffer.head

                            bufferAllHandled = true
                            moveToNewBuffer = true
                            foldResult = true
                        }
                    }
                    // end fold

                    if (!foldResult) {
                        return null
                    }
                } else {
                    val nextBuffer = currentBuffer.next.get()
                    if (nextBuffer == null) {
                        return null
                    }
                    currentBuffer = nextBuffer
                    currentHead = currentBuffer.head

                    bufferAllHandled = true
                    moveToNewBuffer = true
                }
            }
            currentNode = currentBuffer.currBuffer[currentHead]
        }

        return ScanParameters(currentBuffer, currentHead, currentNode)
    }

}


fun main() {
    var bufferSize = 3
    val queue = MPSC<Int>(bufferSize = bufferSize)
    println("after queue")
    for (i in 1..10) {
        queue.enqueue(i)  // Добавляем числа от 1 до 10 в очередь
        println("Добавлено: $i")
        println("headOfQueue")
        for (el in 0..bufferSize - 1) {
            print(
                queue.headOfQueue.currBuffer.get(el).data.toString()
                        + " "
                        + queue.headOfQueue.currBuffer.get(el).isSet.get().toString() + ", "
            )
        }
        println()

        println("tailOfQueue")
        for (el in 0..bufferSize - 1) {
            print(
                queue.tailOfQueue.get().currBuffer[el].data.toString()
                        + " "
                        + queue.tailOfQueue.get().currBuffer[el].isSet.get().toString() + ", "
            )
        }
        println()
        println()
        println()

        if (i % 2 == 0) {
            println("RESULT: " + queue.dequeue().toString())
        }
    }

}