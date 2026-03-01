package com.example.truxpense.presentation.screens.dashboard.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.theme.DashboardDimens

// ── Brand colours ─────────────────────────────────────────────────────────────
private val TealAccent = Color(0xFF26C6B4)
private val RedAccent = Color(0xFFEF4444)
private val AmberAccent = Color(0xFFF59E0B)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen notification list.
 *
 * Read/unread behaviour
 * ──────────────────────
 * • Tapping any row immediately calls [vm.markRead] then navigates.
 * • The row background, title weight, body colour, icon saturation and the
 *   teal dot all animate smoothly from their unread → read states.
 * • The "Mark all read" button in the top bar fades out once every notification
 *   is read; the unread-count badge transitions with a cross-fade.
 * • "Mark all read" fires [vm.markAllRead] which flips every item at once;
 *   each row independently animates to its read state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    vm: NotificationViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToBudgetDetail: (budgetName: String, monthlyLimit: Double, spent: Double) -> Unit = { _, _, _ -> },
    onNavigateToWeeklyAnalytics: () -> Unit = {},
    onNavigateToTransactionDetail: (transactionId: String) -> Unit = {},
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
) {
    val notifications by vm.notifications.collectAsState()
    val unreadCount by vm.unreadCount.collectAsState()
    val hasUnread by vm.hasUnread.collectAsState()

    val groups = remember(notifications) { buildGroups(notifications) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NotificationTopBar(
                unreadCount = unreadCount,
                hasUnread = hasUnread,
                onBack = onBack,
                onMarkAllRead = { vm.markAllRead() },
            )
        },
    ) { innerPadding ->

        if (notifications.isEmpty()) {
            EmptyNotifications(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = DashboardDimens.spaceXxxl),
        ) {
            groups.forEach { (groupLabel, items) ->

                // ── Section header ────────────────────────────────────────────
                item(key = "header_$groupLabel") {
                    Text(
                        text = groupLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = DashboardDimens.screenPaddingH,
                            end = DashboardDimens.screenPaddingH,
                            top = DashboardDimens.spaceLg,
                            bottom = DashboardDimens.spaceSm,
                        ),
                    )
                }

                // ── Notification rows ─────────────────────────────────────────
                items(items, key = { it.id }) { item ->
                    NotificationRow(
                        item = item,
                        onClick = {
                            vm.markRead(item.id)
                            handleNavigation(
                                destination = item.destination,
                                onBudgetDetail = onNavigateToBudgetDetail,
                                onWeeklyAnalytics = onNavigateToWeeklyAnalytics,
                                onTransactionDetail = onNavigateToTransactionDetail,
                                onAddExpense = onNavigateToAddExpense,
                                onTransactions = onNavigateToTransactions,
                            )
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(
    unreadCount: Int,
    hasUnread: Boolean,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.left_arrow),
                    contentDescription = "Back",
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                // Animated unread-count badge — disappears when count reaches 0
                AnimatedVisibility(
                    visible = unreadCount > 0,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200)),
                ) {
                    // Cross-fade the number itself when it decrements
                    AnimatedContent(
                        targetState = unreadCount,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        },
                        label = "unread_count",
                    ) { count ->
                        Box(
                            modifier = Modifier.clip(CircleShape).background(TealAccent)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        },
        actions = {
            // "Mark all read" button fades out once every notification is read
            AnimatedVisibility(
                visible = hasUnread,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        text = "Mark all read",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealAccent,
                    )
                }
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit,
) {
    // ── Animated colours / alpha driven by isRead ─────────────────────────────
    val animSpec = tween<Color>(durationMillis = 350)
    val floatSpec = tween<Float>(durationMillis = 350)

    // Row background: subtle tint → transparent
    val bgColor by animateColorAsState(
        targetValue = if (!item.isRead) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f)
        else Color.Transparent,
        animationSpec = animSpec,
        label = "row_bg_${item.id}",
    )

    // Title colour: full contrast → dimmed
    val titleColor by animateColorAsState(
        targetValue = if (!item.isRead) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        animationSpec = animSpec,
        label = "title_color_${item.id}",
    )

    // Body colour: standard variant → more muted
    val bodyColor by animateColorAsState(
        targetValue = if (!item.isRead) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = animSpec,
        label = "body_color_${item.id}",
    )

    // Icon alpha: full → dimmed when read
    val iconAlpha by animateFloatAsState(
        targetValue = if (!item.isRead) 1f else 0.45f,
        animationSpec = floatSpec,
        label = "icon_alpha_${item.id}",
    )

    // Dot alpha: visible → gone
    val dotAlpha by animateFloatAsState(
        targetValue = if (!item.isRead) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dot_alpha_${item.id}",
    )

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).background(bgColor).padding(
            horizontal = DashboardDimens.screenPaddingH,
            vertical = DashboardDimens.spaceMd,
        ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // Icon (alpha-fades when read)
        Box(modifier = Modifier.alpha(iconAlpha)) {
            NotificationIcon(type = item.iconType, isRead = item.isRead)
        }

        // Text block
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                // Bold for unread, normal for read — no animation on FontWeight itself
                // (Compose doesn't interpolate weight), but the colour change gives enough cue.
                fontWeight = if (!item.isRead) FontWeight.SemiBold else FontWeight.Normal,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Unread dot — animates from opaque → invisible rather than abruptly
        // removing from layout (avoids row width jank).
        Box(
            modifier = Modifier.padding(top = 4.dp).size(8.dp).alpha(dotAlpha).clip(CircleShape).background(TealAccent),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationIcon(type: NotificationIconType, isRead: Boolean) {
    val baseIconAlpha = if (isRead) 0.5f else 1f   // extra tint reduction on read

    data class IconStyle(val bgColor: Color, val iconColor: Color, val iconRes: Int)

    val style = when (type) {
        NotificationIconType.BUDGET_EXCEEDED -> IconStyle(
            bgColor = RedAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = RedAccent.copy(alpha = baseIconAlpha),
            iconRes = R.drawable.ic_warning,
        )

        NotificationIconType.BUDGET_WARNING -> IconStyle(
            bgColor = AmberAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = AmberAccent.copy(alpha = baseIconAlpha),
            iconRes = R.drawable.ic_warning,
        )

        NotificationIconType.SPENDING_INSIGHT -> IconStyle(
            bgColor = TealAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = TealAccent.copy(alpha = baseIconAlpha),
            iconRes = R.drawable.ic_trending_up,
        )

        NotificationIconType.ADD_EXPENSE_PROMPT -> IconStyle(
            bgColor = TealAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = TealAccent.copy(alpha = baseIconAlpha),
            iconRes = R.drawable.ic_lightbulb,
        )

        NotificationIconType.SYNC_SUCCESS -> IconStyle(
            bgColor = TealAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = TealAccent.copy(alpha = baseIconAlpha),
            iconRes = R.drawable.ic_sync,
        )
    }

    val animBg by animateColorAsState(
        targetValue = style.bgColor,
        animationSpec = tween(350),
        label = "icon_bg",
    )
    val animTint by animateColorAsState(
        targetValue = style.iconColor,
        animationSpec = tween(350),
        label = "icon_tint",
    )

    Box(
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(animBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(style.iconRes),
            contentDescription = null,
            tint = animTint,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyNotifications(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_notifications_none),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No notifications yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Groups notifications by their [timeLabel], preserving insertion order. */
private fun buildGroups(
    notifications: List<NotificationItem>,
): List<Pair<String, List<NotificationItem>>> {
    val map = LinkedHashMap<String, MutableList<NotificationItem>>()
    notifications.forEach { item -> map.getOrPut(item.timeLabel) { mutableListOf() }.add(item) }
    return map.entries.map { (label, items) -> label to items }
}

/** Routes a [NotificationDestination] to the appropriate callback. */
private fun handleNavigation(
    destination: NotificationDestination,
    onBudgetDetail: (String, Double, Double) -> Unit,
    onWeeklyAnalytics: () -> Unit,
    onTransactionDetail: (String) -> Unit,
    onAddExpense: () -> Unit,
    onTransactions: () -> Unit,
) = when (destination) {
    is NotificationDestination.BudgetDetail -> onBudgetDetail(
        destination.budgetName, destination.monthlyLimit, destination.spent
    )

    NotificationDestination.WeeklyAnalytics -> onWeeklyAnalytics()

    is NotificationDestination.TransactionDetail -> onTransactionDetail(destination.transactionId)

    NotificationDestination.AddExpense -> onAddExpense()

    NotificationDestination.TransactionList -> onTransactions()
}