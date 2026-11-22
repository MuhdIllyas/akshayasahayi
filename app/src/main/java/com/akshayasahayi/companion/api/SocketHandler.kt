package com.akshayasahayi.companion.api

import io.socket.client.IO
import io.socket.client.Socket
import com.akshayasahayi.companion.utils.Outbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object SocketHandler {

    private var mSocket: Socket? = null

    // This state flow lets the UI instantly know if the socket drops
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    fun establishConnection(token: String) {
        try {
            // Already created (connected, or reconnecting by itself): do nothing.
            // Creating a second socket here would leave the first one running.
            if (mSocket != null) return

            // Configure the Socket with the JWT Auth payload
            val options = IO.Options().apply {
                auth = mapOf("token" to token)
                // Reconnection logic is handled automatically by the library
                reconnection = true
            }

            // Connect to your staging server
            mSocket = IO.socket("https://api.akshayasahayi.com/", options)

            // Listeners for connection state
            mSocket?.on(Socket.EVENT_CONNECT) {
                _isConnected.value = true
                // Send anything that arrived while we were offline
                Outbox.flush()
            }

            mSocket?.on(Socket.EVENT_DISCONNECT) {
                _isConnected.value = false
                Outbox.onDisconnected()
            }

            mSocket?.on(Socket.EVENT_CONNECT_ERROR) {
                _isConnected.value = false
                it.forEach { error ->
                    println("Socket Error: $error")
                }
            }

            mSocket?.connect()

        } catch (e: Exception) {
            e.printStackTrace()
            _isConnected.value = false
        }
    }

    fun closeConnection() {
        mSocket?.disconnect()
        mSocket?.off()
        mSocket = null
        _isConnected.value = false
    }

    // Expose the raw socket if a specific screen needs to emit an event
    fun getSocket(): Socket? = mSocket
}