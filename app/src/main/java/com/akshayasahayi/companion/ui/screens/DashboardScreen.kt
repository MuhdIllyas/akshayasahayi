package com.akshayasahayi.companion.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.akshayasahayi.companion.utils.Outbox
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akshayasahayi.companion.ui.viewmodel.DashboardViewModel
import androidx.compose.foundation.border

// --- Custom Palette Matched to Your Logo ---
private val SurfaceBg = Color(0xFFF8FAFC)
private val CardBorder = Color(0xFFE2E8F0)
private val TextMain = Color(0xFF0F172A)
private val TextSub = Color(0xFF64748B)

// Brand Colors
private val PrimaryNavy = Color(0xFF0A2240) // Logo Dark Blue
private val PrimaryTeal = Color(0xFF00A08A) // Logo Teal Green

// Icon Styling (Teal theme)
private val IconBgTeal = Color(0xFFE5F5F3) // Very light teal background
private val IconTintTeal = PrimaryTeal

// Badge Styling
private val BadgeActiveBg = Color(0xFFE5F5F3)
private val BadgeActiveText = PrimaryTeal
private val BadgeInactiveBg = Color(0xFFFEF2F2)
private val BadgeInactiveText = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogoutSuccess: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current

    // UI States
    val deviceData by viewModel.deviceData.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isServerConnected by viewModel.isServerConnected.collectAsState()
    val isSmsGatewayActive by viewModel.isSmsGatewayActive.collectAsState()
    val centreName by viewModel.centreName.collectAsState()
    val navigateToLogin by viewModel.navigateToLogin.collectAsState()

    LaunchedEffect(navigateToLogin) {
        if (navigateToLogin) onLogoutSuccess()
    }

    // Messages saved on the phone and not yet confirmed by the server
    val pendingCount by Outbox.pending.collectAsState()

    // "Notification access" for reading UPI app payment notifications
    fun hasNotificationAccess() =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    var isUpiAlertsActive by remember { mutableStateOf(hasNotificationAccess()) }

    // Re-check permissions whenever the user comes back from Settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setSmsGatewayStatus(hasSmsPermission)
        isUpiAlertsActive = hasNotificationAccess()
        Outbox.init(context)
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                permissions[Manifest.permission.READ_SMS] == true
        viewModel.setSmsGatewayStatus(isGranted)
    }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, CardBorder),
                color = Color.White
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Akshaya Companion",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryNavy
                            )
                            Text(
                                text = "$centreName | ${deviceData?.deviceName ?: "Loading..."}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSub
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BadgeInactiveBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = BadgeInactiveText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- Section 1: Server ---
            Text(
                text = "NETWORK STATUS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSub,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
            )

            ModernStatusCard(
                title = "CRM Cloud Server",
                subtitle = if (isServerConnected) "Socket connected securely" else "Reconnecting to server...",
                icon = Icons.Rounded.Router,
                isActive = isServerConnected
            )

            // --- Section 2: Gateways ---
            Text(
                text = "HARDWARE GATEWAYS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSub,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
            )

            ModernStatusCard(
                title = "SMS Gateway",
                subtitle = when {
                    !isSmsGatewayActive -> "Tap to grant SMS permissions"
                    pendingCount > 0 -> "$pendingCount message(s) waiting to send"
                    else -> "Forwarding OTPs and bank credit SMS"
                },
                icon = Icons.Rounded.Message,
                isActive = isSmsGatewayActive,
                onClick = {
                    if (!isSmsGatewayActive) {
                        smsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                        )
                    }
                }
            )

            ModernStatusCard(
                title = "UPI Payment Alerts",
                subtitle = if (isUpiAlertsActive) "Reading GPay / Paytm / PhonePe payments"
                           else "Tap to allow notification access",
                icon = Icons.Rounded.CurrencyRupee,
                isActive = isUpiAlertsActive,
                onClick = {
                    // Opens Settings → Notification access; turn on "Akshaya Companion"
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            ModernStatusCard(
                title = "Network Scanner",
                subtitle = "Pending configuration setup",
                icon = Icons.Rounded.QrCodeScanner,
                isActive = false
            )

            // --- Section 3: Device Health ---
            Text(
                text = "DEVICE HEALTH",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSub,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
            )

            ModernStatusCard(
                title = "Device Battery",
                subtitle = "Physical hardware status",
                icon = Icons.Rounded.BatteryFull,
                isActive = batteryLevel > 20,
                customStatusText = "$batteryLevel%"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- NEW: Custom Component to mimic the Web UI Rows ---
@Composable
fun ModernStatusCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    customStatusText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp // Flat design!
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Tinted Icon Box (UPDATED TO TEAL)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IconBgTeal), // <-- Fixed
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = IconTintTeal, // <-- Fixed
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSub
                )
            }

            // Right: Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) BadgeActiveBg else BadgeInactiveBg)
                    .border(
                        width = 1.dp,
                        color = if (isActive) BadgeActiveText.copy(alpha = 0.2f) else BadgeInactiveText.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = customStatusText ?: if (isActive) "Active" else "Offline",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) BadgeActiveText else BadgeInactiveText
                )
            }
        }
    }
}