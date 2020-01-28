package com.tsarev.protospring.proto1.core

import org.hibernate.EntityNameResolver
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.mapping.PersistentClass
import org.hibernate.tuple.entity.EntityMetamodel
import org.hibernate.tuple.entity.EntityTuplizer
import org.hibernate.tuple.entity.PojoEntityTuplizer
import java.io.Serializable
import java.lang.reflect.Proxy

open class ProxyBasedPojoTuplizer(
    entityMetamodel: EntityMetamodel,
    mappedEntity: PersistentClass
) : ProxyBasedTuplizer(PojoEntityTuplizer(entityMetamodel, mappedEntity), entityMetamodel)

open class EntityNameResolver : EntityNameResolver {
    override fun resolveEntityName(entity: Any) =
        entity.takeIf { Proxy.isProxyClass(it.javaClass) }
            ?.let { Proxy.getInvocationHandler(it) }
            ?.let { it as? CommonProxy }
            ?.handledClass
            ?.name
}

open class ProxyBasedTuplizer(
    private val delegate: EntityTuplizer,
    private val entityMeta: EntityMetamodel
) : EntityTuplizer by delegate {

    private val resolver = arrayOf(EntityNameResolver())

    override fun getEntityNameResolvers() = resolver

    override fun getPropertyValues(entity: Any): Array<Any?> =
        entity.takeIf { Proxy.isProxyClass(it.javaClass) }
            ?.let { Proxy.getInvocationHandler(it) }
            ?.let { it as? CommonProxy }
            ?.takeIf { it.nonHibernate }
            ?.run {
                val span = entityMeta.propertySpan
                val result = arrayOfNulls<Any>(span)
                for (j in 0 until span) {
                    val property = entityMeta.properties[j]
                    // Here should be contains key, not null compairing!
                    if (data.containsKey(property.name)) {
                        result[j] = data[property.name]
                    } else {
                        result[j] = LazyPropertyInitializer.UNFETCHED_PROPERTY
                    }
                }
                return result
            }
            ?: delegate.getPropertyValues(entity)

    override fun instantiate(id: Serializable?, session: SharedSessionContractImplementor?): Any =
        if (this.mappedClass.isInterface) {
            Proxy.newProxyInstance(
                this::class.java.classLoader,
                arrayOf(this.mappedClass),
                CommonProxy(this.mappedClass)
            ).also { if (id != null) setIdentifier(it, id, session) }
        } else {
            delegate.instantiate(id, session)
        }
}