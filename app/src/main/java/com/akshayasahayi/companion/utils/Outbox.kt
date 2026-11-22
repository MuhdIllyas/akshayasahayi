package com.akshayasahayi.companion.utils

import android.content.Context
import android.util.Log
import com.akshayasahayi.companion.api.SocketHandler
import io.socket.client.Ack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Everything the phone forwards (SMS, UPI app notifications) goes through here.
 *
 * Each message is first saved to a file, then sent over the socket. It is only
 * removed when the server confirms it (Socket.IO ack). If the phone is offline,
 * the server is restarting or the app was killed, the message waits and is sent
 * on the next connect — so a bank credit SMS is never lost.
 *
 * Every message carries a clientRef; the server saves a resent message only once.
 */
object Outbox {

    private const val TAG = "Outbox"
    private const val FILE_NAME = "outbox.json"
    private const val MAX_ITEMS = 500
    private const val MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000 // 3 days

    private val lock = Any()
    private var appContext: Context? = null

    // clientRefs sent and waiting for the server's ack (not resent meanwhile)
    private val inFlight = mutableSetOf<String>()

    private val _pending = MutableStateFlow(0)
    /** How many messages are waiting to be sent (shown on the dashboard). */
    val pending = _pending.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        synchronized(lock) { _pending.value = load().length() }
    }

    /** Save a message and try to send it right away. */
    fun enqueue(context: Context, event: String, payload: JSONObject) {
        init(context)
        synchronized(lock) {
            val items = load()
            val ref = payload.optString("clientRef")
            for (i in 0 until items.length()) {
                if (items.getJSONObject(i).optString("ref") == ref) return // already queued
            }
            items.put(JSONObject().apply {
                put("ref", ref)
                put("event", event)
                put("payload", payload)
                put("queuedAt", System.currentTimeMillis())
            })
            // Keep the newest MAX_ITEMS
            val trimmed = if (items.length() > MAX_ITEMS) {
                JSONArray().also { out ->
                    for (i in items.length() - MAX_ITEMS until items.length()) out.put(items.get(i))
                }
            } else items
            save(trimmed)
        }
        flush()
    }

    /** Send everything waiting. Safe to call often (on connect, every minute…). */
    fun flush() {
        val socket = SocketHandler.getSocket()
        if (socket == null || !socket.connected()) return

        val toSend = mutableListOf<JSONObject>()
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val items = load()
            val kept = JSONArray()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (now - item.optLong("queuedAt", now) > MAX_AGE_MS) continue // too old, drop
                kept.put(item)
                val ref = item.optString("ref")
                if (ref !in inFlight) {
                    inFlight.add(ref)
                    toSend.add(item)
                }
            }
            if (kept.length() != items.length()) save(kept)
        }

        for (item in toSend) {
            val ref = item.optString("ref")
            socket.emit(item.optString("event"), item.getJSONObject("payload"), Ack { args ->
                val reply = args.firstOrNull() as? JSONObject
                synchronized(lock) {
                    inFlight.remove(ref)
                    if (reply?.optBoolean("ok") == true) remove(ref)
                }
                if (reply?.optBoolean("ok") != true) Log.w(TAG, "Server did not accept $ref, will retry")
            })
        }
        if (toSend.isNotEmpty()) Log.d(TAG, "Sent ${toSend.size} queued message(s)")
    }

    /** Socket dropped: acks for in-flight messages will never come, so resend them later. */
    fun onDisconnected() {
        synchronized(lock) { inFlight.clear() }
    }

    // ---------- file storage (call inside lock) ----------

    private fun file(): File? = appContext?.let { File(it.filesDir, FILE_NAME) }

    private fun load(): JSONArray {
        val f = file() ?: return JSONArray()
        return try {
            if (f.exists()) JSONArray(f.readText()) else JSONArray()
        } catch (e: Exception) {
            Log.e(TAG, "Outbox file unreadable, starting empty", e)
            JSONArray()
        }
    }

    private fun save(items: JSONArray) {
        val f = file() ?: return
        val tmp = File(f.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(items.toString())
        if (!tmp.renameTo(f)) { f.writeText(items.toString()); tmp.delete() }
        _pending.value = items.length()
    }

    private fun remove(ref: String) {
        val items = load()
        val kept = JSONArray()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optString("ref") != ref) kept.put(item)
        }
        save(kept)
    }

    /** Stable id for a message, so a resend is recognised by the server. */
    fun refFor(vararg parts: Any?): String {
        val text = parts.joinToString("|") { it?.toString() ?: "" }
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
