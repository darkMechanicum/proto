//package com.tsarev.protospring.proto1.configuration
//
//import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations
//import org.springframework.context.annotation.Configuration
//import org.springframework.core.annotation.AnnotatedElementUtils
//import org.springframework.web.bind.annotation.RequestMethod
//import org.springframework.web.servlet.mvc.method.RequestMappingInfo
//import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
//import java.lang.reflect.AnnotatedElement
//import java.lang.reflect.Method
//
//open class WebMvcCustomRegistration : WebMvcRegistrations {
//
//    override fun getRequestMappingHandlerMapping(): RequestMappingHandlerMapping {
//        return object : RequestMappingHandlerMapping() {
//
//            override fun getMappingForMethod(method: Method, handlerType: Class<*>) = createRequestMappingInfo(method)
//
//            private fun createRequestMappingInfo(element: AnnotatedElement) =
//                AnnotatedElementUtils
//                    .findMergedAnnotation(element, MyPath::class.java)
//                    ?.let { extractHandlerInfo(it) }
//
//            private fun extractHandlerInfo(annotation: MyPath) = annotation.value
//                .let { it.substring(0..2) to it.substring(4) }
//                .let { RequestMappingInfo.paths(it.second).methods(it.first.toRestMethod()).build() }
//        }
//    }
//
//}
//
//private fun String.toRestMethod() = when(this) {
//    "GET" -> RequestMethod.GET
//    else -> throw UnsupportedOperationException()
//}