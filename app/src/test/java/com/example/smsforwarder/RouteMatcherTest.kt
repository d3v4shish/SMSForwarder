package com.example.smsforwarder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `sender and message literal conditions must both match when populated`() {
        val route = Route(1, "Bank", "OTP", "+15550000001")

        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your balance changed"))
        assertNull(RouteMatcher.firstMatch(listOf(route), "SHOP", "Your OTP is 123456"))
        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "bank-alert", "your otp is 123456")?.id)
    }

    @Test
    fun `contains mode treats regular expression characters as literal text`() {
        val route = Route(1, "[BANK]", "OTP+", "+15550000001")

        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your OTP is 123456"))
        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "[bank]-alert", "Your OTP+ is 123456")?.id)
    }

    @Test
    fun `advanced regex conditions must both match when populated`() {
        val route = Route(
            id = 1,
            senderRule = "^Bank-[A-Z]+$",
            messageRule = "\\b(?:OTP|PIN)\\b",
            destination = "+15550000001",
            matchMode = RouteMatchMode.REGEX,
        )

        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your balance changed"))
        assertNull(RouteMatcher.firstMatch(listOf(route), "SHOP", "Your OTP is 123456"))
        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Use NOTP confirmation instead"))
        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "bank-alert", "your otp is 123456")?.id)
    }

    @Test
    fun `a blank sender condition is a wildcard`() {
        val route = Route(1, "", "receipt", "+15550000001")

        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "+15551234567", "Your RECEIPT is ready")?.id)
    }

    @Test
    fun `invalid regular expressions never match and are reported to the editor`() {
        val route = Route(1, "[unclosed", "", "+15550000001", RouteMatchMode.REGEX)

        assertNotNull(RouteRegex.validationError(route.senderRule))
        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your OTP is 123456"))
    }
}
