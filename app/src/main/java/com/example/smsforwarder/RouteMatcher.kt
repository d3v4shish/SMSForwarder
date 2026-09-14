package com.example.smsforwarder

object RouteMatcher {
    fun firstMatch(
        routes: List<Route>,
        sender: String,
        body: String,
        senderNameProvider: (() -> String?)? = null,
    ): Route? {
        var senderName: String? = null
        var senderNameResolved = false
        return routes.firstOrNull { route ->
            if (!matchesMessage(route, body)) {
                false
            } else if (route.senderRule.isBlank()) {
                true
            } else if (route.matchMode == RouteMatchMode.REGEX) {
                RouteRegex.matches(route.senderRule, sender)
            } else if (sender.contains(route.senderRule, ignoreCase = true)) {
                true
            } else {
                if (!senderNameResolved) {
                    senderName = senderNameProvider?.invoke()
                    senderNameResolved = true
                }
                senderName?.contains(route.senderRule, ignoreCase = true) == true
            }
        }
    }

    private fun matchesMessage(route: Route, body: String): Boolean {
        if (route.messageRule.isBlank()) return true
        return when (route.matchMode) {
            RouteMatchMode.REGEX -> RouteRegex.matches(route.messageRule, body)
            RouteMatchMode.CONTAINS -> when (route.containsMessageSyntax) {
                ContainsMessageSyntax.LEGACY_LITERAL -> body.contains(route.messageRule, ignoreCase = true)
                ContainsMessageSyntax.TERM_EXPRESSION -> ContainsMessageTerms.matches(route.messageRule, body)
            }
        }
    }
}
