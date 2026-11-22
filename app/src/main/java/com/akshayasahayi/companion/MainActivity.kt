package com.akshayasahayi.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.akshayasahayi.companion.navigation.AppNavigation
import com.akshayasahayi.companion.ui.theme.AkshayaSahayiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AkshayaSahayiTheme {
                // Now MainActivity knows exactly where to find this!
                AppNavigation()
            }
        }
    }
}