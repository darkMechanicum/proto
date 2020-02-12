package com.tsarev.protospring.proto6

import java.lang.RuntimeException
import java.lang.StringBuilder
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Class with common validation logic.
 */
open class ValidatedBase<T, Outer : ValidatedBase<T, Outer>> {

    val outerThis get() = this as Outer

    val validatorChain: MutableList<(T) -> String?> = mutableListOf()

    val mergedValidator: (T) -> String?
        get() = { data ->
            val collectedErrors = validatorChain.mapNotNull { it(data) }
            collectedErrors.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")
        }

    /**
     * Validate with static message.
     */
    fun validate(desc: String, validator: (T) -> Boolean) =
        validatorChain.add { data -> desc.takeIf { validator(data) } }
            .let { outerThis }

    /**
     * Validate with generated message.
     */
    fun validate(validator: (T) -> Boolean) = MessageChain(validator)

    /**
     * Class for convenient validation message format.
     */
    inner class MessageChain(private val validator: (T) -> Boolean) {
        fun withMessage(message: (T) -> String) =
            validatorChain.add { data -> if (validator(data)) null else message(data) }
                .let { outerThis }
    }
}

/**
 * Delegate that is able to fill meta and add additional serializing logic.
 */
class NestedSerializeDelegate<T : SerializeAble>(
    private val properties: MutableList<NestedSerializeProp<out SerializeAble>> = mutableListOf(),
    private val ctor: () -> T
) : ValidatedBase<T, NestedSerializeDelegate<T>>() {
    operator fun provideDelegate(thisRef: SerializeAble, prop: KProperty<*>): NestedSerializeProp<T> {
        return NestedSerializeProp(prop.name, ctor, mergedValidator)
            .also { properties.add(it) }
    }
}

/**
 * Serializable class property.
 */
class NestedSerializeProp<T : SerializeAble>(
    val name: String,
    val ctor: () -> T,
    val validator: (T) -> String?
) : ReadWriteProperty<Any, T> {
    var value: T? = null
    override fun getValue(thisRef: Any, property: KProperty<*>) = value!!
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun bindNew() = ctor().also { value = it }
    fun validate() = value?.let { validator(it) }
}

/**
 * Delegate that is able to fill meta and add additional serializing logic.
 */
class PrimitiveSerializeDelegate<T>(
    private val properties: MutableList<PrimitiveSerializeProp<*>> = mutableListOf(),
    private val parser: (String) -> T
) : ValidatedBase<T, PrimitiveSerializeDelegate<T>>() {
    operator fun provideDelegate(thisRef: SerializeAble, prop: KProperty<*>) =
        PrimitiveSerializeProp(prop.name, parser, mergedValidator).also { properties.add(it) }
}

/**
 * Serializable class property.
 */
class PrimitiveSerializeProp<T>(
    val name: String,
    val parser: (String) -> T,
    val validator: (T) -> String?
) : ReadWriteProperty<Any, T> {
    var value: T? = null
    override fun getValue(thisRef: Any, property: KProperty<*>) = value!!
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun parseValue(value: String) = apply { this.value = parser(value) }
    fun validate() = value?.let { validator(it) }
}

/**
 * Class for serializing base.
 */
open class SerializeAble {
    val primitives: MutableList<PrimitiveSerializeProp<*>> = mutableListOf()
    val nested: MutableList<NestedSerializeProp<out SerializeAble>> = mutableListOf()

    fun long() = PrimitiveSerializeDelegate(primitives) { it.toLong() }
    fun string() = PrimitiveSerializeDelegate(primitives) { it }
    fun <T : SerializeAble> nested(ctor: () -> T) = NestedSerializeDelegate(nested, ctor)
}

/**
 * Serialize object to string.
 */
object StringSerializer {

    /**
     * Do serialize.
     */
    fun <T : SerializeAble> serialize(data: T): String {
        val sb = StringBuilder()
        sb.append("{ ")
        doSerialize(data, sb)
        sb.append(" }")
        return sb.toString()
    }

