package org.hibernate.annotations.common.reflection.java

import org.hibernate.annotations.common.reflection.ReflectionManager
import org.hibernate.annotations.common.reflection.XClass

class InterfaceBasedReflectionManager(
    private val delegate: ReflectionManager
) : ReflectionManager by delegate {

    override fun <T : Any?> toXClass(clazz: Class<T>?): XClass? =
        delegate.toXClass(clazz)?.let { InterfaceAwareJavaXClass(it) }

    override fun <T : Any?> classForName(name: String?, caller: Class<T>?): XClass? =
        delegate.classForName(name, caller)?.let { InterfaceAwareJavaXClass(it) }

    override fun classForName(name: String?): XClass? =
        delegate.classForName(name)?.let { InterfaceAwareJavaXClass(it) }
}

internal class InterfaceAwareJavaXClass(
    private val delegate: XClass
) : JavaXClass(null, null, null), XClass by delegate { // JavaXClass inheritance for type checks in ReflectionManager.
    override fun getSuperclass(): XClass {
        val naiveSuperClass = super.getSuperclass()
        return naiveSuperClass ?: super.getInterfaces().first()
    }
}