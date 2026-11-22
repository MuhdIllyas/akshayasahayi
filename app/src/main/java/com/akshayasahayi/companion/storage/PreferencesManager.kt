package com.akshayasahayi.companion.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

// Extension to create the DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "companion_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val CENTRE_ID = intPreferencesKey("centre_id")
        val CENTRE_NAME = stringPreferencesKey("centre_name") // <-- 1. NEW KEY
        val INSTALLATION_ID = stringPreferencesKey("installation_id")
        val DEVICE_NAME = stringPreferencesKey("device_name")
    }

    // --- Write Data ---
    // <-- 2. UPDATED TO ACCEPT centreName
    suspend fun saveAuthData(token: String, centreId: Int, centreName: String) {
        context.dataStore.edit { prefs ->
            prefs[JWT_TOKEN] = token
            prefs[CENTRE_ID] = centreId
            prefs[CENTRE_NAME] = centreName // <-- 3. SAVE IT
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.remove(JWT_TOKEN)
            prefs.remove(CENTRE_ID)
            prefs.remove(CENTRE_NAME) // <-- 4. CLEAR IT ON LOGOUT
        }
    }

    // --- Read Data ---
    val jwtTokenFlow: Flow<String?> = context.dataStore.data.map { it[JWT_TOKEN] }
    val centreIdFlow: Flow<Int?> = context.dataStore.data.map { it[CENTRE_ID] }

    // <-- 5. EXPOSE IT TO THE UI
    val centreNameFlow: Flow<String?> = context.dataStore.data.map { it[CENTRE_NAME] }

    // --- Installation ID Logic ---
    // Generates a UUID on first launch and reuses it forever
    suspend fun getOrGenerateInstallationId(): String {
        var id: String? = null
        context.dataStore.edit { prefs ->
            id = prefs[INSTALLATION_ID]
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs[INSTALLATION_ID] = id!!
            }
        }
        return id!!
    }
}