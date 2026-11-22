package com.akshayasahayi.companion.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshayasahayi.companion.storage.PreferencesManager
import com.akshayasahayi.companion.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// 1. Changed to AndroidViewModel to access the Application Context
class SplashViewModel(application: Application) : AndroidViewModel(application) {

    // 2. Initialize your PreferencesManager
    private val prefs = PreferencesManager(application)

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo = _navigateTo.asStateFlow()

    init {
        checkRegistrationStatus()
    }

    private fun checkRegistrationStatus() {
        viewModelScope.launch {
            // Give the user 1.5 seconds to admire your new logo!
            delay(1500)

            // 3. Safely pause and wait for the physical disk to read the actual token
            val token = prefs.jwtTokenFlow.firstOrNull()

            // 4. Make the real routing decision based on saved data
            if (token.isNullOrBlank()) {
                _navigateTo.value = Constants.ROUTE_REGISTRATION
            } else {
                _navigateTo.value = Constants.ROUTE_DASHBOARD
            }
        }
    }

    fun navigationHandled() {
        _navigateTo.value = null
    }
}