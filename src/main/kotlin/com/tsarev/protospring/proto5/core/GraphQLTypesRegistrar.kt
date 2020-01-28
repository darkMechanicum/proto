package com.tsarev.protospring.proto5.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.tsarev.protospring.proto5.api.GInput
import com.tsarev.protospring.proto5.api.GInterface
import com.tsarev.protospring.proto5.api.GType
import graphql.GraphQL
import graphql.Scalars
import graphql.language.*
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.TypeDefinitionRegistry
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.data.repository.config.GraphQLAnnotatedWiringRegistrar
import java.lang.NullPointerException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

/**
 * Registering all [javax.persistence.Entity] types to
 * GraphQLSchema.
 */
@Configuration
open class GraphQLTypesRegistrar : BeanFactoryAware {

    @Autowired
    lateinit var wiringRegistrar: GraphQLAnnotatedWiringRegistrar

    @Autowired
    lateinit var myBeanFactory: BeanFactory

    @Autowired
    lateinit var mapper: ObjectMapper

    override fun setBeanFactory(beanFactory: BeanFactory) = run { myBeanFactory = beanFactory }

    /**
     * Create [GraphQL].
     */
    @Bean
    open fun graphQL(schema: GraphQLSchema): GraphQL =
        GraphQL.newGraphQL(schema).build()

    /**
     * Create [GraphQLSchema].
     */
    @Bean
    open fun graphQLSchema(registry: TypeDefinitionRegistry, wiring: RuntimeWiring): GraphQLSchema =
        SchemaGenerator().makeExecutableSchema(registry, wiring)

    /**
     * Build [RuntimeWiring] from repository methods.
     */
    @Bean
    open fun runtimeWiring(): RuntimeWiring {
        val builder = RuntimeWiring.newRuntimeWiring()
        wiringRegistrar.registeredQueryMethods.forEach { method ->
            builder.type("Query") { it.dataFetcher(method.name, buildFetcher(method)) }
        }
        wiringRegistrar.registeredQueryMethods.forEach { method ->
            builder.type("Mutation") { it.dataFetcher(method.name, buildFetcher(method)) }
        }
        return builder.build()
    }

    /**
     * Create simple data fetcher from method.
     */
    private fun buildFetcher(method: Method) = object : DataFetcher<Any> {
        val wrappedMethod = method.apply { isAccessible = true }
        val repository = myBeanFactory.getBean(wrappedMethod.declaringClass)
        override fun get(environment: DataFetchingEnvironment) = with(environment) {
            val parameterNames = wrappedMethod.parameters.map { it.name }
            val parameterValues = parameterNames.map { getArgument<Any>(it) }.mapIndexed { index, value ->
                if (Map::class.java.isAssignableFrom(value.javaClass)) {
                    try {
                        mapper.convertValue(value, wrappedMethod.parameters[index].type)
                    } catch (ex: Throwable) {
                        value
                    }
                } else {
                    value
                }
            }
            return@with wrappedMethod.invoke(repository, *parameterValues.toTypedArray())
        }
    }

    /**
     * Create [TypeDefinitionRegistry] used to build [GraphQLSchema].
     */
    @Bean
    open fun graphQlTypeDefinitionRegistry() = TypeDefinitionRegistry().apply {
        getGQLEntities().forEach { entityType ->
            val nullableDefinition = defineObjDefinition(entityType)
            nullableDefinition?.let { add(it) }
        }
        addQueryOrMutation("Query", wiringRegistrar.registeredQueryMethods)
        addQueryOrMutation("Mutation", wiringRegistrar.registeredMutationMethods)
    }

    /**
     * Try to search annotation mapped graphql entities via classpath.
     */
    private fun getGQLEntities(): List<Class<*>> {
        val provider = InterfaceBasedScanner().apply {
            addIncludeFilter(AnnotationTypeFilter(GType::class.java))
            addIncludeFilter(AnnotationTypeFilter(GInterface::class.java))
        }
        val candidates = provider.findCandidateComponents("com.tsarev.protospring.proto5")
        return candidates.map { it.beanClassName }.map { Class.forName(it) }
    }

    /**
     * Add Query or Mutation type.
     */
    private fun TypeDefinitionRegistry.addQueryOrMutation(typeName: String, methods: List<Method>) {
        val builder = ObjectTypeDefinition.newObjectTypeDefinition()
            .name(typeName)
        methods.forEach {
            builder.fieldDefinition(defineTypeField(it))
        }
        add(builder.build())
    }

    /**
     * Generate type field description.
     */
    private fun defineTypeField(method: Method) = FieldDefinition.newFieldDefinition()
        .name(method.name)
        .type(resolveType(method.genericReturnType))
        .inputValueDefinitions(toFieldDefinitions(method))
        .build()

    /**
     * Convert method parameters to definitions.
     */
    private fun toFieldDefinitions(method: Method) = run {
        val parameterNames = method.parameters.map { it.name }
        val parameterTypes = method.parameterTypes.map { resolveType(it) }
        parameterNames.mapIndexed { index, name ->
            InputValueDefinition(name, parameterTypes[index])
        }
    }

    /**
     * Generate [ObjectTypeDefinition] from entity class.
     */
    private fun <T : Any> defineObjDefinition(type: Class<T>): TypeDefinition<*>? {
        if (!type.isInterface) return null
        val klass = type.kotlin
        if (type.isAnnotationPresent(GInput::class.java)) {
            val builder = InputObjectTypeDefinition.newInputObjectDefinition()
                .name(type.simpleName)
            klass.memberProperties.forEach {
                val fieldDef = InputValueDefinition(it.name, resolveType(it.returnType.javaType))
                builder.inputValueDefinition(fieldDef)
            }
            return builder.build()
        } else {
            val builder = ObjectTypeDefinition.newObjectTypeDefinition()
                .name(type.simpleName)
            klass.memberProperties.forEach {
                val fieldDef = FieldDefinition(it.name, resolveType(it.returnType.javaType))
                builder.fieldDefinition(fieldDef)
            }
            return builder.build()
        }
    }

    private fun resolveType(clazz: java.lang.reflect.Type?): Type<*> =
        when (clazz) {
            null -> throw NullPointerException()
            Long::class.javaPrimitiveType, Long::class.java -> TypeName(Scalars.GraphQLInt.name)
            String::class.java -> TypeName(Scalars.GraphQLString.name)
            is ParameterizedType -> ListType(resolveType(clazz.actualTypeArguments.first()))
            is Class<*> -> TypeName(clazz.simpleName)
            else -> throw RuntimeException()
        }

}

/**
 * [ClassPathScanningCandidateComponentProvider] inheritor that looks up only for interfaces.
 */
class InterfaceBasedScanner : ClassPathScanningCandidateComponentProvider(false) {
    override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
        val metadata = beanDefinition.metadata
        return metadata.isInterface
    }
}