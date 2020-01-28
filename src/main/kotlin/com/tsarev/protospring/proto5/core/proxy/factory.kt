package com.tsarev.protospring.proto5.core.proxy

import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

//
// Contains classes for creating entity proxies.
//

/**
 * Key for additional [EntityProxy] parameter, used mostly to cache proxy related metadata.
 */
class ProxyKey<T : Any> private constructor(
    val valueKlass: KClass<T>,
    private val identifier: Long
) {
    override fun equals(other: Any?) = (other as? ProxyKey<*>)?.identifier == this.identifier
    override fun hashCode() = identifier.toInt()

    companion object {
        private val identifierCounter = AtomicLong()
        fun <T : Any> create(klass: KClass<T>) = ProxyKey(klass, identifierCounter.incrementAndGet())
        inline fun <reified T : Any> create() = create(T::class)
    }
}

/**
 * Common interface to create various proxies builders.
 * Is used to distinguish between various proxy creation strategies,
 * such as ByteBuddy proxy or Java Dynamic Proxy.
 */
interface EntityProxyFactory<F : EntityProxy> {

    /**
     * Create proxy instance.
     *
     * @param targetInterface interface to implement
     * @param isManaged is created proxy is managed by orm
     */
    fun <T : Any> createProxy(targetInterface: KClass<T>, isManaged: Boolean): T

    /**
     * Try to cast passed candidate to internal proxy class.
     */
    fun asProxy(candidate: Any): F?

}

/**
 * Common interface for base proxy functionality.
 */
interface EntityProxy {

    /**
     * Target proxy interface.
     */
    val targetInterface: KClass<*>

    /**
     * Get property value by its name.
     */
    operator fun get(pName: String): Any?

    /**
     * Get additional proxy property or calculate it if it not present.
     */
    fun <T : Any> get(proxyKey: ProxyKey<T>, op: () -> T): T

}