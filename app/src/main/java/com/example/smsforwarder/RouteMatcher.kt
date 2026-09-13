package com.example.smsforwarder

import java.util.Locale

object RouteMatcher {
    fun firstMatch(routes: List<Route>, sender: String, body: String): Route? {
        val normalizedSender = sender.lowercase(Locale.ROOT)
        val normalizedBody = body.lowercase(Locale.ROOT)

        return routes.firstOrNull { route ->
            matches(route.senderContains, normalizedSender) &&
                matches(route.messageContains, normalizedBody)
        }
    }

    private fun matches(condition: String, value: String): Boolean {
        return condition.isBlank() || value.contains(condition.lowercase(Locale.ROOT))
    }
}
