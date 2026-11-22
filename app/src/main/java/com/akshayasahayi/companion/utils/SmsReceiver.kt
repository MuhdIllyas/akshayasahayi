package com.akshayasahayi.companion.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsGateway"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        // A long SMS (bank credit alerts often are) arrives in several parts.
        // Join the parts from the same sender back into one message.
        val bySender = linkedMapOf<String, StringBuilder>()
        val timeBySender = mutableMapOf<String, Long>()
        for (part in parts) {
            val sender = part.displayOriginatingAddress ?: "Unknown"
            bySender.getOrPut(sender) { StringBuilder() }.append(part.displayMessageBody ?: "")
            timeBySender.putIfAbsent(sender, part.timestampMillis)
        }

        for ((sender, bodyBuilder) in bySender) {
            val body = bodyBuilder.toString()
            val timestamp = timeBySender[sender] ?: System.currentTimeMillis()

            Log.d(TAG, "📩 SMS from $sender (${body.length} chars)")

            // Hint only; the server decides (it also knows bank credit SMS).
            val isOtpPattern = body.contains("OTP", true) ||
                    body.contains("code", true) ||
                    body.contains("verification", true) ||
                    Regex("\\b\\d{4,6}\\b").containsMatchIn(body)

            val payload = JSONObject().apply {
                put("sender", sender)
                put("message", body)
                put("timestamp", timestamp)
                put("isOtp", isOtpPattern)
                put("clientRef", Outbox.refFor("sms", sender, timestamp, body))
            }

            // Saved first, then sent; kept until the server confirms it.
            Outbox.enqueue(context, "incoming_sms", payload)
        }
    }
}
