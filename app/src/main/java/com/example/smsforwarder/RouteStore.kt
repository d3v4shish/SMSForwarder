package com.example.smsforwarder

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class RouteStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<Route> {
        val savedRoutes = preferences.getString(ROUTES_KEY, "[]") ?: "[]"
        return try {
            val routes = JSONArray(savedRoutes)
            buildList {
                for (index in 0 until routes.length()) {
                    val route = routes.optJSONObject(index) ?: continue
                    val destination = route.optString(DESTINATION_KEY).trim()
                    if (destination.isNotEmpty()) {
                        add(
                            Route(
                                id = route.optLong(ID_KEY),
                                senderRule = senderRule(route),
                                messageRule = messageRule(route),
                                destination = destination,
                                matchMode = matchMode(route),
                            ),
                        )
                    }
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun save(routes: List<Route>) {
        val serialized = JSONArray()
        routes.forEach { route ->
            serialized.put(
                JSONObject()
                    .put(ID_KEY, route.id)
                    .put(SENDER_RULE_KEY, route.senderRule.trim())
                    .put(MESSAGE_RULE_KEY, route.messageRule.trim())
                    .put(DESTINATION_KEY, route.destination.trim())
                    .put(MATCH_MODE_KEY, route.matchMode.storageValue),
            )
        }
        preferences.edit().putString(ROUTES_KEY, serialized.toString()).apply()
    }

    private fun senderRule(route: JSONObject): String = rule(route, SENDER_RULE_KEY, SENDER_REGEX_KEY, LEGACY_SENDER_KEY)

    private fun messageRule(route: JSONObject): String = rule(route, MESSAGE_RULE_KEY, MESSAGE_REGEX_KEY, LEGACY_MESSAGE_KEY)

    private fun rule(route: JSONObject, currentKey: String, regexKey: String, legacyKey: String): String {
        return when {
            route.has(currentKey) -> route.optString(currentKey).trim()
            route.has(regexKey) -> route.optString(regexKey).trim()
            else -> route.optString(legacyKey).trim()
        }
    }

    private fun matchMode(route: JSONObject): RouteMatchMode {
        RouteMatchMode.fromStorage(route.optString(MATCH_MODE_KEY))?.let { return it }
        return if (route.has(SENDER_REGEX_KEY) || route.has(MESSAGE_REGEX_KEY)) {
            RouteMatchMode.REGEX
        } else {
            RouteMatchMode.CONTAINS
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "forwarding_routes"
        private const val ROUTES_KEY = "routes"
        private const val ID_KEY = "id"
        private const val SENDER_RULE_KEY = "senderRule"
        private const val MESSAGE_RULE_KEY = "messageRule"
        private const val MATCH_MODE_KEY = "matchMode"
        private const val SENDER_REGEX_KEY = "senderRegex"
        private const val MESSAGE_REGEX_KEY = "messageRegex"
        private const val LEGACY_SENDER_KEY = "senderContains"
        private const val LEGACY_MESSAGE_KEY = "messageContains"
        private const val DESTINATION_KEY = "destination"
    }
}