    /**
     * Perform serialization logic.
     */
    private fun <T : SerializeAble> doSerialize(data: T, sb: StringBuilder) {
        val primitivesSize = data.primitives.size
        data.primitives.forEachIndexed { index, it ->
            sb.append("\"${it.name}\": ")
            if (it.value == null) sb.append("null") else sb.append("\"${it.value}\"")
            if (index != primitivesSize - 1) sb.append(", ")
        }
        val nestedSize = data.nested.size
        if (primitivesSize != 0 && nestedSize != 0) sb.append(", ")
        data.nested.forEachIndexed { index, it ->
            sb.append("\"${it.name}\": ")
            if (it.value == null) {
                sb.append("null")
            } else {
                sb.append("{ ")
                it.value?.let { nestedData -> doSerialize(nestedData, sb) }
                sb.append(" }")
            }
            if (index != nestedSize - 1) sb.append(", ")
        }
    }
}

/**
 * Serialize object to string.
 */
class StringDeserializer<T : SerializeAble>(
    private val seed: T,
    private val raw: String,
    private val sharedIndex: SharedIndex = SharedIndex()
) {
    private var index
        get() = sharedIndex.index
        set(value) {
            sharedIndex.index = value
        }

    private val current get() = raw[sharedIndex.index]

    /**
     * Small class to share string position.
     */
    data class SharedIndex(var index: Int = 0)

    /**
     * Read object from property.
     */
    private fun readObject(name: String) {
        val nestedProperty = seed.nested.firstOrNull { it.name == name }
            ?: throw RuntimeException("Unknown property $name at $index")
        val nestedObject = nestedProperty.bindNew()
        StringDeserializer(nestedObject, raw, sharedIndex).readCurrentObject()
        nestedProperty.validate()?.let { throw RuntimeException(it) }
    }

    /**
     * Fetch all current object properties.
     */
    public fun readCurrentObject() {
        requireChar('{')
        var wasInLoop = false
        outer@ while (readProperty()) {
            wasInLoop = true
            when (fetchChar()) {
                ',' -> {
                    index++; continue@outer
                }
                '}' -> {
                    index++; break@outer
                }
                else -> throw RuntimeException("Unexpected character at $index.")
            }
        }
        if (!wasInLoop) requireChar('}')
    }

    /**
     * Read property logic.
     */
    private fun readProperty(): Boolean {
        val name = readLiteral() ?: return false
        requireChar(':')
        val value = readLiteral()
        if (value != null)
            attachPrimitive(name, value)
        else
            readObject(name)
        return true
    }

    /**
     * Attach property to current seed.
     */
    private fun attachPrimitive(name: String, value: String) {
        seed.primitives.firstOrNull { it.name == name }
            ?.parseValue(value)
            ?.also { prop -> prop.validate()?.let { throw RuntimeException(it) } }
            ?: throw RuntimeException("Unknown property $name at $index")
    }

    /**
     * Read " wrapped literal.
     */
    private fun readLiteral(): String? {
        skipSpaces()
        return when (current) {
            '\"' -> {
                index++; readLiteralToEnd()
            }
            else -> null
        }
    }

    /**
     * Search for literal " end.
     */
    private fun readLiteralToEnd(): String {
        var literal = ""
        while (current != '\"') {
            literal += current
            index++
        }
        index++
        return literal
    }

    /**
     * Get next non space character.
     */
    private fun fetchChar() = skipSpaces().let { current }

    /**
     * Skip specified character and spaces and throw if no character found.
     */
    private fun requireChar(required: Char) {
        skipSpaces()
        if (current == required) {
            index++
            return
        } else throw RuntimeException("Colon expected at $index")
    }

    /**
     * Skip spaces and newlines.
     */
    private fun skipSpaces() {
        while (current == ' ' || current == '\n' || current == '\t') index++
    }
}