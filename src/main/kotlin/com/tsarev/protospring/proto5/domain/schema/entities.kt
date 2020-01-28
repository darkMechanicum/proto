package com.tsarev.protospring.proto5.domain.schema

import com.tsarev.protospring.proto5.api.GInput
import com.tsarev.protospring.proto5.api.GType
import com.tsarev.protospring.proto5.core.proxy.ProxyBasedPojoTuplizer
import org.hibernate.annotations.Tuplizer
import javax.persistence.*

@GType
@Entity
@Tuplizer(impl = ProxyBasedPojoTuplizer::class)
@Table(name = "CARS")
interface Car {

    @get:Id
    var id: Long

    @get:ManyToOne
    @get:JoinColumn(name = "USER_ID")
    var user: User?

    @get:Column(name = "IDENTIFICATION")
    var identification: String?

    @get:Column(name = "NOTE")
    var note: String?

    @get:Column(name = "PRICE")
    var price: Long?
}

@GType
@Entity
@Tuplizer(impl = ProxyBasedPojoTuplizer::class)
@Table(name = "USERS")
interface User {

    @get:Id
    var id: Long

    @get:Column(name = "NAME")
    var name: String?

    @get:Column(name = "SURNAME")
    var surname: String?

    @JvmDefault
    @get:Transient
    val fullName: String?
        get() = "$name $surname"

    @JvmDefault
    @get:Transient
    val some: List<String?>?
        get() = listOf(name)

    @get:ManyToOne
    @get:JoinColumn(name = "DOC_ID")
    var doc: IdDocument
}

@GType
@GInput
interface UserFilter {

    var namePattern: String?

    var ids: List<Long>

    var docs: IdDocumentFilter
}

@GType
@Entity
@Tuplizer(impl = ProxyBasedPojoTuplizer::class)
@Table(name = "DOCS")
interface IdDocument {

    @get:Id
    var id: Long

    @get:Column(name = "IDENTIFICATION")
    var identification: String

}

@GType
@GInput
interface IdDocumentFilter {

    var identificationPattern: String?

    var ids: List<Long>
}