package com.example.truxpense.presentation.screens.onboarding.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
                                                                                                                                                                                                                                                    import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.theme.AppDialogTheme
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun SmsPermission(
    modifier: Modifier = Modifier,
    onAllow: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val permission = Manifest.permission.READ_SMS

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    // Two-step skip flow state
    var skipConfirmed by remember { mutableStateOf(false) } // false = default 'Skip', true = show confirmation UI (icon/small text change)

    // Handle back navigation
    // When user presses system back on this screen, exit the app instead of navigating back
    BackHandler(enabled = true) {
        activity?.finishAffinity()
    }

    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            // Permission granted -> navigate to centralized Loading screen via callback
            onAllow?.invoke()
        } else {
            // Determine whether we should show rationale or it's permanently denied
            val shouldShowRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
            if (shouldShowRationale) {
                showRationaleDialog = true
            } else {
                // permanently denied (or device policy)
                showPermanentlyDeniedDialog = true
            }
        }
    }

    // Helper to start permission request or handle granted
    fun requestPermission() {
        val status = ContextCompat.checkSelfPermission(context, permission)
        if (status == PackageManager.PERMISSION_GRANTED) {
            // Already granted: navigate to Loading via callback
            onAllow?.invoke()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Scaffold(
        bottomBar = {
            Column(modifier = modifier) {
                AuthButton(
                    onClick = { requestPermission() },
                    text = "Allow access",
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Skip / Continue without SMS button: first tap toggles confirmation UI, second tap navigates to Loading
                TextButton(
                    onClick = {
                        if (!skipConfirmed) {
                            // First tap: switch icon and button label to confirm
                            skipConfirmed = true
                        } else {
                            // Second tap: navigate to Loading via callback
                            onSkip?.invoke()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (!skipConfirmed) "Skip" else "Continue without SMS",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = 32.dp)
                .padding(padding)
                .fillMaxSize()
                .clearFocusOnTap(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Allow SMS access",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Swap between sms_1_icon and sms_icon when skip is confirmed
            val illustrationRes = if (!skipConfirmed) R.drawable.sms_1_icon else R.drawable.sms_icon
            Image(
                painter = painterResource(id = illustrationRes),
                contentDescription = "SMS permission illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )


            Spacer(modifier = Modifier.height(20.dp))

            // Bulleted list explaining SMS access
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "We use SMS to track your expenses automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Only bank messages are read.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Personal messages stay private.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // helpers to close dialogs (avoids linter "assigned value is never read" warnings)
    fun closeRationale() { showRationaleDialog = false }
    fun closePermanentlyDenied() { showPermanentlyDeniedDialog = false }

    // Rationale dialog when permission denied but should show rationale
    if (showRationaleDialog) {
        AppDialogTheme {
            AlertDialog(
                onDismissRequest = { closeRationale() },
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                title = { Text("Why we need SMS access") },
                text = { Text("We need access to your SMS to detect bank transaction messages and automatically categorize your expenses. Only transaction messages are read.") },
                confirmButton = {
                    Button(onClick = {
                        closeRationale()
                        permissionLauncher.launch(permission)
                    }) { Text("Grant") }
                },
                dismissButton = {
                    TextButton(onClick = { closeRationale() }) { Text("Cancel") }
                }
            )
        }
    }

    // Permanently denied dialog: guide user to app settings
    if (showPermanentlyDeniedDialog) {
        AppDialogTheme {
            AlertDialog(
                onDismissRequest = { closePermanentlyDenied() },
                shape = RoundedCornerShape(DashboardDimens.cornerCard),
                title = { Text("Permission blocked") },
                text = { Text("SMS permission has been permanently denied. Open app settings to grant the permission.") },
                confirmButton = {
                    Button(onClick = {
                        closePermanentlyDenied()
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) { Text("Open settings") }
                },
                dismissButton = {
                    TextButton(onClick = { closePermanentlyDenied() }) { Text("Cancel") }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SmsPermissionPreview() {
    SmsPermission()
}