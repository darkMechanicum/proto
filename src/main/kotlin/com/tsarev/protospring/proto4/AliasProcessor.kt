package com.tsarev.protospring.proto4

import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import kotlin.collections.HashSet

/**
 * Alias metadata.
 */
interface IAlias {
    val alias: Class<Annotation>
    val dependents: Set<Class<Annotation>>
    val mapping: Map<String, String>
}

/**
 * Simple implementation.
 */
data class Alias(
    override val alias: Class<Annotation>,
    override val dependents: Set<Class<Annotation>>,
    override val mapping: Map<String, String>
) : IAlias

/**
 * Provider to get metadata.
 */
interface AliasProvider {
    val aliases: Set<IAlias>
}

/**
 * Extending alias capabilities.
 */
class ExtendedAlias(
    private val nested: IAlias,
    private val env: ProcessingEnvironment
) : IAlias by nested {
    val aliasName by lazy { alias.name!!.let { env.elementUtils.getName(it) } }
}

/**
 * Grouping alias metadata provider.
 */
class AliasRegistry(
    private val nested: Collection<AliasProvider>,
    private val env: ProcessingEnvironment
) {
    /**
     * Collected aliases.
     */
    private val aliases by lazy { nested.flatMap { it.aliases }.map { ExtendedAlias(it, env) }.toSet() }

    /**
     * Aliases map by name.
     */
    val aliasMap: Map<Name, ExtendedAlias> by lazy {
        aliases.map { it.alias.name!!.let { env.elementUtils.getName(it) } to it }.toMap()
    }

    /**
     * Alias names as strings view.
     */
    val aliasStringNames by lazy { aliases.map { it.alias.name!! }.toSet() }

    /**
     * Alias names view.
     */
    val aliasNames by lazy { aliasMap.keys }

}

/**
 * Alias processing logic.
 */
class AliasProcessor : AbstractProcessor() {

    private lateinit var registry: AliasRegistry

    override fun init(processingEnv: ProcessingEnvironment) = super.init(processingEnv).also {
        val providersIter = ServiceLoader.load(AliasProvider::class.java).iterator()
        val providers = HashSet<AliasProvider>().apply { providersIter.forEach { add(it) } }
        registry = AliasRegistry(providers, processingEnv)
    }

    override fun getSupportedAnnotationTypes() = registry.aliasStringNames

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment) = with<RoundContext, Boolean>(
        RoundContext(roundEnv, processingEnv, registry)
    ) {
        // No supported aliases found.
        if (annotations.none { registry.aliasNames.contains(it.qualifiedName) }) return false
        annotations.forEach { processAnnotation(it) }
        return true
    }

}

/**
 * Single processing round context.
 */
 data class RoundContext(
    private val roundEnv: RoundEnvironment,
    private val env: ProcessingEnvironment,
    private val registry: AliasRegistry
) {

    /**
     * Process single alias.
     */
    fun processAnnotation(annotation: TypeElement) {
        val alias = registry.aliasMap.getValue(annotation.qualifiedName)
        val annotated = roundEnv.getElementsAnnotatedWith(annotation)
        annotated.forEach { if (checkAlias(alias, it)) replaceAlias(alias, it) }
    }

    /**
     * Check alias for coherence.
     */
    private fun checkAlias(alias: ExtendedAlias, annotated: Element): Boolean {
        TODO("checking magic")
    }

    /**
     * Replace alias annotation with its dependents.
     */
    private fun replaceAlias(alias: IAlias, annotated: Element) {
        TODO("processing magic")
    }

}