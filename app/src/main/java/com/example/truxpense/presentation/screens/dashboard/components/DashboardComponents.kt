package com.example.truxpense.presentation.screens.dashboard.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.truxpense.R
import com.example.truxpense.presentation.navigation.BottomNavBarMenu
import com.example.truxpense.presentation.theme.AppDialogTheme
import com.example.truxpense.presentation.theme.DashboardDimens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.unit.dp


@Composable
fun DashboardBottomBar(
    items: List<BottomNavBarMenu>,
    isSelected: (BottomNavBarMenu) -> Boolean,
    onItemSelected: (BottomNavBarMenu) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = NavigationBarDefaults.Elevation,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider(modifier = Modifier.fillMaxWidth().height(DashboardDimens.dividerHeight))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DashboardDimens.bottomNavHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val selected = isSelected(item)
                    val contentAlpha = if (selected) 1f else 0.6f

                    val baseScale = if (item == BottomNavBarMenu.Analytics) 1.12f else 1f
                    val scaleAnim = remember { Animatable(baseScale) }

                    LaunchedEffect(selected) {
                        if (selected) {
                            scaleAnim.animateTo(baseScale * 0.88f, tween(durationMillis = 80))
                            scaleAnim.animateTo(
                                baseScale * 1.06f,
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                            )
                            scaleAnim.animateTo(
                                baseScale,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        } else {
                            scaleAnim.animateTo(baseScale, tween(durationMillis = 120))
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }) { onItemSelected(item) }
                            .padding(vertical = DashboardDimens.spaceMd),
                    ) {
                        Icon(
                            painter = painterResource(if (selected) item.selectedIcon else item.icon),
                            contentDescription = item.label,
                            modifier = Modifier
                                .size(21.dp)
                                .scale(scaleAnim.value),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha)
                        )

                        Spacer(Modifier.height(DashboardDimens.spaceXs))

                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = DashboardDimens.textSm),
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionEnableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(DashboardDimens.buttonHeightSm),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF4A62A),
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = DashboardDimens.spaceLg, vertical = DashboardDimens.spaceSm),
    ) {
        Text(
            text = "Enable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun SmsPermissionBanner(
    modifier: Modifier = Modifier,
    onGranted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val permission = Manifest.permission.READ_SMS

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            onGranted?.invoke()
        } else {
            val shouldShowRationale =
                activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
            if (shouldShowRationale) {
                showRationaleDialog = true
            } else {
                showPermanentlyDeniedDialog = true
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = DashboardDimens.spaceLg)
                .padding(top = DashboardDimens.spaceMd, bottom = DashboardDimens.spaceLg)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.sms_icon),
                    contentDescription = "Sms Icon",
                    tint = Color(0xFFF4A62A),
                    modifier = Modifier.size(DashboardDimens.iconMd)
                )
                Spacer(modifier = Modifier.width(DashboardDimens.spaceMd))
                Text(
                    text = "Enable SMS access",
                    style = MaterialTheme.typography.labelMedium.copy(
                        lineHeight = DashboardDimens.lineHeightBanner,
                        color = Color(0xFFF4A62A),
                    ),
                )
            }

            PermissionEnableButton(
                onClick = { permissionLauncher.launch(permission) },
                modifier = Modifier.padding(horizontal = DashboardDimens.spaceLg, vertical = DashboardDimens.spaceXs)
            )
        }
    }

    if (showRationaleDialog) {
        AppConfirmDialog(
            title = "Why we need SMS access",
            message = "We need access to your SMS to detect bank transaction messages and automatically categorize your expenses. Only transaction messages are read.",
            confirmLabel = "Grant",
            cancelLabel = "Cancel",
            isDestructive = false,
            iconRes = null,
            onConfirm = {
                showRationaleDialog = false
                permissionLauncher.launch(permission)
            },
            onDismiss = { showRationaleDialog = false },
        )
    }

    if (showPermanentlyDeniedDialog) {
        AppConfirmDialog(
            title = "Permission blocked",
            message = "SMS permission has been permanently denied. Open app settings to grant the permission.",
            confirmLabel = "Open settings",
            cancelLabel = "Cancel",
            isDestructive = false,
            iconRes = null,
            onConfirm = {
                showPermanentlyDeniedDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { showPermanentlyDeniedDialog = false },
        )
    }
}

@Composable
fun AddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Add",
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.background,
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = contentDescription)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardBottomBarPreview() {
    val items = BottomNavBarMenu.all
    DashboardBottomBar(items = items, isSelected = { it == BottomNavBarMenu.Home }, onItemSelected = {})
}
