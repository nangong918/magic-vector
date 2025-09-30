package com.core.appcore.api.handler

fun interface FourConsumer<T, U, V, X> {
    fun accept(t: T?, u: U?, v: V?, x: X?)
}