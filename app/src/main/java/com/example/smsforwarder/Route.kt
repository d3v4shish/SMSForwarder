package com.example.smsforwarder

data class Route(
    val id: Long,
    val senderRule: String,
    val messageRule: String,
    val destination: String,
    val matchMode: RouteMatchMode = RouteMatchMode.CONTAINS,
)

enum class RouteMatchMode(val storageValue: String) {
    CONTAINS("contains"),
    REGEX("regex"),
    ;

    companion object {
        fun fromStorage(value: String): RouteMatchMode? = entries.firstOrNull { it.storageValue == value }
    }
}
