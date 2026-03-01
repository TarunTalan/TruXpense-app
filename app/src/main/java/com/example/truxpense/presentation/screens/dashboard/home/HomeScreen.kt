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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.truxpense.notification.NotificationDeepLink
import com.example.truxpense.notification.NotificationDeepLinkViewModel
import com.example.truxpense.presentation.navigation.BottomNavBarMenu
import com.example.truxpense.presentation.navigation.Screen
import com.example.truxpense.presentation.navigation.safeNavigate
import com.example.truxpense.presentation.screens.dashboard.addexpense.AddExpenseScreen
import com.example.truxpense.presentation.screens.dashboard.analytics.AnalyticsEmptyScreen
import com.example.truxpense.presentation.screens.dashboard.analytics.AnalyticsScreen
import com.example.truxpense.presentation.screens.dashboard.budget.AddBudgetScreen
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetDetailScreen
import com.example.truxpense.presentation.screens.dashboard.budget.BudgetTab
import com.example.truxpense.presentation.screens.dashboard.components.DashboardBottomBar
import com.example.truxpense.presentation.screens.dashboard.components.SmsPermissionBanner
import com.example.truxpense.presentation.screens.dashboard.settings.SettingsScreen
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens
import com.example.truxpense.presentation.screens.dashboard.transaction.EditExpenseScreen
import com.example.truxpense.presentation.screens.dashboard.transaction.TransactionDetailScreen
import com.example.truxpense.presentation.screens.dashboard.transaction.TransactionsScreen
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationScreen
import com.example.truxpense.notification.NotificationSettingsScreen

// Dashboard shell: owns the NavController and tab routing

