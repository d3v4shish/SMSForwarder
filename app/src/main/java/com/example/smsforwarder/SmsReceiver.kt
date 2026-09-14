package com.example.smsforwarder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import java.util.concurrent.Executors

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        forwardingExecutor.execute {
            try {
                if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    return@execute
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) return@execute

                val sender = messages.first().originatingAddress.orEmpty()
                val body = messages.joinToString(separator = "") { message -> message.messageBody.orEmpty() }
                val appContext = context.applicationContext
                val senderName by lazy { ContactNameResolver.forSender(appContext, sender) }
                val route = RouteMatcher.firstMatch(
                    RouteStore(appContext).load(),
                    sender,
                    body,
                    senderNameProvider = { senderName },
                )
                    ?: return@execute

                SmsForwarder.forward(appContext, route.destination, body)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val forwardingExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "sms-forwarder").apply { isDaemon = true }
        }
    }
}
