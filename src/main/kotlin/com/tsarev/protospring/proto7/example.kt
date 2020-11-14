package com.tsarev.protospring.proto7

import java.lang.invoke.*
import java.util.function.Function
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class SecretClass(
    var a: String
) {
    fun pavlik() = a
}

fun main() = Thief.doMain()

object Thief {
    fun doMain() {
        val secret = SecretClass("5")
        var a: String? by iNeedIt(secret)
        val field = secret.javaClass.getDeclaredField("a").apply { isAccessible = true }
        val bindGetter = MethodHandles.lookup()!!
            .unreflectGetter(field)
            .bindTo(secret)
            .asType(MethodType.methodType(Object::class.java))
        val varHandle = MethodHandles.privateLookupIn(
            SecretClass::class.java, MethodHandles.lookup()!!
        )!!.unreflectVarHandle(field)
        measure("Direct access") { secret.pavlik() }
        measure("Method handle access") { TrickyGetter.doTest(bindGetter) as? String }
        measure("Var handle access") { TrickyGetter.doTest(varHandle, secret) as? String }
        measure("Reflection access") { field.get(secret) as String? }
        measure("Var handle and delegate access") { a }
    }

    fun measure(prefix: String, lambda: () -> String?) {
        val start = System.nanoTime()
        for (i in 1..100000000) {
            lambda()
        }
        val end = System.nanoTime()
        println("$prefix took ${end - start}ns.")
        println("$prefix took ${(end - start) / 1000000}ns for operation.")
        println()
    }
}

class INeedItDelegateProvider<T : Any, R : Any>(
    private val instance: T,
    private val pKlass: KClass<R>
) {
    companion object {
        val lookup = MethodHandles.privateLookupIn(
            SecretClass::class.java, MethodHandles.lookup()!!
        )!!
    }

    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): INeedItDelegate<T, R> {
        val tClass = instance::class
        val stolenField = tClass.java.getDeclaredField(property.name)
        stolenField.isAccessible = true
        return INeedItDelegate<T, R>(
            tClass = instance.javaClass,
            pClass = pKlass.java,
            instance = instance,
            varHandle = lookup.unreflectVarHandle(stolenField),
            getterHandle = lookup.findVirtual(
                SecretClass::class.java,
                "getA",
                MethodType.methodType(String::class.java)
            )
        ).also {
            stolenField.isAccessible = false
        }
    }
}

inline fun <reified T : Any, reified R : Any> iNeedIt(
    instance: T
) = INeedItDelegateProvider(instance, R::class)

class INeedItDelegate<T : Any, R : Any>(
    private val tClass: Class<T>,
    private val pClass: Class<R>,
    private val instance: T,
    private val varHandle: VarHandle,
    private val getterHandle: MethodHandle
) : ReadWriteProperty<Any?, R?> {

    val getter = TrickyGetter.doTest2<T, R>(
        LambdaMetafactory.metafactory(
            MethodHandles.privateLookupIn(SecretClass::class.java, MethodHandles.lookup()),
            "apply",
            MethodType.methodType(Function::class.java),
            MethodType.methodType(Object::class.java, Object::class.java),
            getterHandle,
            MethodType.methodType(pClass, tClass)
        ).target
    )
//    val bindGetter = varHandle.bindTo(instance)
//            .asType(MethodType.methodType(Object::class.java))
//    val bindSetter = setter.bindTo(instance)

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        getter.apply(instance)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R?) =
        varHandle.set(instance, value).run {}
}