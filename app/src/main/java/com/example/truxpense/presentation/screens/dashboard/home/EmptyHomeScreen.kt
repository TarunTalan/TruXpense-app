package com.example.truxpense.presentation.screens.dashboard.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.EmptyScreenContent
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.screens.dashboard.components.SmsPermissionBanner

@Composable
fun EmptyHomeContent(
    onAddExpense: (() -> Unit)? = null,
    hasSmsPermission: Boolean,
    onSmsGranted: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = {
            ScreenTopBar(
                headerTitle = "TruXpense", showBack = false, showProfileIcons = true
            )
        }) { innerPadding ->

        EmptyScreenContent(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            illustrationRes = R.drawable.illustration_home,
            title = "Start tracking your spending",
            subtitle = "We'll automatically track expenses from SMS, or you can add them manually.",
            ctaText = "Add your first expense",
            onCta = { onAddExpense?.invoke() },
            topBanner = if (!hasSmsPermission) {
                { SmsPermissionBanner(modifier = Modifier.fillMaxWidth(), onGranted = { onSmsGranted?.invoke() }) }
            } else null,
        )
    }
}