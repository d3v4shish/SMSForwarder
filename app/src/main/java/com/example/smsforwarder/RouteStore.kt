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
                                senderContains = route.optString(SENDER_KEY).trim(),
                                messageContains = route.optString(MESSAGE_KEY).trim(),
                                destination = destination,
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
                    .put(SENDER_KEY, route.senderContains.trim())
                    .put(MESSAGE_KEY, route.messageContains.trim())
                    .put(DESTINATION_KEY, route.destination.trim()),
            )
        }
        preferences.edit().putString(ROUTES_KEY, serialized.toString()).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "forwarding_routes"
        private const val ROUTES_KEY = "routes"
        private const val ID_KEY = "id"
        private const val SENDER_KEY = "senderContains"
        private const val MESSAGE_KEY = "messageContains"
        private const val DESTINATION_KEY = "destination"
    }
}
