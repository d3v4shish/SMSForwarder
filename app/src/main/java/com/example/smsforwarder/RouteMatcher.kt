package com.example.smsforwarder

object RouteMatcher {
    fun firstMatch(routes: List<Route>, sender: String, body: String): Route? {
        return routes.firstOrNull { route ->
            matches(route.matchMode, route.senderRule, sender) &&
                matches(route.matchMode, route.messageRule, body)
        }
    }

    private fun matches(mode: RouteMatchMode, condition: String, value: String): Boolean {
        if (condition.isBlank()) return true
        return when (mode) {
            RouteMatchMode.CONTAINS -> value.contains(condition, ignoreCase = true)
            RouteMatchMode.REGEX -> RouteRegex.matches(condition, value)
        }
    }
}
