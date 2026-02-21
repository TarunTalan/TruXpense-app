package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.truxpense.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    username: String? = null,
    title: String? = null,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title ?: if (username.isNullOrBlank()) "Hi, there!" else "Hi, $username",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 10.dp)
            )
        },
        navigationIcon = {
            if (showBack && onBack != null) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_icon),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = {
            if (!showBack) {
                val isDark = isSystemInDarkTheme()
                IconButton(onClick = { /* TODO: profile */ }) {
                    Icon(
                        painter = painterResource(id = if (isDark) R.drawable.profile_dark_icon else R.drawable.profile_icon),
                        contentDescription = "Profile",
                        tint = Color.Unspecified
                    )
                }
                IconButton(onClick = { /* TODO: notifications */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.notifications_icon),
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        windowInsets = WindowInsets.statusBars,
        modifier = Modifier.fillMaxWidth()
    )
}
