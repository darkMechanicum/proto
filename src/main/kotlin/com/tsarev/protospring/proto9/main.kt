package com.tsarev.protospring.proto9

import graphql.spring.web.servlet.GraphQLEndpointConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

fun main(vararg args: String) {
    SpringApplication.run(Main::class.java, *args)
}

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        GraphQLEndpointConfiguration::class,
        HibernateJpaAutoConfiguration::class
    ]
)
@EnableCaching
open class Main