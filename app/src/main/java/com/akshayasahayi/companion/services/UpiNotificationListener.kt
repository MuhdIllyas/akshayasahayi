package com.akshayasahayi.companion.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.akshayasahayi.companion.utils.Outbox
import org.json.JSONObject

/**
 * Reads "payment received" notifications from UPI apps (GPay, Paytm, PhonePe,
 * BHIM SBI Pay / YONO) so payments to their QR codes can be shown in Sahayi.
 *
 * Only the apps in UPI_PACKAGES are read; every other app's notifications
 * (WhatsApp, messages…) are ignored here and never leave the phone.
 * The server decides which notifications are payments.
 *
 * Needs "Notification access" turned on for Akshaya Companion
 * (the dashboard card opens that setting).
 */
class UpiNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "UpiListener"

        // Keep in step with UPI_APP_PACKAGES in server/utils/upiCredit.js
        val UPI_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.merchant", // Google Pay for Business
            "com.google.android.apps.nbu.paisa.user",     // Google Pay
            "com.paytm.business",                         // Paytm for Business
            "net.one97.paytm",                            // Paytm
            "com.phonepe.app",                            // PhonePe
            "com.phonepe.app.business",                   // PhonePe Business
            "com.sbi.upi",                                // BHIM SBI Pay
            "com.sbi.lotusintouch",                       // YONO SBI
        )

        // Apps re-post (update) the same notification; remember what was sent for this long.
        private const val REPEAT_WINDOW_MS = 10 * 60 * 1000L
    }

    private val recent = LinkedHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName !in UPI_PACKAGES) return
        val n = sbn.notification ?: return

        // Group summaries ("3 new payments") and ongoing ones are not payments.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // Cheap filter: only notifications that mention money go to the server.
        val all = "$title $text"
        if (!Regex("(₹|rs\\.?|inr)\\s*[0-9]", RegexOption.IGNORE_CASE).containsMatchIn(all)) return

        // Two ₹1 payments from the same person have the same text, so text alone
        // can't tell them apart. An update of one notification keeps its key and
        // its "when" time; a new payment gets a new one.
        val eventTime = if (n.`when` > 0) n.`when` else sbn.postTime
        val now = System.currentTimeMillis()
        val key = "${sbn.packageName}|${sbn.key}|$eventTime|$title|$text"
        synchronized(recent) {
            recent.entries.removeAll { now - it.value > REPEAT_WINDOW_MS }
            if (recent.containsKey(key)) return
            recent[key] = now
        }

        Log.d(TAG, "💰 ${sbn.packageName}: $title — $text")

        val payload = JSONObject().apply {
            put("package", sbn.packageName)
            put("title", title)
            put("text", text)
            put("postedAt", eventTime)
            put("clientRef", Outbox.refFor("notif", sbn.packageName, sbn.key, eventTime, title, text))
        }
        Outbox.enqueue(applicationContext, "incoming_notification", payload)
    }
}
