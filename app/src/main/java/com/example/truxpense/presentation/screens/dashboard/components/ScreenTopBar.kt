package com.example.truxpense.presentation.screens.dashboard.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    headerTitle: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    showProfileIcons: Boolean = false,
    onNotificationsClick: (() -> Unit)? = null,
    unreadCount: Int = 0,
) {
    TopAppBar(
        title = {
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
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
                // show an initial badge instead of a profile icon.
                val namePart = remember(headerTitle) {
                    headerTitle.split(',').getOrNull(1)?.trim()?.ifEmpty { headerTitle } ?: headerTitle
                }
                val initial = remember(namePart) {
                    namePart.split(' ').firstOrNull()?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                }

                val bgColor = remember(namePart) {
                    val hash = namePart.hashCode()
                    val hue = (hash and 0xFFFF) % 360f
                    val hsv = floatArrayOf(hue, 0.55f, 0.85f)
                    Color(AndroidColor.HSVToColor(hsv))
                }

                val contentColor = if (bgColor.luminance() < 0.5f) Color.White else Color.Black

                IconButton(onClick = { /* profile */ }) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color = bgColor, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            color = contentColor,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)
                        )
                    }
                }

                // Show a badge when unreadCount > 0
                if (unreadCount > 0) {
                    BadgedBox(badge = { Badge { /* dot */ } }) {
                        IconButton(onClick = { onNotificationsClick?.invoke() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.notifications_icon),
                                contentDescription = "Notifications",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { onNotificationsClick?.invoke() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.notifications_icon),
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
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
