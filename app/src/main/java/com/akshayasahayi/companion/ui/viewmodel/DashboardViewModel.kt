package com.akshayasahayi.companion.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshayasahayi.companion.api.ApiClient
import com.akshayasahayi.companion.models.DeviceData
import com.akshayasahayi.companion.models.HeartbeatRequest
import com.akshayasahayi.companion.services.GatewayService
import com.akshayasahayi.companion.storage.PreferencesManager
import com.akshayasahayi.companion.utils.HardwareUtils
import com.akshayasahayi.companion.api.SocketHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val appContext = application.applicationContext

    // UI States
    private val _deviceData = MutableStateFlow<DeviceData?>(null)
    val deviceData = _deviceData.asStateFlow()

    // ---> Dedicated Centre Name State <---
    private val _centreName = MutableStateFlow("Loading...")
    val centreName = _centreName.asStateFlow()

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected = _isServerConnected.asStateFlow()

    private val _isSmsGatewayActive = MutableStateFlow(false)
    val isSmsGatewayActive = _isSmsGatewayActive.asStateFlow()

    init {
        // ---> NEW: Start the background service to keep the app alive <---
        val serviceIntent = Intent(appContext, GatewayService::class.java)
        ContextCompat.startForegroundService(appContext, serviceIntent)

        // Listen to DataStore for the Centre Name instantly
        viewModelScope.launch {
            prefs.centreNameFlow.collect { name ->
                _centreName.value = name ?: "Unknown Centre"
            }
        }

        // Kick off both tasks as soon as the Dashboard opens
        fetchInitialStatus()
        startHeartbeatLoop()
    }

    // Add this function so the UI can update the state
    fun setSmsGatewayStatus(isActive: Boolean) {
        _isSmsGatewayActive.value = isActive
    }

    private fun fetchInitialStatus() {
        viewModelScope.launch {
            val token = prefs.jwtTokenFlow.firstOrNull() ?: return@launch

            // Start the real-time Socket connection
            SocketHandler.establishConnection(token)

            try {
                val response = ApiClient.apiService.getStatus("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _deviceData.value = response.body()!!.device
                    _isServerConnected.value = true
                } else {
                    _isServerConnected.value = false
                }
            } catch (e: Exception) {
                _isServerConnected.value = false
                e.printStackTrace()
            }
        }
    }

    private fun startHeartbeatLoop() {
        viewModelScope.launch {
            // Infinite loop that runs as long as the ViewModel is alive
            while (isActive) {
                val token = prefs.jwtTokenFlow.firstOrNull()

                if (token != null) {
                    // 1. Read the physical battery sensor
                    val currentBattery = HardwareUtils.getBatteryPercentage(appContext)
                    _batteryLevel.value = currentBattery

                    // 2. Send the heartbeat to the CRM
                    try {
                        val request = HeartbeatRequest(
                            batteryLevel = currentBattery,
                            networkStatus = "Connected" // Future: Detect real Wi-Fi/LTE status
                        )
                        val response = ApiClient.apiService.sendHeartbeat("Bearer $token", request)

                        if (response.isSuccessful) {
                            _isServerConnected.value = true
                        }
                    } catch (e: Exception) {
                        _isServerConnected.value = false
                    }
                }

                // Wait exactly 60 seconds before looping again
                delay(60_000)
            }
        }
    }

    // ---> Logout State and Function <---
    private val _navigateToLogin = MutableStateFlow(false)
    val navigateToLogin = _navigateToLogin.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            // 1. Tell the CRM Cloud that this device is unpairing
            val token = prefs.jwtTokenFlow.firstOrNull()
            if (token != null) {
                try {
                    ApiClient.apiService.unpairDevice("Bearer $token")
                } catch (e: Exception) {
                    e.printStackTrace() // Ignore network errors if it's already offline
                }
            }

            // 2. Stop the background service so it doesn't run forever
            val serviceIntent = Intent(appContext, GatewayService::class.java)
            appContext.stopService(serviceIntent)

            // 3. Disconnect the socket
            SocketHandler.closeConnection()

            // 4. Wipe the DataStore
            prefs.clearAuthData()

            // 5. Trigger navigation back to the QR screen
            _navigateToLogin.value = true
        }
    }
}