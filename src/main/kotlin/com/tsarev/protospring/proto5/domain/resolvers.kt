package com.tsarev.protospring.proto5.domain

import com.tsarev.protospring.proto5.api.GMutation
import com.tsarev.protospring.proto5.api.GQuery
import com.tsarev.protospring.proto5.api.ctxFindAll
import com.tsarev.protospring.proto5.api.rawSpec
import com.tsarev.protospring.proto5.domain.schema.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

object Specs {
    fun userSpec(filter: UserFilter?) =
        rawSpec<User, UserFilter>(filter) {
            and(
                User::id isIn it.ids,
                User::name isLike it.namePattern
            )
        }

    fun docSpec(filter: IdDocumentFilter?) =
        rawSpec<IdDocument, IdDocumentFilter>(filter) {
            and(
                IdDocument::id isIn it.ids,
                IdDocument::identification isLike it.identificationPattern
            )
        }
}

interface CarRepository : JpaRepository<Car, Long> {

    @GQuery
    @JvmDefault
    fun fetchCar(id: Long): Car? = findById(id).orElse(null)

//    @GMutation
//    @JvmDefault
//    fun saveCar(debt: Car): Car? = save(debt)

}

interface UserRepository : JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

//    @GQuery
//    @JvmDefault
//    fun fetchUser(rawUser: User?): User? = rawUser?.let { fetchUser(rawUser.id) }

//    @GMutation
//    @JvmDefault
//    fun saveUser(doc: User): User = save(doc)

    @GQuery
    @JvmDefault
    fun fetchUser(id: Long): User? = findById(id).orElse(null)

    @JvmDefault
    fun searchUser(filter: UserFilter, pageable: Pageable): Page<User> = ctxFindAll(pageable) {
        and(
            Specs.userSpec(filter).fromRoot,
            Specs.docSpec(filter.docs).fromJoin("doc")
        )
    }
}

interface IdDocRepository : JpaRepository<IdDocument, Long>, JpaSpecificationExecutor<IdDocument> {

    @GQuery
    @JvmDefault
    fun searchDoc(filter: IdDocumentFilter): List<IdDocument> = ctxFindAll { Specs.docSpec(filter).fromRoot }

//    @GMutation
//    @JvmDefault
//    fun saveDoc(doc: IdDocument): IdDocument = save(doc)
}