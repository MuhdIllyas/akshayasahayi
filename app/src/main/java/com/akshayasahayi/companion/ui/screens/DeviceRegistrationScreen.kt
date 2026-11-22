package com.akshayasahayi.companion.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akshayasahayi.companion.ui.components.PrimaryButton
import com.akshayasahayi.companion.ui.components.QrScanner
import com.akshayasahayi.companion.ui.components.StatusCard
import com.akshayasahayi.companion.ui.viewmodel.RegistrationViewModel

@Composable
fun DeviceRegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    viewModel: RegistrationViewModel = viewModel() // Inject ViewModel here
) {
    val context = LocalContext.current

    // ViewModel States
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.error.collectAsState()
    val navigateToDashboard by viewModel.navigateToDashboard.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) isScanning = true
    }

    // Trigger navigation when ViewModel says success
    LaunchedEffect(navigateToDashboard) {
        if (navigateToDashboard) {
            onRegistrationSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Akshaya Companion",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Setup New Device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Show Error Message if API fails
            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (isLoading) {
                // Loading State
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pairing with CRM...")
            } else if (isScanning && hasCameraPermission) {
                // Live Camera Scanner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    QrScanner(
                        onQrCodeScanned = { qrResult ->
                            isScanning = false // Turn off camera
                            viewModel.pairDevice(qrResult) // Send to ViewModel
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Point your camera at the CRM screen.")
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { isScanning = false }) {
                    Text("Cancel Scan")
                }
            } else {
                // Default State
                StatusCard(title = "Connection Status", statusText = "Not Registered", isOnline = false)
                Spacer(modifier = Modifier.height(32.dp))
                PrimaryButton(
                    text = "Scan QR Code",
                    onClick = {
                        viewModel.clearError()
                        if (hasCameraPermission) isScanning = true
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }

        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}