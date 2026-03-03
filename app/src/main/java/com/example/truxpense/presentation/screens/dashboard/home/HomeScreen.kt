package com.example.truxpense.presentation.screens.dashboard.home

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.truxpense.notification.deeplink.NotificationDeepLink
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
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationDeepLinkViewModel
import com.example.truxpense.presentation.screens.dashboard.notifications.NotificationScreen
import com.example.truxpense.presentation.screens.dashboard.settings.*
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.screens.dashboard.transaction.EditExpenseScreen
import com.example.truxpense.presentation.screens.dashboard.transaction.TransactionDetailScreen
import com.example.truxpense.presentation.screens.dashboard.transaction.TransactionsScreen
import androidx.core.net.toUri

// Dashboard shell: owns the NavController and tab routing

@SuppressLint("UseKtx")
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
) {
    val vm = hiltViewModel<HomeViewModel>()
    val deepLinkVm = hiltViewModel<NotificationDeepLinkViewModel>()

    val dashboardNavController = rememberNavController()
    val navBackStackEntry by dashboardNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // ── Notification deep-link handler ─────────────────────────────────────────
    LaunchedEffect(deepLinkVm) {
        deepLinkVm.pendingDeepLink.collect { link ->
            when (link) {
                is NotificationDeepLink.AddExpense -> dashboardNavController.safeNavigate(Screen.Dashboard.Home.AddExpense)

                is NotificationDeepLink.BudgetTab -> dashboardNavController.safeNavigate(Screen.Dashboard.Budget.Root) {
                    popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }

                is NotificationDeepLink.BudgetDetail -> {
                    dashboardNavController.safeNavigate(Screen.Dashboard.Budget.Root) {
                        popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                    try {
                        dashboardNavController.getBackStackEntry(Screen.Dashboard.Budget.Root).savedStateHandle.set(
                            "highlightCategory",
                            link.category
                        )
                    } catch (_: Exception) {
                    }
                }

                is NotificationDeepLink.Analytics -> dashboardNavController.safeNavigate(Screen.Dashboard.Analytics.Root) {
                    popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }

                is NotificationDeepLink.Transactions -> dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                    popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }

                is NotificationDeepLink.Home -> dashboardNavController.safeNavigate(Screen.Dashboard.Home.Root) {
                    popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            }
            deepLinkVm.consume()
        }
    }

    // Top-level tab roots (these show the bottom bar)
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
        contentWindowInsets = WindowInsets(0.dp),
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
                                launchSingleTop = true; restoreState = true
                            }
                        },
                    )
                }
            }
        },
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

            // ══════════════════════════════════════════════════════════════════
            // HOME TAB
            // ══════════════════════════════════════════════════════════════════
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
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        onViewAll = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Transactions.Root) {
                                popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        onNotificationsClick = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Notifications.Root)
                        },
                        onProfileClick = {
                            dashboardNavController.safeNavigate(Screen.Dashboard.Settings.PersonalInfo)
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

            // ══════════════════════════════════════════════════════════════════
            // TRANSACTIONS TAB
            // ══════════════════════════════════════════════════════════════════
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

            // ══════════════════════════════════════════════════════════════════
            // BUDGET TAB
            // ══════════════════════════════════════════════════════════════════
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
                            launchSingleTop = true; restoreState = true
                        }
                        if (!cat.isNullOrBlank()) {
                            try {
                                dashboardNavController.getBackStackEntry(Screen.Dashboard.Transactions.Root).savedStateHandle.set(
                                    "preselectCategory",
                                    cat
                                )
                            } catch (_: Exception) {
                                dashboardNavController.currentBackStackEntry?.savedStateHandle?.set(
                                    "preselectCategory",
                                    cat
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

            // ══════════════════════════════════════════════════════════════════
            // ANALYTICS TAB
            // ══════════════════════════════════════════════════════════════════
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

            // ══════════════════════════════════════════════════════════════════
            // SETTINGS TAB  (root)
            // ══════════════════════════════════════════════════════════════════
            composable(Screen.Dashboard.Settings.Root) {
                Box(Modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                    SettingsTab(
                        navController = dashboardNavController,
                        onLogout = onLogout,
                    )
                }
            }

            // ── Settings → Personal Info ──────────────────────────────────────
            composable(Screen.Dashboard.Settings.PersonalInfo) {
                val settingsVm: SettingsViewModel = hiltViewModel()
                val username by settingsVm.username.collectAsStateWithLifecycle(initialValue = "")
                val phone by settingsVm.phone.collectAsStateWithLifecycle(initialValue = "")
                val isSaving by settingsVm.isSavingProfile.collectAsStateWithLifecycle()

                PersonalInfoScreen(
                    initialUsername = username ?: "",
                    initialPhone = phone ?: "",
                    isSaving = isSaving,
                    onBack = { dashboardNavController.popBackStack() },
                    onSave = { name, ph ->
                        settingsVm.saveProfile(name, ph) {
                            dashboardNavController.popBackStack()
                        }
                    },
                )
            }

            // ── Settings → Linked Accounts ────────────────────────────────────
            composable(Screen.Dashboard.Settings.LinkedAccounts) {
                val settingsVm: SettingsViewModel = hiltViewModel()
                val linkedAccounts by settingsVm.linkedAccounts.collectAsStateWithLifecycle()
                val smsEnabled by settingsVm.smsEnabled.collectAsStateWithLifecycle()

                LinkedAccountsScreen(
                    accounts = linkedAccounts,
                    smsPermissionGranted = smsEnabled,
                    onBack = { dashboardNavController.popBackStack() },
                    onAddAccount = { /* TODO: launch bank-linking flow */ },
                    onRemoveAccount = settingsVm::removeLinkedAccount,
                )
            }

            // ── Settings → Security ───────────────────────────────────────────
            composable(Screen.Dashboard.Settings.Security) {
                val settingsVm: SettingsViewModel = hiltViewModel()
                val biometrics by settingsVm.biometricsEnabled.collectAsStateWithLifecycle()
                val appLock by settingsVm.appLockEnabled.collectAsStateWithLifecycle()

                SecurityScreen(
                    biometricsAvailable = true, // TODO: query BiometricManager
                    biometricsEnabled = biometrics,
                    appLockEnabled = appLock,
                    onBack = { dashboardNavController.popBackStack() },
                    onBiometricsToggle = settingsVm::setBiometricsEnabled,
                    onAppLockToggle = settingsVm::setAppLockEnabled,
                    onChangePassword = settingsVm::changePassword,
                )
            }

            // ── Settings → Notifications & Reminders ──────────────────────────
            composable(Screen.Dashboard.Settings.Notifications) {
                val settingsVm: SettingsViewModel = hiltViewModel()
                val notifPrefs by settingsVm.notifPrefs.collectAsStateWithLifecycle()

                NotificationsScreen(
                    prefs = notifPrefs,
                    onBack = { dashboardNavController.popBackStack() },
                    onPrefsChanged = settingsVm::updateNotifPrefs,
                )
            }

            // ── Settings → Help & Support ─────────────────────────────────────
            composable(Screen.Dashboard.Settings.Help) {
                val context = LocalContext.current
                HelpSupportScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onEmailSupport = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:support@truxpense.app".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "TruXpense Support Request")
                        }
                        context.startActivity(intent)
                    },
                )
            }

            // ── Settings → Privacy Policy ─────────────────────────────────────
            composable(Screen.Dashboard.Settings.PrivacyPolicy) {
                PrivacyPolicyScreen(onBack = { dashboardNavController.popBackStack() })
            }

            // ── Settings → Terms of Service ───────────────────────────────────
            composable(Screen.Dashboard.Settings.Terms) {
                TermsScreen(onBack = { dashboardNavController.popBackStack() })
            }

            // ── Settings → About TruXpense ────────────────────────────────────
            composable(Screen.Dashboard.Settings.About) {
                val context = LocalContext.current
                AboutScreen(
                    onBack = { dashboardNavController.popBackStack() },
                    onRateApp = {
                        val intent = Intent(
                            Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onViewLicences = { /* TODO: OssLicensesMenuActivity */ },
                )
            }

            // ── Settings → Delete Account ─────────────────────────────────────
            composable(Screen.Dashboard.Settings.DeleteAccount) {
                val settingsVm: SettingsViewModel = hiltViewModel()
                val username by settingsVm.username.collectAsStateWithLifecycle(initialValue = "")
                val isDeleting by settingsVm.isDeletingAccount.collectAsStateWithLifecycle()

                DeleteAccountScreen(
                    username = username ?: "",
                    isDeleting = isDeleting,
                    onBack = { dashboardNavController.popBackStack() },
                    onConfirmDelete = {
                        settingsVm.deleteAccount {
                            // Pop all the way back to login/intro via the outer nav
                            onLogout()
                        }
                    },
                )
            }

            // ══════════════════════════════════════════════════════════════════
            // NOTIFICATIONS (full-screen, no bottom bar)
            // ══════════════════════════════════════════════════════════════════
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
                            popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
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
                            popUpTo(dashboardNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                )
            }
        }
    }

    SmsPermissionDialogHandler(vm = vm)
}

