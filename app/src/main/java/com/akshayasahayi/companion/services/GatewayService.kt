package com.akshayasahayi.companion.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.akshayasahayi.companion.api.SocketHandler
import com.akshayasahayi.companion.storage.PreferencesManager
import com.akshayasahayi.companion.utils.Outbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GatewayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // This is the persistent notification that keeps the app alive
        val notification = NotificationCompat.Builder(this, "GATEWAY_CHANNEL")
            .setContentTitle("Akshaya Gateway Active")
            .setContentText("Forwarding OTPs and UPI payments to Sahayi.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's icon later
            .setOngoing(true)
            .build()

        // Tell Android this is a Foreground Service (with type for Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        Outbox.init(this)

        // Keep the server connection from the service, not only while the
        // dashboard screen is open, and resend anything waiting every minute.
        if (loop?.isActive != true) {
            loop = scope.launch {
                val prefs = PreferencesManager(applicationContext)
                while (isActive) {
                    val token = prefs.jwtTokenFlow.firstOrNull()
                    if (token != null) {
                        SocketHandler.establishConnection(token)
                        Outbox.flush()
                    }
                    delay(60_000)
                }
            }
        }

        // START_STICKY tells Android to restart this service immediately if it ever crashes
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't need to bind to this service
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "GATEWAY_CHANNEL",
                "Gateway Background Service",
                NotificationManager.IMPORTANCE_LOW // Low importance so it doesn't beep/vibrate constantly
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
