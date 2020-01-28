package com.tsarev.protospring.proto5.core.proxy

import org.hibernate.EntityNameResolver
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.mapping.PersistentClass
import org.hibernate.tuple.entity.EntityMetamodel
import org.hibernate.tuple.entity.EntityTuplizer
import org.hibernate.tuple.entity.PojoEntityTuplizer
import java.io.Serializable

val propertyNamesKey = ProxyKey.create<Array<String>>()

val propertySpanKey = ProxyKey.create<Int>()

open class ProxyBasedPojoTuplizer(
    entityMetamodel: EntityMetamodel,
    mappedEntity: PersistentClass
) : ProxyBasedTuplizer(PojoEntityTuplizer(entityMetamodel, mappedEntity), entityMetamodel)

open class EntityNameResolver : EntityNameResolver {
    override fun resolveEntityName(entity: Any) =
        HibernateJDPFactory.asProxy(entity)?.targetInterface?.qualifiedName
}

open class ProxyBasedTuplizer(
    private val delegate: EntityTuplizer,
    private val entityMeta: EntityMetamodel
) : EntityTuplizer by delegate {

    private val resolver = arrayOf(EntityNameResolver())

    override fun getEntityNameResolvers() = resolver

    override fun getPropertyValues(entity: Any): Array<Any?> =
        HibernateJDPFactory.asProxy(entity)
            ?.takeIf { !it.isManaged }
            ?.run {
                val result = arrayOfNulls<Any>(propertySpan)
                propertyNames.forEachIndexed { index, pName ->
                    // Here should be contains key, not `data[property.name] != null`, cause value can be null.
                    if (data.containsKey(pName)) {
                        result[index] = data[pName]
                    } else {
                        result[index] = LazyPropertyInitializer.UNFETCHED_PROPERTY
                    }
                }
                return result
            } ?: delegate.getPropertyValues(entity)

    /**
     * Get cached property names.
     */
    private val EntityProxy.propertyNames
        get() = get(propertyNamesKey) { entityMeta.properties.map { it.name }.toTypedArray() }

    /**
     * Get cached property span.
     */
    private val EntityProxy.propertySpan
        get() = get(propertySpanKey) { entityMeta.propertySpan }

    override fun instantiate(id: Serializable?, session: SharedSessionContractImplementor?): Any =
        if (this.mappedClass.isInterface) {
            HibernateJDPFactory.createProxy(this.mappedClass.kotlin, true)
                .also { if (id != null) setIdentifier(it, id, session) }
        } else {
            delegate.instantiate(id, session)
        }
}