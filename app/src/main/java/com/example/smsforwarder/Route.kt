package com.example.smsforwarder

data class Route(
    val id: Long,
    val senderRule: String,
    val messageRule: String,
    val destination: String,
    val matchMode: RouteMatchMode = RouteMatchMode.CONTAINS,
    val containsMessageSyntax: ContainsMessageSyntax = ContainsMessageSyntax.TERM_EXPRESSION,
)

enum class RouteMatchMode(val storageValue: String) {
    CONTAINS("contains"),
    REGEX("regex"),
    ;

    companion object {
        fun fromStorage(value: String): RouteMatchMode? = entries.firstOrNull { it.storageValue == value }
    }
}

enum class ContainsMessageSyntax(val storageValue: String) {
    LEGACY_LITERAL("literal"),
    TERM_EXPRESSION("terms"),
    ;

    companion object {
        fun fromStorage(value: String?): ContainsMessageSyntax {
            return entries.firstOrNull { it.storageValue == value } ?: LEGACY_LITERAL
        }
    }
}
