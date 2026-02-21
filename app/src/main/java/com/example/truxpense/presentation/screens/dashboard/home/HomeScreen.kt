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
import com.example.truxpense.presentation.screens.dashboard.analytic.AnalyticsEmptyScreen
import com.example.truxpense.presentation.screens.dashboard.analytic.AnalyticsScreen
import com.example.truxpense.presentation.screens.dashboard.analytic.AnalyticsViewModel
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetCategory
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetScreen
import com.example.truxpense.presentation.screens.dashboard.components.AppTopBar
import com.example.truxpense.presentation.screens.dashboard.settings.SettingsScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: (() -> Unit)? = null,
    onNavigateToAddExpense: (() -> Unit)? = null,
    onNavigateToAddBudget: (() -> Unit)? = null,
    onNavigateToBudgetDetail: ((BudgetCategory) -> Unit)? = null
) {
    val vm = hiltViewModel<HomeViewModel>()
    val username by vm.username.collectAsState(initial = null)

    val navController = rememberNavController()
    val items = BottomNavBarMenu.all

    // Track current route to highlight the right tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // No scaffold-level FAB: HomeTabScreenContent supplies a styled FAB so remove duplicate
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
                    onAddExpense = { onNavigateToAddExpense?.invoke() }
                )
            }
            composable(BottomNavBarMenu.Transactions.route) {
                TransactionsTab()
            }
            composable(BottomNavBarMenu.Budget.route) {
                BudgetTab(
                    onNavigateToAddBudget = { onNavigateToAddBudget?.invoke() },
                    onNavigateToBudgetDetail = { cat: BudgetCategory -> onNavigateToBudgetDetail?.invoke(cat) }
                )
            }
            composable(BottomNavBarMenu.Analytics.route) {
                AnalyticsTab(
                    onAddExpense = { onNavigateToAddExpense?.invoke() }
                )
            }
            composable(BottomNavBarMenu.Settings.route) {
                SettingsTab()
            }
        }
    }
}

@Composable
fun DashboardTopBar(username: String?) {
    AppTopBar(username = username, showBack = false)
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

    // Dialog close helpers (avoids inline state assignments which some analyzers flag)
    fun closeRationale() { showRationaleDialog = false }
    fun openRationale() { showRationaleDialog = true }
    fun closePermanentlyDenied() { showPermanentlyDeniedDialog = false }
    fun openPermanentlyDenied() { showPermanentlyDeniedDialog = true }

    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            onGranted?.invoke()
        } else {
            val shouldShowRationale =
                activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
            if (shouldShowRationale) {
                openRationale()
            } else {
                openPermanentlyDenied()
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
            onDismissRequest = { closeRationale() },
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

    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { closePermanentlyDenied() },
            title = { Text("Permission blocked") },
            text = { Text("SMS permission has been permanently denied. Open app settings to grant the permission.") },
            confirmButton = {
                Button(onClick = {
                    closePermanentlyDenied()
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


// ==================== TAB SCREENS ====================


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsTab() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "Transactions") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

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
}

@Composable
private fun BudgetTab(
    onNavigateToAddBudget: (() -> Unit)? = null,
    onNavigateToBudgetDetail: ((BudgetCategory) -> Unit)? = null
) {
    // Delegate to the repository-backed BudgetScreen (shows only real budgets).
    BudgetScreen(onAddBudget = { onNavigateToAddBudget?.invoke() }, onNavigateToDetail = { cat -> onNavigateToBudgetDetail?.invoke(cat) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsTab(
    onAddExpense: (() -> Unit)? = null
) {
    // Collect analytics and transactions state to determine whether to show data
    val analyticsVm: AnalyticsViewModel = hiltViewModel()
    val categories by analyticsVm.categories.collectAsState()
    val totalSpent by analyticsVm.totalSpent.collectAsState()
    val totalBudget by analyticsVm.totalBudget.collectAsState()

    val homeVm: HomeViewModel = hiltViewModel()
    val recentTx by homeVm.recentTransactions.collectAsState()
    val hasSmsPermission by homeVm.hasSmsPermission.collectAsState()

    // Show empty analytics when there are no transactions
    val hasTransactions = remember(recentTx) { recentTx.isNotEmpty() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "Analytics") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!hasTransactions) {
                AnalyticsEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                    onAddExpense = { onAddExpense?.invoke() },
                    hasSmsPermission = hasSmsPermission,
                    onSmsGranted = { homeVm.onSmsPermissionResult(true); homeVm.refreshSmsPermission() }
                )
            } else {
                // Delegate to AnalyticsScreen which will also fallback to sample data for previews
                AnalyticsScreen(
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    categories = categories
                )
            }
        }
    }
}

@Composable
private fun EmptyAnalytics() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No analytics yet",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Add budgets or transactions to view analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTab() {
    val vm: com.example.truxpense.presentation.screens.dashboard.settings.SettingsViewModel = hiltViewModel()
    val username by vm.username.collectAsState(initial = null)
    val smsEnabled by vm.smsEnabled.collectAsState()
    val notificationsEnabled by vm.notificationsEnabled.collectAsState()

    // Render the SettingsScreen composable and wire events to ViewModel
    SettingsScreen(
        username = username ?: "",
        phone = "",
        smsEnabled = smsEnabled,
        notificationsEnabled = notificationsEnabled,
        onSmsToggle = { enabled -> vm.setSmsEnabled(enabled) },
        onNotificationsToggle = { enabled -> vm.setNotificationsEnabled(enabled) },
        onLogout = { vm.logout() }
    )
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
