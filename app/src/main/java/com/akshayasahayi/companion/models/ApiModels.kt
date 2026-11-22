package com.akshayasahayi.companion.models

import com.google.gson.annotations.SerializedName

// What we send to the backend
data class PairRequest(
    @SerializedName("pairingToken") val pairingToken: String,
    @SerializedName("installationId") val installationId: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("appVersion") val appVersion: String = "1.0.0"
)

// What the backend sends back
data class PairResponse(
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("centre_id") val centreId: Int,
    @SerializedName("centre_name") val centreName: String
)

// What we send every 60 seconds
data class HeartbeatRequest(
    @SerializedName("batteryLevel") val batteryLevel: Int,
    @SerializedName("networkStatus") val networkStatus: String
)

// A generic success response from the server
data class GenericResponse(
    @SerializedName("message") val message: String
)

// What we get when we ask for the Dashboard status
data class StatusResponse(
    @SerializedName("message") val message: String,
    @SerializedName("device") val device: DeviceData
)

data class DeviceData(
    @SerializedName("id") val id: String,
    @SerializedName("centre_id") val centreId: Int,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_online") val isOnline: Boolean
)