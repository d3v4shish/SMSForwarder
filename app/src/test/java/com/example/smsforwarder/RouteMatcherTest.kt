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
        val route = Route(1, "[BANK]", "OTP", "+15550000001")

        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your OTP is 123456"))
        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "[bank]-alert", "Your OTP is 123456")?.id)
    }

    @Test
    fun `message comma terms match any literal alternative`() {
        val route = Route(1, "bank", "otp,pin", "+15550000001")

        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your OTP is 123456")?.id)
        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your PIN is 123456")?.id)
        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your code is 123456"))
    }

    @Test
    fun `message plus terms require every literal term`() {
        val route = Route(1, "bank", "payment+received", "+15550000001")

        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Payment received")?.id)
        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Payment pending"))
        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Receipt received"))
    }

    @Test
    fun `message plus terms take precedence over comma terms`() {
        val route = Route(1, "bank", "otp+urgent,invoice", "+15550000001")

        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Urgent OTP needed")?.id)
        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Invoice ready")?.id)
        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "OTP needed"))
    }

    @Test
    fun `empty message terms are rejected and do not match`() {
        assertNotNull(ContainsMessageTerms.validationError("otp,,pin"))
        assertNotNull(ContainsMessageTerms.validationError("otp+"))
        assertNull(ContainsMessageTerms.validationError("otp + urgent, invoice"))
        assertNull(RouteMatcher.firstMatch(listOf(Route(1, "bank", "otp+", "+15550000001")), "bank", "otp"))
    }

    @Test
    fun `legacy message rules retain literal comma and plus matching`() {
        val route = Route(
            id = 1,
            senderRule = "bank",
            messageRule = "OTP+PIN",
            destination = "+15550000001",
            containsMessageSyntax = ContainsMessageSyntax.LEGACY_LITERAL,
        )

        assertEquals(1L, RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your OTP+PIN is 123456")?.id)
        assertNull(RouteMatcher.firstMatch(listOf(route), "BANK-ALERT", "Your OTP PIN is 123456"))
        assertEquals(ContainsMessageSyntax.LEGACY_LITERAL, ContainsMessageSyntax.fromStorage(null))
        assertEquals(ContainsMessageSyntax.TERM_EXPRESSION, ContainsMessageSyntax.fromStorage("terms"))
    }

    @Test
    fun `contains sender matches a contact name after raw sender misses`() {
        val route = Route(1, "Alice", "OTP", "+15550000001")

        assertEquals(
            1L,
            RouteMatcher.firstMatch(listOf(route), "+15551234567", "Your OTP is 123456") { "Alice Example" }?.id,
        )
        assertNull(RouteMatcher.firstMatch(listOf(route), "+15551234567", "Your OTP is 123456"))
    }

    @Test
    fun `contact name lookup is skipped when raw sender or message already decides`() {
        var calls = 0
        val rawSenderRoute = Route(1, "1555", "OTP", "+15550000001")
        assertEquals(
            1L,
            RouteMatcher.firstMatch(listOf(rawSenderRoute), "+15551234567", "Your OTP is 123456") {
                calls += 1
                "Unused"
            }?.id,
        )
        val unmatchedMessageRoute = Route(2, "Alice", "receipt", "+15550000001")
        assertNull(
            RouteMatcher.firstMatch(listOf(unmatchedMessageRoute), "+15551234567", "Your OTP is 123456") {
                calls += 1
                "Unused"
            },
        )
        assertEquals(0, calls)
    }

    @Test
    fun `contact name is resolved once across failed sender rules`() {
        val routes = listOf(
            Route(1, "Alice", "OTP", "+15550000001"),
            Route(2, "Bob", "OTP", "+15550000002"),
        )
        var calls = 0

        assertNull(
            RouteMatcher.firstMatch(routes, "+15551234567", "Your OTP is 123456") {
                calls += 1
                "Carol"
            },
        )
        assertEquals(1, calls)
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
