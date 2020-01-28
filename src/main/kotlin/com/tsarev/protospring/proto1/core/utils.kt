package com.tsarev.protospring.proto1.core

import java.lang.reflect.Proxy
import kotlin.reflect.KProperty0

fun <T : Any> T.hasValue(op: T.() -> KProperty0<*>): Boolean {
    val proxy = this.takeIf { Proxy.isProxyClass(it.javaClass) }
        ?.let { Proxy.getInvocationHandler(it) }
        ?.let { it as? CommonProxy }
    return proxy?.data?.containsKey(this.op().name) ?: false
}