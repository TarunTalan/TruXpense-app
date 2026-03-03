package com.example.truxpense

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.truxpense.data.session.AuthSessionManager
import com.example.truxpense.notification.deeplink.NotificationDeepLinkManager
import com.example.truxpense.presentation.navigation.AppNavHost
import com.example.truxpense.presentation.navigation.Screen
import com.example.truxpense.presentation.navigation.safeNavigate
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.data.sms.SmsPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Volatile
    private var keepSplashOn = true

    @Inject
    lateinit var sessionManager: AuthSessionManager

    @Inject
    lateinit var deepLinkManager: NotificationDeepLinkManager

    // ── Permission launchers ──────────────────────────────────────────────
    // Must be registered before onCreate per the Activity Result API contract.

    /** Android 13+ POST_NOTIFICATIONS runtime permission. */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
        }


    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val receiveSms = grants[android.Manifest.permission.RECEIVE_SMS] ?: false
            val readSms    = grants[android.Manifest.permission.READ_SMS]    ?: false
            Log.d(TAG, "SMS permissions — RECEIVE_SMS=$receiveSms  READ_SMS=$readSms")

            if (!receiveSms) {
                Log.w(TAG, "RECEIVE_SMS denied — real-time SMS parsing disabled. " +
                        "Show UpsellBanner on HomeScreen to re-request.")
            }
            if (!readSms) {
                Log.w(TAG, "READ_SMS denied — Re-scan History (S-05) will be disabled.")
            }
        }

    // ─────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        val isColdStart = savedInstanceState == null

        val splashScreen: SplashScreen = installSplashScreen()

        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate().alpha(0f).setDuration(120).withEndAction {
                provider.remove()
            }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Handle notification deep-link on cold start ────────────────────
        deepLinkManager.handle(intent)

        // ── Request POST_NOTIFICATIONS (Android 13+) ──────────────────────
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!alreadyGranted) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            val enabled = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()
            if (!enabled) {
                Log.w(TAG, "Notifications disabled in system settings (API < 33).")
            }
        }

        // ── Request SMS permissions ───────────────────────────────────────
        // Request only if not already granted to avoid redundant dialogs on every launch.
        if (!SmsPermissionHelper.hasAllSmsPermissions(this)) {
            // shouldShowRequestPermissionRationale → true means user previously denied.
            // In that case navigate to SmsPermissionRationaleScreen instead of requesting directly.
            val showRationale = SmsPermissionHelper.ALL_SMS_PERMISSIONS.any {
                shouldShowRequestPermissionRationale(it)
            }
            if (showRationale) {
                // User denied before — don't nag; let the HomeScreen banner guide them.
                Log.d(TAG, "SMS permission rationale needed — deferring to HomeScreen banner")
            } else {
                smsPermissionLauncher.launch(SmsPermissionHelper.ALL_SMS_PERMISSIONS)
            }
        } else {
            Log.d(TAG, "SMS permissions already granted")
        }

        // ── Splash screen keep-alive ──────────────────────────────────────
        splashScreen.setKeepOnScreenCondition { isColdStart && keepSplashOn }

        Handler(Looper.getMainLooper()).postDelayed({
            Log.w(TAG, "Splash fallback timeout — dismissing system splash")
            keepSplashOn = false
        }, 5000)

        // ── Compose UI ────────────────────────────────────────────────────
        setContent {
            TruXpenseTheme(darkTheme = isSystemInDarkTheme()) {
                val navController = androidx.navigation.compose.rememberNavController()

                Scaffold(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding: PaddingValues ->
                    val layoutDirection = LocalLayoutDirection.current

                    val contentPadding = PaddingValues(
                        start  = innerPadding.calculateLeftPadding(layoutDirection) + 16.dp,
                        top    = 10.dp,
                        end    = innerPadding.calculateRightPadding(layoutDirection) + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 10.dp
                    )

                    LaunchedEffect(sessionManager) {
                        sessionManager.logoutEvents.collect {
                            navController.safeNavigate(Screen.Intro) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        }
                    }

                    AppNavHost(
                        navController      = navController,
                        startDestination   = Screen.Splash,
                        contentPadding     = contentPadding,
                        onSplashEnter      = {
                            Log.d(TAG, "onSplashEnter — dismissing system splash")
                            keepSplashOn = false
                        }
                    )
                }
            }
        }

        // Dismiss splash as soon as the first frame is ready to draw
        try {
            val decorView = window.decorView
            val listener = object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                    Log.d(TAG, "decorView pre-draw — dismissing system splash")
                    keepSplashOn = false
                    return true
                }
            }
            decorView.viewTreeObserver.addOnPreDrawListener(listener)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to add decorView pre-draw listener: ${t.message}")
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkManager.handle(intent)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}