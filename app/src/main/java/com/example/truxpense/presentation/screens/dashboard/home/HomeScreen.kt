package com.example.truxpense.presentation.screens.dashboard.home

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.truxpense.presentation.navigation.BottomNavBarMenu
import com.example.truxpense.presentation.navigation.Screen
import com.example.truxpense.presentation.navigation.safeNavigate
import com.example.truxpense.presentation.screens.dashboard.addexpense.AddExpenseScreen
import com.example.truxpense.presentation.screens.dashboard.addexpense.AddExpenseScreenResultant
import com.example.truxpense.presentation.screens.dashboard.analytic.AnalyticsEmptyScreen
import com.example.truxpense.presentation.screens.dashboard.analytic.AnalyticsScreen
import com.example.truxpense.presentation.screens.dashboard.analytic.AnalyticsViewModel
import com.example.truxpense.presentation.screens.dashboard.budget.AddBudgetScreen
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetDetailScreen
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetTab
import com.example.truxpense.presentation.screens.dashboard.components.BOTTOM_NAV_BAR_HEIGHT
import com.example.truxpense.presentation.screens.dashboard.components.DashboardBottomBar
import com.example.truxpense.presentation.screens.dashboard.components.DashboardTopBar
import com.example.truxpense.presentation.screens.dashboard.components.SmsPermissionBanner
import com.example.truxpense.presentation.screens.dashboard.settings.SettingsScreen
import com.example.truxpense.presentation.screens.dashboard.transaction.TransactionsEmptyScreen

// Dashboard shell: owns the NavController and tab routing

@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
) {
    val vm = hiltViewModel<HomeViewModel>()
    val username by vm.username.collectAsState(initial = null)

    // Single NavController for the dashboard
    val dashboardNavController = rememberNavController()
    val navBackStackEntry by dashboardNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Top-level tab roots (these show the bottom bar). Sub-screens hide it.
    val topLevelRoutes = remember {
        setOf(
            Screen.Dashboard.Home.Root,
            Screen.Dashboard.Transactions.Root,
            Screen.Dashboard.Budget.Root,
            Screen.Dashboard.Analytics.Root,
            Screen.Dashboard.Settings.Root,
        )
    }

    // True only when a tab root is active; false for all sub-screens.
    val isTopLevelDestination by remember(currentDestination) {
        derivedStateOf { currentDestination?.route in topLevelRoutes }
    }

    // Resolve the active tab root so DashboardBottomBar can highlight the correct item.
    // We match on the hierarchy so that sub-screens (e.g. budget/detail) still highlight Budget.
    fun isTabSelected(tab: BottomNavBarMenu): Boolean =
        currentDestination?.hierarchy?.any { it.route == tab.route } == true

    // Scaffold with persistent bottom-bar spacer and NavHost padding strategy
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Keep a fixed-height spacer as layout anchor; overlay the real bar visually.
            Box(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.fillMaxWidth().height(BOTTOM_NAV_BAR_HEIGHT))

                AnimatedVisibility(visible = isTopLevelDestination, enter = fadeIn(), exit = ExitTransition.None) {
                    DashboardBottomBar(
                        items = BottomNavBarMenu.all,
                        isSelected = ::isTabSelected,
                        onItemSelected = { tab ->
                            dashboardNavController.navigate(tab.route) {
                                popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->

        // Padding strategy: capture bottom padding once and pass to tab roots
        val bottomBarPadding = innerPadding.calculateBottomPadding()

        NavHost(
            navController = dashboardNavController,
            startDestination = Screen.Dashboard.Home.Root,
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {

            // Home tab
            composable(Screen.Dashboard.Home.Root) {
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    HomeTabScreen(
                        username = username,
                        vm = vm,
                        onLogout = onLogout,
                        onAddExpense = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Home.AddExpense)
                        },
                    )
                }
            }

            composable(Screen.Dashboard.Home.AddExpense) {
                AddExpenseScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onSave = { tx: HomeTransactionItem ->
                        // save tx into savedStateHandle and navigate to result screen
                        dashboardNavController.currentBackStackEntry?.savedStateHandle?.set<HomeTransactionItem>("new_tx", tx)
                        dashboardNavController.safeNavigate(Screen.Dashboard.Home.AddExpenseResult)
                    },
                    onCancel = { dashboardNavController.popBackStack() },
                )
            }

            // Result screen — shows confirmation + details, then routes back to Home and informs HomeViewModel
            composable(Screen.Dashboard.Home.AddExpenseResult) {
                val homeVm: HomeViewModel = hiltViewModel()
                val saved = dashboardNavController.previousBackStackEntry?.savedStateHandle
                val tx: HomeTransactionItem? = saved?.get<HomeTransactionItem>("new_tx")
                AddExpenseScreenResultant(
                     tx = tx,
                     onDone = {
                        if (tx != null) homeVm.addTransaction(tx)
                        // remove saved data
                        saved?.remove<HomeTransactionItem>("new_tx")
                        // pop back to home root
                        dashboardNavController.popBackStack(Screen.Dashboard.Home.Root, inclusive = false)
                    }
                )
            }

            // Transactions tab
            composable(Screen.Dashboard.Transactions.Root) {
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    TransactionsTab(
                        onAddExpense = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.AddExpense)
                        },
                    )
                }
            }

            composable(Screen.Dashboard.Transactions.AddExpense) {
                AddExpenseScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onSave = { _: HomeTransactionItem -> dashboardNavController.popBackStack() },
                    onCancel = { dashboardNavController.popBackStack() },
                )
            }

            // Budget tab
            composable(Screen.Dashboard.Budget.Root) {
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    BudgetTab(
                        onNavigateToAddBudget = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Budget.Add)
                        },
                        onNavigateToBudgetDetail = { cat ->
                            dashboardNavController.safeNavigate(
                                Screen.Dashboard.Budget.detailRoute(
                                    budgetName = cat.name,
                                    monthlyLimit = cat.total.toDouble(),
                                    spent = cat.spent.toDouble(),
                                )
                            )
                        },
                    )
                }
            }

            composable(Screen.Dashboard.Budget.Add) {
                AddBudgetScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onSave = { dashboardNavController.popBackStack() },
                )
            }

            // BudgetDetail — args carried as URL segments, no SavedStateHandle needed
            composable(
                route = Screen.Dashboard.Budget.Detail,
                arguments = listOf(
                    navArgument("budgetName") { type = NavType.StringType },
                    navArgument("monthlyLimit") { type = NavType.FloatType },
                    navArgument("spent") { type = NavType.FloatType },
                ),
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                // Percent-decode the name since we URL-encoded it when navigating
                val name = java.net.URLDecoder.decode(
                    args?.getString("budgetName") ?: "Budget", "UTF-8"
                )
                val limit = args?.getFloat("monthlyLimit")?.toDouble() ?: 0.0
                val spent = args?.getFloat("spent")?.toDouble() ?: 0.0

                BudgetDetailScreen(
                    budgetName = name,
                    monthlyLimit = limit,
                    spent = spent,
                    onBack = { dashboardNavController.popBackStack() },
                )
            }

            // Analytics tab
            composable(Screen.Dashboard.Analytics.Root) {
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    AnalyticsTab(
                        onAddExpense = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Analytics.AddExpense)
                        },
                    )
                }
            }

            composable(Screen.Dashboard.Analytics.AddExpense) {
                AddExpenseScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onSave = { _: HomeTransactionItem -> dashboardNavController.popBackStack() },
                    onCancel = { dashboardNavController.popBackStack() },
                )
            }

            // Settings tab
            composable(Screen.Dashboard.Settings.Root) {
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    SettingsTab(onLogout = onLogout)
                }
            }
        }
    }

    SmsPermissionDialogHandler(vm = vm)
}


