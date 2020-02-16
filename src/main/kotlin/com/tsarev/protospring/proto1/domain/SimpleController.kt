package com.tsarev.protospring.proto1.domain

import com.tsarev.protospring.proto1.core.ProxyBase
import com.tsarev.protospring.proto1.core.ProxyBasedPojoTuplizer
import org.hibernate.annotations.Tuplizer
import org.springframework.data.repository.CrudRepository
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.persistence.*

@MappedSuperclass
@Tuplizer(impl = ProxyBasedPojoTuplizer::class)
interface IdAble : ProxyBase {

    @get:Id @get:Column(name = "ID")
    var id: Long

}

@Entity @Table(name = "TEST")
@Tuplizer(impl = ProxyBasedPojoTuplizer::class)
interface CarUpdate : IdAble {

    @get:Id @get:Column(name = "ID")
    override var id: Long

    @get:Column(name = "AMOUNT")
    var amount: Long

}

@Entity @Table(name = "TEST")
@Tuplizer(impl = ProxyBasedPojoTuplizer::class)
interface Car : CarUpdate {

    @get:Id @get:Column(name = "ID")
    override var id: Long

    @get:Column(name = "MODEL")
    var model: String

    @get:Column(name = "ENGINE")
    var engine: String

    @get:Column(name = "AMOUNT")
    override var amount: Long

}

@RestController
interface SimpleController : CrudRepository<Car, Long> {

    @JvmDefault
    @GetMapping("/cars/{id}")
    fun getCar(@PathVariable id: Long): Optional<Car> = findById(id)

    @JvmDefault
    @PutMapping("/cars")
    fun update(@RequestBody car: CarUpdate?): Car? =
        car.takeIf { it?.isDetached ?: false }?.let { save(it) }
            ?.let { car?.id?.let { findById(it).orElse(null) } }
            ?: car?.id?.let { findById(it).orElse(null) }

    @JvmDefault
    @PostMapping("/cars")
    fun saveCar(@RequestBody car: Car?): Car? = save(car)

    fun save(entity: CarUpdate)
}