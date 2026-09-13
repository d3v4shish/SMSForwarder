package com.example.smsforwarder

data class Route(
    val id: Long,
    val senderContains: String,
    val messageContains: String,
    val destination: String,
)