// ══════════════════════════════════════════════════════════════════════════════
// PRIVATE TAB COMPOSABLES
// ══════════════════════════════════════════════════════════════════════════════

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

/**
 * Settings tab — now a simple wrapper that wires the SettingsScreen callbacks
 * to routes in the parent [navController]. No nested NavHost needed.
 */
@Composable
private fun SettingsTab(
    navController: androidx.navigation.NavHostController,
    onLogout: () -> Unit,
) {
    val vm: SettingsViewModel = hiltViewModel()
    val username by vm.username.collectAsStateWithLifecycle(initialValue = null)
    val phone by vm.phone.collectAsStateWithLifecycle(initialValue = null)

    SettingsScreen(
        username = username ?: "",
        phone = phone ?: "",
        onPersonalInfo = { navController.safeNavigate(Screen.Dashboard.Settings.PersonalInfo) },
        onLinkedAccounts = { navController.safeNavigate(Screen.Dashboard.Settings.LinkedAccounts) },
        onSecurity = { navController.safeNavigate(Screen.Dashboard.Settings.Security) },
        onNotifications = { navController.safeNavigate(Screen.Dashboard.Settings.Notifications) },
        onHelp = { navController.safeNavigate(Screen.Dashboard.Settings.Help) },
        onPrivacyPolicy = { navController.safeNavigate(Screen.Dashboard.Settings.PrivacyPolicy) },
        onTerms = { navController.safeNavigate(Screen.Dashboard.Settings.Terms) },
        onAbout = { navController.safeNavigate(Screen.Dashboard.Settings.About) },
        onDeleteAccount = { navController.safeNavigate(Screen.Dashboard.Settings.DeleteAccount) },
        onLogout = { vm.logout(); onLogout() },
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SMS PERMISSION DIALOG
// ══════════════════════════════════════════════════════════════════════════════

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
                TextButton(onClick = { }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ══════════════════════════════════════════════════════════════════════════════

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