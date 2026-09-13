package com.example.smsforwarder

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

object RouteRegex {
    private const val MAX_EXPRESSION_LENGTH = 256
    private const val FLAGS = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    private const val CACHE_LIMIT = 256
    private val compiledPatterns = linkedMapOf<String, Pattern>()

    fun validationError(expression: String): String? {
        if (expression.isBlank()) return null
        if (expression.length > MAX_EXPRESSION_LENGTH) {
            return "Regular expressions are limited to $MAX_EXPRESSION_LENGTH characters."
        }
        return try {
            Pattern.compile(expression, FLAGS)
            null
        } catch (_: PatternSyntaxException) {
            "Invalid regular expression."
        }
    }

    fun matches(expression: String, value: String): Boolean {
        if (expression.isBlank()) return true
        return compiled(expression)?.matcher(value)?.find() == true
    }

    private fun compiled(expression: String): Pattern? {
        if (expression.length > MAX_EXPRESSION_LENGTH) return null
        synchronized(compiledPatterns) {
            compiledPatterns[expression]?.let { return it }
        }
        val pattern = try {
            Pattern.compile(expression, FLAGS)
        } catch (_: PatternSyntaxException) {
            return null
        }
        synchronized(compiledPatterns) {
            if (compiledPatterns.size >= CACHE_LIMIT) compiledPatterns.clear()
            return compiledPatterns.getOrPut(expression) { pattern }
        }
    }
}
