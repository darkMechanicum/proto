package com.tsarev.protospring.proto5.api

/**
 * Annotation to mark method as GraphQl query.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GQuery

/**
 * Annotation to mark method as GraphQl mutation.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GMutation

/**
 * Annotation to mark interface as GraphQl type.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GType

/**
 * Annotation to mark GQL type as input type.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GInput

/**
 * Annotation to mark interface as GraphQl interface.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GInterface