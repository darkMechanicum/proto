package com.tsarev.protospring.proto6.core

/**
 * Class that can perform value type transformation logic.
 */
abstract class TypedProvider<Original, Current>(
    private val pullPrevious: TypedProvider<Original, Current>? = null
) {

    lateinit var fromRoot: (Original) -> Current


    lateinit var toRoot: (Current) -> Original

    protected fun <Next, NextProvider : TypedProvider<Original, Next>> serialize(
        nextProvider: NextProvider,
        block: (Current) -> Next
    ) = object : OutTransformerExpect<Original, Current, Next, NextProvider> {
        override fun deserialize(block: (Next) -> Current) = nextProvider.apply {
            fromRoot = { root -> block(this@TypedProvider.fromRoot(root)) }
            toRoot = { current -> this@TypedProvider.toRoot(block(current)) }
        }
    }

    interface OutTransformerExpect<Original, Current, Next, NextProvider> {
        infix fun deserialize(block: (Next) -> Current): NextProvider
    }

}