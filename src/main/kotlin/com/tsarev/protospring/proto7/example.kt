package com.tsarev.protospring.proto7

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

class SecretClass(
    private var a: String
)

fun main() = Thief.doMain()

object Thief {
    fun doMain() {
        val secret = SecretClass("5")
        var a: String by iNeedIt(secret)
        println(a)
    }
}

class INeedItDelegateProvider<T : Any, R : Any>(
    private val tKlass: KClass<T>,
    private val pKlass: KClass<R>,
    private val instance: T
) {
    companion object {
        val lookup = MethodHandles.lookup()!!
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): INeedItDelegate<T, R> {
        tKlass.java.getDeclaredField(property.name).isAccessible = true
        return INeedItDelegate(
            instance,
            getter = lookup.findGetter(tKlass.java, property.name, pKlass.java),
            setter = lookup.findSetter(tKlass.java, property.name, pKlass.java)
        )
    }
}

inline fun <reified T : Any, reified R : Any> iNeedIt(instance: T) =
    INeedItDelegateProvider<T, R>(T::class, R::class, instance)

class INeedItDelegate<T : Any, R>(
    private val instance: T,
    private val getter: MethodHandle,
    private val setter: MethodHandle
) :
    ReadWriteProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R = getter.invoke(instance) as R
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) =
        setter.invoke(instance, value)?.run {} ?: Unit
}