package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import android.graphics.Color as AndroidColor

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
    onProfileClick: (() -> Unit)? = null,
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
                if (unreadCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                modifier = Modifier.size(14.dp).offset(x = (-12).dp, y = 9.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                            ) { /* dot */ }
                        },
                    ) {
                        IconButton(onClick = { onNotificationsClick?.invoke() }) {
                            Box(
                                modifier = Modifier.size(38.dp).border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                // blurred background layer (behind the icon)
                                Box(
                                    modifier = Modifier.matchParentSize().background(
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f), CircleShape
                                    ).blur(8.dp)
                                )

                                Icon(
                                    painter = painterResource(id = R.drawable.notifications_icon),
                                    contentDescription = "Notifications",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                } else {
                    IconButton(onClick = { onNotificationsClick?.invoke() }) {
                        Box(
                            modifier = Modifier.size(38.dp).border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)), CircleShape
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            // blurred background layer (behind the icon)
                            Box(
                                modifier = Modifier.matchParentSize().background(
                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f), CircleShape
                                ).blur(8.dp)
                            )

                            Icon(
                                painter = painterResource(id = R.drawable.notifications_icon),
                                contentDescription = "Notifications",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }

                // Profile icon AFTER notification
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

                // gradient background derived from bgColor
                val profileGradient = remember(bgColor) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(bgColor, androidx.compose.ui.graphics.lerp(bgColor, Color.White, 0.12f))
                    )
                }

                IconButton(onClick = { onProfileClick?.invoke() }) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(brush = profileGradient, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            color = contentColor,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)
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

@Preview(showBackground = true, widthDp = 360, name = "TopBar - Default")
@Composable
fun ScreenTopBarPreview_Default() {
    MaterialTheme {
        ScreenTopBar(
            headerTitle = "Dashboard, Tushar Talan",
            showBack = false,
            actions = null,
            showProfileIcons = true,
            onNotificationsClick = {},
            onProfileClick = {},
            unreadCount = 0,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "TopBar - With Badge")
@Composable
fun ScreenTopBarPreview_WithBadge() {
    MaterialTheme {
        ScreenTopBar(
            headerTitle = "Home, Tushar Talan",
            showBack = false,
            showProfileIcons = true,
            onNotificationsClick = {},
            onProfileClick = {},
            unreadCount = 5,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "TopBar - Back")
@Composable
fun ScreenTopBarPreview_Back() {
    MaterialTheme {
        ScreenTopBar(
            headerTitle = "Transactions",
            showBack = true,
            onBack = {},
            showProfileIcons = false,
        )
    }
}
