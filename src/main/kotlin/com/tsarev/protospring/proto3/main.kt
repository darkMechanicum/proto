package com.tsarev.protospring.proto3

class SmartSwitch<T : Any> {
    private var shouldSkip = 0
    val it get() = _it
    private lateinit var _it: T
    val breakMulti get(): (T) -> Unit = { shouldSkip = Int.MAX_VALUE }
    val skip get() = 1.skip
    private val conditions: MutableList<Pair<(T) -> Boolean, (T) -> Unit>> = ArrayList()
    fun doSwitch(value: T?) = value?.let {
        this._it = it
        for (condition in conditions) {
            if (shouldSkip == 0 && condition.first(it))
                condition.second(it)
            else if (shouldSkip > 0) shouldSkip -= 1
        }
    }

    val Int.skip: (T) -> Unit get() = { shouldSkip += this }
    operator fun (() -> Boolean).compareTo(op: (T) -> Unit): Int = run { conditions.add({ arg: T -> this@compareTo() } to op) }.let { 0 }
    operator fun T.compareTo(op: (T) -> Unit): Int = run { conditions.add({ arg: T -> arg == this@compareTo } to op) }.let { 0 }
    fun doInvariant(op: (T) -> Unit): Unit = run { conditions.add({ _: T -> true } to op) }
    infix fun Any?.and(op: (T) -> Unit) = apply {
        val (oldCond, oldOp) = conditions.removeAt(conditions.size - 1)
        conditions.add(oldCond to { _: T -> oldOp(it); op(it) })
    }
}

fun <T : Any> T?.smartSwitch(op: SmartSwitch<T>.() -> Unit) = SmartSwitch<T>().apply { op() }.doSwitch(this)

fun main() {
    30.smartSwitch {
        fun divisibleBy(n: Int) = run { { it % n == 0 } > { println("\tdivisible by $n") } }

        doInvariant { println("$it is:") }
        divisibleBy(7)
        divisibleBy(4) and skip
        divisibleBy(2)
        divisibleBy(15) and 2.skip
        divisibleBy(3)
        divisibleBy(5);
        100 > { println("\tis 100") }
        { it % 5 == 0 } > { println("\tdivisible by 5") }
    }
}