@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
) {
    val vm = hiltViewModel<HomeViewModel>()
    val deepLinkVm = hiltViewModel<NotificationDeepLinkViewModel>()

    // Single NavController for the dashboard
    val dashboardNavController = rememberNavController()
    val navBackStackEntry by dashboardNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // ── Notification deep-link handler ─────────────────────────────────────────
    // Collects deep links emitted by NotificationDeepLinkManager (via MainActivity)
    // and navigates to the appropriate screen.  consume() clears the replay slot so
    // the same destination is not re-navigated on recomposition.
    LaunchedEffect(deepLinkVm) {
        deepLinkVm.pendingDeepLink.collect { link ->
            when (link) {
                is NotificationDeepLink.AddExpense -> {
                    dashboardNavController.safeNavigate(Screen.Dashboard.Home.AddExpense)
                }

                is NotificationDeepLink.BudgetTab -> {
                    dashboardNavController.safeNavigate(Screen.Dashboard.Budget.Root) {
                        popUpTo(dashboardNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                is NotificationDeepLink.BudgetDetail -> {
                    // Navigate to Budget tab root; the user taps the highlighted category.
                    // We don't carry limit/spent in the notification extra, so we can't
                    // deep-link straight to BudgetDetailScreen without a repo look-up.
                    // That look-up is done here so the deep link resolves correctly.
                    dashboardNavController.safeNavigate(Screen.Dashboard.Budget.Root) {
                        popUpTo(dashboardNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    // Signal the Budget tab to highlight the correct category
                    try {
                        dashboardNavController.getBackStackEntry(Screen.Dashboard.Budget.Root).savedStateHandle.set(
                            "highlightCategory", link.category
                        )
                    } catch (_: Exception) { /* backstack entry not yet available */
                    }
                }

                is NotificationDeepLink.Analytics -> {
                    dashboardNavController.safeNavigate(Screen.Dashboard.Analytics.Root) {
                        popUpTo(dashboardNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                is NotificationDeepLink.Transactions -> {
                    dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                        popUpTo(dashboardNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                is NotificationDeepLink.Home -> {
                    dashboardNavController.safeNavigate(Screen.Dashboard.Home.Root) {
                        popUpTo(dashboardNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            deepLinkVm.consume()
        }
    }

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

    val isTopLevelDestination by remember(currentDestination) {
        derivedStateOf { currentDestination?.route in topLevelRoutes }
    }

    fun isTabSelected(tab: BottomNavBarMenu): Boolean =
        currentDestination?.hierarchy?.any { it.route == tab.route } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.fillMaxWidth().height(DashboardDimens.bottomNavHeight))

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
                        vm = vm,
                        onAddExpense = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Home.AddExpense)
                        },
                        onNavigateToBudget = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Budget.Root) {
                                popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onViewAll = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                                popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNotificationsClick = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Notifications.Root)
                        },
                    )
                }
            }

            composable(Screen.Dashboard.Home.AddExpense) {
                AddExpenseScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onSave = { _ ->
                        dashboardNavController.safeNavigate(Screen.Dashboard.Home.Root) {
                            popUpTo(Screen.Dashboard.Root) { inclusive = false }
                        }
                    },
                )
            }

            // Transactions tab
            composable(Screen.Dashboard.Transactions.Root) { backStackEntry ->
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    TransactionsScreen(
                        navBackStackEntry = backStackEntry,
                        onTransactionClick = { transactionId ->
                            dashboardNavController.safeNavigate(
                                Screen.Dashboard.Transactions.detailRoute(transactionId)
                            )
                        },
                        onAddTransaction = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Home.AddExpense)
                        },
                    )
                }
            }

            composable(
                route = Screen.Dashboard.Transactions.Detail,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                TransactionDetailScreen(
                    transactionId = transactionId,
                    onBack = { dashboardNavController.popBackStack() },
                    onEdit = {
                        dashboardNavController.safeNavigate(
                            Screen.Dashboard.Transactions.editRoute(transactionId)
                        )
                    },
                    onDeleted = {
                        dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                            popUpTo(Screen.Dashboard.Transactions.Root) { inclusive = false }
                        }
                    },
                )
            }

            composable(
                route = Screen.Dashboard.Transactions.Edit,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                EditExpenseScreen(
                    transactionId = transactionId,
                    onCancel = { dashboardNavController.popBackStack() },
                    onSaved = {
                        dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                            popUpTo(Screen.Dashboard.Transactions.Root) { inclusive = false }
                        }
                    },
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

            composable(
                route = Screen.Dashboard.Budget.Detail,
                arguments = listOf(
                    navArgument("budgetName") { type = NavType.StringType },
                    navArgument("monthlyLimit") { type = NavType.FloatType },
                    navArgument("spent") { type = NavType.FloatType },
                ),
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val name = java.net.URLDecoder.decode(args?.getString("budgetName") ?: "Budget", "UTF-8")
                val limit = args?.getFloat("monthlyLimit")?.toDouble() ?: 0.0
                val spent = args?.getFloat("spent")?.toDouble() ?: 0.0

                BudgetDetailScreen(
                    budgetName = name,
                    monthlyLimit = limit,
                    spent = spent,
                    onBack = { dashboardNavController.popBackStack() },
                    onDeleted = { dashboardNavController.popBackStack() },
                    onSeeAll = { category ->
                        val cat = category?.trim()
                        dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                            popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        if (!cat.isNullOrBlank()) {
                            try {
                                dashboardNavController.getBackStackEntry(Screen.Dashboard.Transactions.Root).savedStateHandle.set(
                                    "preselectCategory", cat
                                )
                            } catch (_: Exception) {
                                dashboardNavController.currentBackStackEntry?.savedStateHandle?.set(
                                    "preselectCategory", cat
                                )
                            }
                        }
                    },
                    onTransactionClick = { txId ->
                        dashboardNavController.safeNavigate(
                            Screen.Dashboard.Transactions.detailRoute(txId)
                        )
                    },
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
                    onSave = { _ ->
                        dashboardNavController.safeNavigate(Screen.Dashboard.Home.Root) {
                            popUpTo(Screen.Dashboard.Root) { inclusive = false }
                        }
                    },
                )
            }

            // Settings tab
            composable(Screen.Dashboard.Settings.Root) {
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    SettingsTab(
                        onLogout = onLogout,
                        onNotificationsClick = {
                            dashboardNavController.safeNavigate(
                                Screen.Dashboard.Settings.NotificationSettings
                            )
                        },
                    )
                }
            }

            // Notification settings sub-screen (from Settings → Notifications row)
            composable(Screen.Dashboard.Settings.NotificationSettings) {
                NotificationSettingsScreen(
                    onBack = { dashboardNavController.popBackStack() },
                )
            }

            // Notifications screen (full-screen, no bottom bar)
            composable(Screen.Dashboard.Notifications.Root) {
                NotificationScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onNavigateToBudgetDetail = { budgetName, monthlyLimit, spent ->
                        dashboardNavController.safeNavigate(
                            Screen.Dashboard.Budget.detailRoute(budgetName, monthlyLimit, spent)
                        )
                    },
                    onNavigateToWeeklyAnalytics = {
                        dashboardNavController.safeNavigate(Screen.Dashboard.Analytics.Root) {
                            popUpTo(dashboardNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTransactionDetail = { transactionId ->
                        dashboardNavController.safeNavigate(
                            Screen.Dashboard.Transactions.detailRoute(transactionId)
                        )
                    },
                    onNavigateToAddExpense = {
                        dashboardNavController.safeNavigate(Screen.Dashboard.Home.AddExpense)
                    },
                    onNavigateToTransactions = {
                        dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                            popUpTo(dashboardNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }

    SmsPermissionDialogHandler(vm = vm)
}


@Composable
private fun AnalyticsTab(onAddExpense: () -> Unit = {}) {
    val homeVm: HomeViewModel = hiltViewModel()
    val recentTx by homeVm.recentTransactions.collectAsState()

    if (recentTx.isEmpty()) {
        AnalyticsEmptyScreen(
            modifier = Modifier.fillMaxSize(),
            onAddExpense = onAddExpense,
        )
    } else {
        AnalyticsScreen()
    }
}

@Composable
private fun SettingsTab(
    onLogout: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
) {
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
        onNotificationsClick = onNotificationsClick,
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

    LaunchedEffect(vm) {
        vm.requestSmsPermission.collect { permissionLauncher.launch(Manifest.permission.READ_SMS) }
    }

    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { },
            shape = RoundedCornerShape(DashboardDimens.cornerCard),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Permission blocked", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Text(
                    "SMS permission has been permanently denied. Open app settings to grant access.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancel", color = MaterialTheme.colorScheme.primary) }
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
    MaterialTheme {
        DashboardBottomBar(
            items = BottomNavBarMenu.all,
            isSelected = { it == BottomNavBarMenu.Home },
            onItemSelected = {},
        )
    }
}