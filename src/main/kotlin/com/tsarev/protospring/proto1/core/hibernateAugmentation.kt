package com.tsarev.protospring.proto1.core

import org.hibernate.annotations.common.reflection.java.InterfaceBasedReflectionManager
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.internal.MetadataBuilderImpl
import org.hibernate.boot.spi.BootstrapContext
import org.hibernate.boot.spi.MetadataBuilderFactory
import org.hibernate.boot.spi.MetadataBuilderImplementor

class XClassReplaceMetadataBuilderFactory : MetadataBuilderFactory {

    override fun getMetadataBuilder(
        metadatasources: MetadataSources?,
        defaultBuilder: MetadataBuilderImplementor?
    ) = object : MetadataBuilderImpl(metadatasources) {
        override fun getBootstrapContext(): BootstrapContext {
            val delegateBootstrapCtx: BootstrapContext = super.getBootstrapContext()
            return object : BootstrapContext by super.getBootstrapContext() {
                override fun getReflectionManager() =
                    InterfaceBasedReflectionManager(
                        delegateBootstrapCtx.reflectionManager
                    )
            }
        }
    }

}