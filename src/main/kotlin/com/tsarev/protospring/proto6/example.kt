package com.tsarev.protospring.proto6

import com.tsarev.protospring.proto6.core.SerializeAble

class SomeNested : SerializeAble() {

    var data by long()
        .validate { it > 100 }.withMessage { "Data must be more than 100 but is ${it}." }
        .validate { it % 2 == 0L }.withMessage { "Data must be even but is ${it}." }
        .validate("Bad data") { it == 95L }
}

class Some : SerializeAble() {

    var first by long()

    var second by string()
        .serialize { it.toLong() }
        .deserialize { "$it" }

    var inner: SomeNested by nested(::SomeNested)

    var inner2 by nested(::Some)

    // $string$delegate.getValue()
}

class Some2 : SerializeAble() {

    var first: Long = 1

    var second: String = "1"

    var inner: SomeNested? = null

    var inner2: Some? = null
}

fun main() {
    val size = Sizer().getObjectSize { Some() }
    println(size)
    val size2 = Sizer().getObjectSize { Some2() }
    println(size2)
}

class Sizer {

    fun <T> getObjectSize(ctor: () -> T): Long {
        var result: Long = 0
        val objects = arrayOfNulls<Any>(10000)
        val throwAway = ctor()
        val startMemoryUse = memoryUse()
        val start = System.currentTimeMillis()
        for (idx in objects.indices) {
            objects[idx] = ctor()
        }
        println("Time - ${System.currentTimeMillis() - start}")
        val endMemoryUse = memoryUse()
        return (endMemoryUse - startMemoryUse) / 10000
    }

    private fun memoryUse(): Long {
        collectGarbage()
        collectGarbage()
        val totalMemory = Runtime.getRuntime().totalMemory()
        collectGarbage()
        collectGarbage()
        val freeMemory = Runtime.getRuntime().freeMemory()
        return totalMemory - freeMemory
    }

    private fun collectGarbage() {
        try {
            System.gc()
            Thread.sleep(100)
            System.runFinalization()
            Thread.sleep(100)
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
    }
}