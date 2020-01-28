package com.tsarev.protospring.proto1.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.tsarev.protospring.proto1.core.ProxyBasedPojoTuplizer
import org.hibernate.annotations.Generated
import org.hibernate.annotations.Tuplizer
import org.springframework.data.repository.CrudRepository
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "TEST")
@Tuplizer(impl = ProxyBasedPojoTuplizer::class)
interface Ent {

    @get:Id
    @get:Column(name = "ID")
    var id: Long

    @get:Column(name = "DATA")
    var data: String

    @get:JsonProperty("some")
    @get:Column(name = "DATA2")
    var data2: String

}

@RestController
interface SimpleController : CrudRepository<Ent, Long> {

    @GetMapping("/tmp/{id}")
    override fun findById(@PathVariable id: Long): Optional<Ent>

    @PostMapping("/tmp")
    override fun <S : Ent?> save(@RequestBody entity: S): S

}