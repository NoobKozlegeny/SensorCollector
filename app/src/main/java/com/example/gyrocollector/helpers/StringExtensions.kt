package com.example.gyrocollector.helpers

fun String.intOrString(): Any {
    val v = toIntOrNull()
    return when(v) {
        null -> this
        else -> v
    }
}