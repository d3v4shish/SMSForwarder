package com.example.smsforwarder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteMatcherTest {
    @Test
    fun `first matching route wins in stored order`() {
        val routes = listOf(
            Route(1, "bank", "", "+15550000001"),
            Route(2, "", "otp", "+15550000002"),
        )

        val match = RouteMatcher.firstMatch(routes, "BANK-ALERT", "Your OTP is 123456")

        assertEquals(1L, match?.id)
    }

    @Test
    fun `sender and message conditions must both match when populated`() {
        val route = Route(1, "Bank", "OTP", "+15550000001")

        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your balance changed"))
        assertNull(RouteMatcher.firstMatch(listOf(route), "SHOP", "Your OTP is 123456"))
        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "bank-alert", "your otp is 123456")?.id)
    }

    @Test
    fun `a blank sender condition is a wildcard`() {
        val route = Route(1, "", "receipt", "+15550000001")

        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "+15551234567", "Your RECEIPT is ready")?.id)
    }
}
