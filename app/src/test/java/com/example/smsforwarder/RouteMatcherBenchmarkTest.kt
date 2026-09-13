package com.example.smsforwarder

import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteMatcherBenchmarkTest {
    @Test
    fun fixedRouteSelectionWorkload() {
        val routes = List(100) { index ->
            if (index == 75) {
                Route(
                    id = index.toLong(),
                    senderRule = "^service-[a-z]+$",
                    messageRule = "\\bimportant\\b",
                    destination = "+15550000075",
                    matchMode = RouteMatchMode.REGEX,
                )
            } else {
                Route(index.toLong(), "unmatched-$index", "", "+15550000000")
            }
        }
        var matchedIdTotal = 0L
        val elapsedMillis = measureTimeMillis {
            repeat(100_000) {
                matchedIdTotal += checkNotNull(
                    RouteMatcher.firstMatch(routes, "SERVICE-ALERT", "An IMPORTANT update"),
                ).id
            }
        }

        assertEquals(7_500_000L, matchedIdTotal)
        println("RouteMatcher fixed workload: $elapsedMillis ms")
    }
}
