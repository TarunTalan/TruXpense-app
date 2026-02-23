package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.truxpense.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    showProfileIcons: Boolean = false,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (showBack && onBack != null) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_icon),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        actions = {
            if (actions != null) {
                actions()
            } else if (showProfileIcons && !showBack) {
                val isDark = isSystemInDarkTheme()
                IconButton(onClick = { /* profile */ }) {
                    Icon(
                        painter = painterResource(id = if (isDark) R.drawable.profile_dark_icon else R.drawable.profile_icon),
                        contentDescription = "Profile",
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                    )
                }
                IconButton(onClick = { /* notifications */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.notifications_icon),
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        windowInsets = WindowInsets.statusBars,
        modifier = modifier.fillMaxWidth(),
    )
}
