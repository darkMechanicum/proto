package com.tsarev.protospring.proto5.core.proxy

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

typealias PropName = String

object LookupUtils {

    val lookupCtor by lazy {
        MethodHandles.Lookup::class.java.getDeclaredConstructor(
            Class::class.java,
            Int::class.java
        ).apply { isAccessible = true }
    }

    fun createLookup(declaringClass: Class<out Any>) =
        lookupCtor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)

    fun getBindHandle(method: Method, declaringClass: Class<out Any>, proxy: Any) =
        createLookup(declaringClass)
            .unreflectSpecial(method, declaringClass)
            .bindTo(proxy)

    fun getBindHandle(method: Method, proxy: Any) =
        getBindHandle(
            method,
            method.declaringClass,
            proxy
        )

}

/**
 * Java dynamic proxy factory.
 */
object HibernateJDPFactory : EntityProxyFactory<JavaDynamicProxy> {
    override fun <T : Any> createProxy(targetInterface: KClass<T>, isManaged: Boolean): T {
        return Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(targetInterface.java),
            JavaDynamicProxy(targetInterface, isManaged)
        ) as T
    }

    override fun asProxy(candidate: Any) = candidate
        .takeIf { Proxy.isProxyClass(it.javaClass) }
        ?.let { Proxy.getInvocationHandler(it) }
        ?.let { it as? JavaDynamicProxy }
}

class JavaDynamicProxy(
    override val targetInterface: KClass<*>,
    val isManaged: Boolean = true
) : InvocationHandler, EntityProxy {

    private lateinit var bindProxy: Any

    private val indexedMethods = HashMap<PropName, Method>()

    private val methodsReverseIndex = HashMap<Method, PropName>()

    val data = HashMap<PropName, Any?>()

    private val indexedGetters = HashMap<PropName, () -> Any?>()

    private val indexedSetters = HashMap<PropName, (Any?) -> Unit>()

    private val pNameToGetter = HashMap<PropName, PropName>()

    private val pNameToSetter = HashMap<PropName, PropName>()

    private val unrecognizedMethods = HashSet<PropName>()

    private val additionalParameters = HashMap<ProxyKey<*>, Any?>()

    override fun get(pName: String) = data[pName]

    override fun <T : Any> get(proxyKey: ProxyKey<T>, op: () -> T): T =
        additionalParameters[proxyKey]?.let { proxyKey.valueKlass.safeCast(it) }
            ?: op().also { additionalParameters[proxyKey] = it }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        // Try fast
        val mName = method.name
        indexedGetters[mName]?.let { return it() }
        indexedSetters[mName]?.let { it(args?.get(0)); return Unit }
        if (unrecognizedMethods.contains(mName)) return null

        // Extract
        val prefix = method.name.substring(0..2).toLowerCase()
        val name = method.name.substring(3).decapitalize()

        // Check
        if (name.isBlank() || prefix.length < 3) {
            unrecognizedMethods += mName
            return Unit
        }

        // Check default
        var mutHandle: MethodHandle? = null
        if (method.isDefault) {
            mutHandle =
                LookupUtils.getBindHandle(method, proxy)
        }
        val handle = mutHandle

        // Index
        bindProxy = proxy
        indexedMethods[mName] = method
        methodsReverseIndex[method] = mName
        when {
            // TODO add getters and setters type checks.
            prefix == "get" && handle != null -> indexedGetters[mName] = { handle.invokeWithArguments() }
            prefix == "set" && handle != null -> indexedSetters[mName] = { handle.invokeWithArguments(it) }
            prefix == "get" -> indexedGetters[mName] = { data[name] }
            prefix == "set" -> indexedSetters[mName] = { data[name] = it }
            else -> unrecognizedMethods += mName
        }

        // Index property name
        when (prefix) {
            "get" -> pNameToGetter[name] = mName
            "set" -> pNameToSetter[name] = mName
        }

        // All indexed, so recall
        return invoke(proxy, method, args)
    }

}