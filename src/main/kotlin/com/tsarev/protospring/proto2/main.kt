package com.tsarev.protospring.proto2

import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.builders.ResponseMessageBuilder
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.OperationBuilderPlugin
import springfox.documentation.spi.service.contexts.OperationContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.common.SwaggerPluginSupport
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import kotlin.reflect.KClass

fun main(vararg args: String) = SpringApplication.run(ExampleConf::class.java, *args).unit

// --- Custom request handler annotation ---

annotation class MyPath(val value: String)

private fun MyPath.toRestMethod() = value
    .let { it.substring(0..2) to it.substring(4) }
    .let { (methodStr, path) ->
        when (methodStr) {
            "GET" -> RequestMappingInfo.paths(path).methods(RequestMethod.GET).build()
            else -> throw IllegalArgumentException()
        }
    }

// --- Custom swagger annotation ---

@ApiResponses(
    value = [
        ApiResponse(code = 500, message = "response 500"),
        ApiResponse(code = 400, message = "response 400")
    ]
)
annotation class CommonResponses

// --- Custom swagger annotations mini plugin ---

inline fun <reified T : Annotation> customAnnotationPlugin() = object : OperationBuilderPlugin {
    override fun apply(context: OperationContext) = context.apply {
        operationBuilder().responseMessages(responsesFrom(T::class))
    }.unit

    override fun supports(delimiter: DocumentationType?) = SwaggerPluginSupport.pluginDoesApply(delimiter)
}

fun <T : Annotation> OperationContext.responsesFrom(annotationClass: KClass<T>) = annotationClass.java
        .mergedAnnotations(ApiResponses::class)
        .flatMap { it.value.toList() }
        .distinctBy { it.code }
        .map { ResponseMessageBuilder().code(it.code).message(it.message).build() }
        .toSet()

// --- Example application ---

@RestController
@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class ExampleConf : WebMvcRegistrations {

    @Bean
    open fun api(): Docket = Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(PathSelectors.any())
        .build()

    @Bean
    open fun commonResponsesPlugin() = customAnnotationPlugin<CommonResponses>()

    override fun getRequestMappingHandlerMapping() = object : RequestMappingHandlerMapping() {
        override fun getMappingForMethod(method: Method, handlerType: Class<*>) = createRequestMappingInfo(method)
        private fun createRequestMappingInfo(element: AnnotatedElement) =
            element.mergedAnnotation(MyPath::class)?.toRestMethod()
    }

    @ResponseBody
    @CommonResponses
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "200")
        ]
    )
    @MyPath("GET /example")
    fun example() = "example"

}

// --- Utils ---

val Any?.unit: Unit get() = Unit

fun <T : Annotation> AnnotatedElement.mergedAnnotation(klass: KClass<T>): T? =
    AnnotatedElementUtils.findMergedAnnotation(this, klass.java)

fun <T : Annotation> AnnotatedElement.mergedAnnotations(klass: KClass<T>): Set<T> =
    AnnotatedElementUtils.findAllMergedAnnotations(this, klass.java)