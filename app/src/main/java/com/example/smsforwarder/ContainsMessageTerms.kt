package com.example.smsforwarder

object ContainsMessageTerms {
    fun validationError(expression: String): String? {
        if (expression.isBlank()) return null
        return if (parse(expression) == null) "Message terms cannot be empty." else null
    }

    fun matches(expression: String, value: String): Boolean {
        if (expression.isBlank()) return true
        return parse(expression)?.any { requiredTerms ->
            requiredTerms.all { term -> value.contains(term, ignoreCase = true) }
        } == true
    }

    private fun parse(expression: String): List<List<String>>? {
        val alternatives = expression.split(',').map { group ->
            group.split('+').map(String::trim)
        }
        if (alternatives.any { requiredTerms -> requiredTerms.any(String::isEmpty) }) return null
        return alternatives
    }
}
