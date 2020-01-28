package com.tsarev.protospring.proto5.api

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import javax.persistence.criteria.*
import kotlin.reflect.KProperty

data class SpecCtx<T, F, Z>(
    val from: From<T, F>,
    val criteria: CriteriaQuery<Z>,
    private val cb: CriteriaBuilder
) : CriteriaBuilder by cb, From<T, F> by from, CriteriaQuery<Z> by criteria {

    val ((From<T, *>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate).fromRoot
        get() = this(this@SpecCtx.from, this@SpecCtx.criteria, this@SpecCtx.cb)

    fun <X> ((From<X, *>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate).fromJoin(attributeName: String) =
        this(from.join<X, T>(attributeName), this@SpecCtx.criteria, this@SpecCtx.cb)

    fun <R> get(prop: KProperty<R>): Expression<R> = this.get<R>(prop.name)

    infix fun  KProperty<String?>.isLike(pattern: String?): Predicate = pattern?.let { this@SpecCtx.like(get(this), it) } ?: aTrue

    infix fun <L> KProperty<L?>.isIn(values: Collection<L>?): Predicate = values?.let { get(this).`in`(it) } ?: aTrue

    val aTrue: Predicate get() = cb.and()
}

fun <T> rawSpec(op: SpecCtx<T, *, *>.() -> Predicate): (From<T, *>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate =
    { from, cq, cb -> op(SpecCtx(from, cq, cb)) }

fun <T, F> rawSpec(
    arg: F?,
    op: SpecCtx<T, *, *>.(F) -> Predicate
): (From<T, *>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate =
    { from, cq, cb -> arg?.let { op(SpecCtx(from, cq, cb), it) } ?: cb.and() }

fun <T> JpaSpecificationExecutor<T>.ctxFindAll(op: SpecCtx<T, *, *>.() -> Predicate) =
    this.findAll(rawSpec { op() })

fun <T> JpaSpecificationExecutor<T>.ctxFindAll(pageable: Pageable, op: SpecCtx<T, *, *>.() -> Predicate) =
    this.findAll(rawSpec { op() }, pageable)

fun <T> JpaSpecificationExecutor<T>.ctxFindAll(sort: Sort, op: SpecCtx<T, *, *>.() -> Predicate) =
    this.findAll(rawSpec { op() }, sort)