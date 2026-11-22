package com.akshayasahayi.companion.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshayasahayi.companion.api.ApiClient
import com.akshayasahayi.companion.models.PairRequest
import com.akshayasahayi.companion.storage.PreferencesManager
import com.akshayasahayi.companion.utils.HardwareUtils // <-- 1. Added Import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _navigateToDashboard = MutableStateFlow(false)
    val navigateToDashboard = _navigateToDashboard.asStateFlow()

    fun pairDevice(qrResult: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. Parse the JSON from the QR Code
                val json = JSONObject(qrResult)
                val pairingToken = json.optString("pairingToken")

                if (pairingToken.isEmpty()) {
                    _error.value = "Invalid QR Code format. Please scan a CRM pairing code."
                    _isLoading.value = false
                    return@launch
                }

                // 2. Get permanent hardware ID, full phone model, and app version
                val installationId = prefs.getOrGenerateInstallationId()
                val deviceName = HardwareUtils.getDeviceName() // <-- 2. Updated to use HardwareUtils
                val appVersion = "1.0.0" // <-- 3. Added app version

                // 3. Make the API Call to your Node backend
                val request = PairRequest(pairingToken, installationId, deviceName, appVersion) // <-- 4. Updated Request Payload
                val response = ApiClient.apiService.pairDevice(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // 4. Save the stateless JWT and Centre ID!
                    prefs.saveAuthData(body.token, body.centreId, body.centreName)

                    // 5. Trigger navigation to Dashboard
                    _navigateToDashboard.value = true
                } else if (response.code() == 401) {
                    // <-- 5. Specific error for expired 15-minute token
                    _error.value = "QR code expired or invalid. Please generate a new one from Admin Settings."
                } else {
                    _error.value = "Pairing failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Network Error: Cannot reach CRM server. Check Wi-Fi."
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}