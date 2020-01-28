package org.springframework.data.repository.config

import com.tsarev.protospring.proto5.api.GMutation
import com.tsarev.protospring.proto5.api.GQuery
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.Configuration
import java.lang.reflect.Method
import javax.annotation.PostConstruct

/**
 * Register all custom annotated queries and mutations.
 */
@Configuration
open class GraphQLAnnotatedWiringRegistrar : BeanFactoryAware {

    lateinit var bdr: BeanDefinitionRegistry

    val registeredQueryMethods: MutableList<Method> = mutableListOf()

    val registeredMutationMethods: MutableList<Method> = mutableListOf()

    override fun setBeanFactory(beanFactory: BeanFactory) {
        bdr = beanFactory as BeanDefinitionRegistry
    }

    @PostConstruct
    fun postProcess() {
        val candidateDefinitions = RepositoryComponentProvider(emptyList(), bdr)
            .findCandidateComponents("com.tsarev.protospring.proto5")
            .map { it.beanClassName }
            .map { Class.forName(it) }
        candidateDefinitions.forEach { clazz ->
            registeredQueryMethods += clazz.declaredMethods.filter { it.isAnnotationPresent(GQuery::class.java) }
            registeredMutationMethods += clazz.declaredMethods.filter { it.isAnnotationPresent(GMutation::class.java) }
        }
    }

}