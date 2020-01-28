package com.tsarev.protospring.proto5

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.config.GraphQLAnnotatedWiringRegistrar

fun main(vararg args: String) {
    SpringApplication.run(MainConfig::class.java, *args)
}

@Import(GraphQLAnnotatedWiringRegistrar::class)
@EnableJpaRepositories
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class MainConfig