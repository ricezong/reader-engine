package io.legado.app.data.entities

data class Cache(
    val key: String,
    val value: String,
    val deadline: Long
)
