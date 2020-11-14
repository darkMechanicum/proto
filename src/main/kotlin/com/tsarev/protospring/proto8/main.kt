package com.tsarev.protospring.proto8

import graphql.spring.web.servlet.GraphQLEndpointConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

fun main(vararg args: String) {
    SpringApplication.run(MainEntryPoint::class.java, *args)
}

class SimpleData {
    lateinit var first: String
    lateinit var second: String
}

@RestController
@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        GraphQLEndpointConfiguration::class
    ]
)
open class MainEntryPoint {

    @PostMapping("test/")
    fun test(
        @RequestPart("file") file: MultipartFile,
        @RequestPart("body") data: SimpleData
    ) {
        println("json is: $data")
        println("file name is: ${file.originalFilename}")
        println("file size is: ${file.size}")
    }

}