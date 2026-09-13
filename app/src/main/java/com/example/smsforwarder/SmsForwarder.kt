package com.example.smsforwarder

import android.content.Context
import android.telephony.SmsManager

object SmsForwarder {
    fun forward(context: Context, destination: String, body: String) {
        if (body.isEmpty()) return

        val smsManager = context.getSystemService(SmsManager::class.java) ?: return
        val parts = smsManager.divideMessage(body)
        if (parts.size == 1) {
            smsManager.sendTextMessage(destination, null, parts.first(), null, null)
        } else {
            smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
        }
    }
}
