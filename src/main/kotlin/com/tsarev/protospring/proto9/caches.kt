package com.tsarev.protospring.proto9

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.Cache
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.DispatcherServlet
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

/**
 * Configuration bean for creating caches.
 */
@Configuration
open class CachesFactory : ApplicationContextAware {

    lateinit var appCtx: ApplicationContext
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        appCtx = applicationContext
    }

    @Bean
    open fun testCache(): Cache {
        val bean = appCtx.getBean("requestTestCache", Cache::class.java)
        return object : Cache by bean {
            override fun getName() = "testCache"
        }
    }

    @Bean(autowireCandidate = false)
    @Cacheable("requestTestCacheCache", keyGenerator = "txKeyGenerator")
//    @Cacheable("requestTestCacheCache")
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
    open fun requestTestCache(): Cache = CaffeineCache(
        "",
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1)
            .build()
    )

    @Bean
    open fun requestTestCacheCache(): Cache = CaffeineCache(
        "requestTestCacheCache",
        Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.SECONDS)
            .maximumSize(10)
            .build()
    )

    @Bean
    open fun txKeyGenerator() = KeyGenerator { _, _, _ ->
        try {
            (RequestContextHolder.currentRequestAttributes() as? ServletRequestAttributes)
                ?.request
                ?.getHeader("txid")
                ?: "<deafult>"
        } catch (ignore: Throwable) {
            "<deafult>"
        }
    }

}