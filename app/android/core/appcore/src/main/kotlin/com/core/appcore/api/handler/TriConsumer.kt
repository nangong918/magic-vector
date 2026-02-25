package com.core.appcore.api.handler

fun interface TriConsumer<T, U, V> {
    fun accept(t: T?, u: U?, v: V?)
}