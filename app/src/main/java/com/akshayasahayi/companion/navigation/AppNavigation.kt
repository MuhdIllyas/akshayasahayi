package com.akshayasahayi.companion.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akshayasahayi.companion.ui.screens.DashboardScreen
import com.akshayasahayi.companion.ui.screens.DeviceRegistrationScreen
import com.akshayasahayi.companion.ui.screens.SplashScreen
import com.akshayasahayi.companion.utils.Constants

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Constants.ROUTE_SPLASH
    ) {
        composable(Constants.ROUTE_SPLASH) {
            SplashScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Constants.ROUTE_SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Constants.ROUTE_REGISTRATION) {
            DeviceRegistrationScreen(
                onRegistrationSuccess = {
                    navController.navigate(Constants.ROUTE_DASHBOARD) {
                        popUpTo(Constants.ROUTE_REGISTRATION) { inclusive = true }
                    }
                }
            )
        }

        composable(Constants.ROUTE_DASHBOARD) {
            DashboardScreen(
                // ---> NEW: Tells the Dashboard where to go on Logout <---
                onLogoutSuccess = {
                    navController.navigate(Constants.ROUTE_REGISTRATION) {
                        popUpTo(Constants.ROUTE_DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}