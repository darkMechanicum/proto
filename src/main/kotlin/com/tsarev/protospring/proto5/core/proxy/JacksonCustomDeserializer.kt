package com.tsarev.protospring.proto5.core.proxy

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.ValueInstantiator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.module.SimpleValueInstantiators
import com.tsarev.protospring.proto1.core.CommonProxy
import org.springframework.context.annotation.Configuration
import java.lang.reflect.Proxy

object InterfaceAwareInstantiators : SimpleValueInstantiators() {
    override fun findValueInstantiator(
        config: DeserializationConfig?,
        beanDesc: BeanDescription?,
        defaultInstantiator: ValueInstantiator?
    ): ValueInstantiator? = if (beanDesc?.beanClass?.isInterface == true)
        InterfaceAwareInstantiator(beanDesc.beanClass)
    else
        defaultInstantiator

}

class InterfaceAwareInstantiator(
    val type: Class<*>
) : ValueInstantiator.Base(type) {
    override fun canInstantiate() = true
    override fun canCreateUsingDefault() = true
    override fun createUsingDefault(ctxt: DeserializationContext?) = Proxy.newProxyInstance(
        this::class.java.classLoader,
        arrayOf(_valueType),
        CommonProxy(type, true)
    )
}

@Configuration
open class JacksonConfig(
    private val mapper: ObjectMapper
) {

    init {
        mapper.registerModule(SimpleModule().apply { setValueInstantiators(InterfaceAwareInstantiators) })
    }

}