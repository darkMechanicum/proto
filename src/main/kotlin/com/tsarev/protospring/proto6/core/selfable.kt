package com.tsarev.protospring.proto6.core

interface SelfAble<T : SelfAble<T>> {
    val self get() = this as T
}