@Composable
private fun TransactionsTab(onAddExpense: () -> Unit = {}) {
    val homeVm: HomeViewModel = hiltViewModel()
    val recentTx by homeVm.recentTransactions.collectAsState()

    if (recentTx.isEmpty()) {
        TransactionsEmptyScreen(
            modifier = Modifier.fillMaxSize(),
            onAddTransaction = onAddExpense,
        )
    } else {
        // Real transactions list goes here once the Transactions feature is built
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}


@Composable
private fun AnalyticsTab(onAddExpense: () -> Unit = {}) {
    val analyticsVm: AnalyticsViewModel = hiltViewModel()
    val categories by analyticsVm.categories.collectAsState()
    val totalSpent by analyticsVm.totalSpent.collectAsState()
    val totalBudget by analyticsVm.totalBudget.collectAsState()

    val homeVm: HomeViewModel = hiltViewModel()
    val recentTx by homeVm.recentTransactions.collectAsState()

    if (recentTx.isEmpty()) {
        AnalyticsEmptyScreen(
            modifier = Modifier.fillMaxSize(),
            onAddExpense = onAddExpense,
        )
    } else {
        AnalyticsScreen(
            totalSpent = totalSpent,
            totalBudget = totalBudget,
            categories = categories,
        )
    }
}

@Composable
private fun SettingsTab(onLogout: () -> Unit = {}) {
    val vm: com.example.truxpense.presentation.screens.dashboard.settings.SettingsViewModel = hiltViewModel()
    val username by vm.username.collectAsState(initial = null)
    val smsEnabled by vm.smsEnabled.collectAsState()
    val notificationsEnabled by vm.notificationsEnabled.collectAsState()

    SettingsScreen(
        username = username ?: "",
        phone = "",
        smsEnabled = smsEnabled,
        notificationsEnabled = notificationsEnabled,
        onSmsToggle = { vm.setSmsEnabled(it) },
        onNotificationsToggle = { vm.setNotificationsEnabled(it) },
        onLogout = { vm.logout(); onLogout() },
    )
}

@Composable
private fun SmsPermissionDialogHandler(vm: HomeViewModel) {
    val context = LocalContext.current
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        vm.onSmsPermissionResult(granted)
        if (!granted) {
            val activity = context as? android.app.Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_SMS)
            } ?: false
            if (!showRationale) showPermanentlyDeniedDialog = true
        }
    }

    // Collect one-shot events emitted by the VM when any child calls emitRequestSmsPermission()
    LaunchedEffect(vm) {
        vm.requestSmsPermission.collect { permissionLauncher.launch(Manifest.permission.READ_SMS) }
    }

    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Permission blocked") },
            text = { Text("SMS permission has been permanently denied. Open app settings to grant access.") },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancel") }
            },
        )
    }
}


// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmsPermissionBannerPreview() {
    MaterialTheme { SmsPermissionBanner() }
}

@Preview(showBackground = true)
@Composable
private fun DashboardBottomBarPreview() {
    val items = BottomNavBarMenu.all
    MaterialTheme {
        DashboardBottomBar(
            items = items,
            isSelected = { it == BottomNavBarMenu.Home },
            onItemSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardTopBarPreview() {
    DashboardTopBar(username = "Tarun")
}