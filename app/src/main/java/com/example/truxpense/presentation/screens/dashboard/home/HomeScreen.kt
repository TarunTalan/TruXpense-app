package com.example.truxpense.presentation.screens.dashboard.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color.rgb
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.truxpense.R
import com.example.truxpense.presentation.navigation.BottomNavBarMenu


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: (() -> Unit)? = null) {
    val vm = hiltViewModel<HomeViewModel>()
    val username by vm.username.collectAsState(initial = null)

    val navController = rememberNavController()
    val items = BottomNavBarMenu.all

    // Track current route to highlight the right tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(username = username)
        },
        bottomBar = {
            DashboardBottomBar(
                items = items,
                currentRoute = currentRoute,
                onItemSelected = { destination ->
                    navController.navigate(destination.route) {
                        // Single-Top + restore state behavior for bottom nav
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavBarMenu.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(BottomNavBarMenu.Home.route) {
                HomeTabScreen(
                    username = username,
                    vm = vm,
                    onLogout = onLogout,
                    onAddExpense = { vm.setExpenseCount(vm.expenseCount.value + 1) }
                )
            }
            composable(BottomNavBarMenu.Transactions.route) {
                TransactionsTab()
            }
            composable(BottomNavBarMenu.Budget.route) {
                BudgetTab()
            }
            composable(BottomNavBarMenu.Analytics.route) {
                AnalyticsTab()
            }
            composable(BottomNavBarMenu.Settings.route) {
                SettingsTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(username: String?) {
    TopAppBar(
        title = {
            Text(
                text = if (username.isNullOrBlank()) "Hi, there!" else "Hi, $username",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 10.dp)
            )
        },
        actions = {
            val isDark = isSystemInDarkTheme()
            IconButton(onClick = { /* TODO: Handle profile click */ }) {
                Icon(
                    painter = painterResource(id = if (isDark) R.drawable.profile_dark_icon else R.drawable.profile_icon),
                    contentDescription = "Profile",
                    tint = Color.Unspecified
                )
            }
            IconButton(onClick = { /* TODO: Handle notifications click */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.notifications_icon),
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        windowInsets = WindowInsets.statusBars,
        modifier = Modifier
            .fillMaxWidth()
    )
}

@Composable
fun DashboardBottomBar(
    items: List<BottomNavBarMenu>,
    currentRoute: String?,
    onItemSelected: (BottomNavBarMenu) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = NavigationBarDefaults.Elevation,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // top border for bottom bar
        Column {
            HorizontalDivider(
                // Replace default divider with a canvas line drawn with round end caps
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            )
            // existing row content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    val contentAlpha = if (selected) 1f else 0.6f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(item) {
                                detectTapGestures(onTap = { onItemSelected(item) })
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (selected) item.selectedIcon else item.icon
                            ),
                            contentDescription = item.label,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp).alpha(contentAlpha)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val labelFontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = labelFontWeight,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


// New: SMS Permission Banner used on Home screen when READ_SMS is not granted
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

    fun requestPermission() {
        val status = ContextCompat.checkSelfPermission(context, permission)
        if (status == PackageManager.PERMISSION_GRANTED) {
            onGranted?.invoke()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    val isDark = isSystemInDarkTheme()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.sms_icon),
                            contentDescription = "Sms Icon",
                            tint = Color(rgb(244, 166, 42)),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enable SMS access",
                            style = TextStyle(
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(rgb(244, 166, 42)),
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Use a small, reusable PermissionEnableButton for consistent styling
                    PermissionEnableButton(
                        onClick = { requestPermission() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Auto track expenses from bank messages.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(rgb(193, 199, 205)) else MaterialTheme.colorScheme.onSecondary,
                    textAlign = TextAlign.Center,
                )
            }


        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Why we need SMS access") },
            text = { Text("We need access to your SMS to detect bank transaction messages and automatically categorize your expenses. Only transaction messages are read.") },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    permissionLauncher.launch(permission)
                }) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = { Text("Permission blocked") },
            text = { Text("SMS permission has been permanently denied. Open app settings to grant the permission.") },
            confirmButton = {
                Button(onClick = {
                    showPermanentlyDeniedDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentlyDeniedDialog = false }) { Text("Cancel") }
            }
        )
    }
}


// ==================== TAB SCREENS ====================


@Composable
private fun TransactionsTab() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "View and manage your transactions here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetTab() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Budget",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Set and track your budgets here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyticsTab() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "View your spending analytics here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsTab() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Manage your app settings here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionEnableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(28.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF4A62A),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Enable",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Preview(showBackground = true,uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SmsPermissionBannerPreview() {
    SmsPermissionBanner()
}

@Preview(showBackground = true)
@Composable
fun DashboardBottomBarPreview() {
    val items = BottomNavBarMenu.all
    DashboardBottomBar(
        items = items,
        currentRoute = BottomNavBarMenu.Home.route,
        onItemSelected = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardTopBarPreview() {
    DashboardTopBar(username = "Tarun")
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "TopBar - Dark")
@Composable
fun DashboardTopBarDarkPreview() {
    DashboardTopBar(username = "Tarun")
}
