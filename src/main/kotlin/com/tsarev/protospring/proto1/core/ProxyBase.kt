package com.tsarev.protospring.proto1.core

import java.lang.reflect.Proxy

interface ProxyBase {

    val isDetached get() = this
        .takeIf { Proxy.isProxyClass(it.javaClass) }
        ?.let { Proxy.getInvocationHandler(it) }
        ?.let { it as? CommonProxy }
        ?.nonHibernate ?: true

}