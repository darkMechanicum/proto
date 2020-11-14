package com.tsarev.protospring.proto9

import com.fasterxml.jackson.databind.ObjectMapper
import feign.jackson.JacksonDecoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.cloud.openfeign.FeignClientBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseBody

class TestData {
    var some: String? = null
}

@ResponseBody
interface TestFeign {

    @GetMapping("/test1")
    fun test1(
        @RequestHeader("txid", defaultValue = "1") txid: String
    ): TestData

    @Cacheable("testCache", key = "(#root.methodName)")
    @GetMapping("/test2")
    fun test2(
        @RequestHeader("txid", defaultValue = "1") txid: String
    ): TestData

}

@Configuration
open class FeignInit : ApplicationContextAware {

    private lateinit var appCtx: ApplicationContext
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        appCtx = applicationContext;
    }

    @Bean
    open fun jacksonDecoder(
        @Autowired mapper: ObjectMapper
    ) = JacksonDecoder(mapper)

    @Bean("testApiFeign")
    open fun testApiFeign(): TestFeign = FeignClientBuilder(appCtx)
        .forType(TestFeign::class.java, "testFeignApi")
        .url("localhost:8080")
        .build<TestFeign>() as TestFeign
}