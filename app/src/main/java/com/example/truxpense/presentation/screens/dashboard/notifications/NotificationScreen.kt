package com.example.truxpense.presentation.screens.dashboard.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val selectedIds by vm.selectedIds.collectAsState()
    val isSelectionMode by vm.isSelectionMode.collectAsState()
    val isAllSelected by vm.isAllSelected.collectAsState()
    val selectedCount by vm.selectedCount.collectAsState()

    val haptic = LocalHapticFeedback.current

    // Back press in selection mode exits selection instead of leaving the screen
    BackHandler(enabled = isSelectionMode) { vm.clearSelection() }

    val groups = remember(notifications) { buildGroups(notifications) }

    // ── Delete-all confirmation dialog ────────────────────────────────────────
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    if (showDeleteAllDialog) {
        DeleteConfirmDialog(
            message = "Delete all notifications? This cannot be undone.",
            onConfirm = { vm.deleteAll(); showDeleteAllDialog = false },
            onDismiss = { showDeleteAllDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                },
                label = "topbar_mode",
            ) { inSelectionMode ->
                if (inSelectionMode) {
                    SelectionTopBar(
                        selectedCount = selectedCount,
                        isAllSelected = isAllSelected,
                        onClose = { vm.clearSelection() },
                        onToggleSelectAll = { vm.toggleSelectAll() },
                        onDeleteSelected = { vm.deleteSelected() },
                        onDeleteAll = { showDeleteAllDialog = true },
                    )
                } else {
                    NotificationTopBar(
                        unreadCount = unreadCount,
                        hasUnread = hasUnread,
                        onBack = onBack,
                        onMarkAllRead = { vm.markAllRead() },
                    )
                }
            }
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

                // ── Rows ──────────────────────────────────────────────────────
                items(items, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds

                    NotificationRow(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                vm.toggleSelection(item.id)
                            } else {
                                vm.markRead(item.id)
                                handleNavigation(
                                    destination = item.destination,
                                    onBudgetDetail = onNavigateToBudgetDetail,
                                    onWeeklyAnalytics = onNavigateToWeeklyAnalytics,
                                    onTransactionDetail = onNavigateToTransactionDetail,
                                    onAddExpense = onNavigateToAddExpense,
                                    onTransactions = onNavigateToTransactions,
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.startSelection(item.id)
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = if (isSelectionMode) 72.dp else 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Normal top bar
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
                AnimatedVisibility(
                    visible = unreadCount > 0,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200)),
                ) {
                    AnimatedContent(
                        targetState = unreadCount,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
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
// Selection-mode top bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        navigationIcon = {
            // X button — exits selection mode
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Cancel selection",
                    modifier = Modifier.size(DashboardDimens.iconMd),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        title = {
            // Animates the number as selection changes
            AnimatedContent(
                targetState = selectedCount,
                transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                label = "sel_count",
            ) { count ->
                Text(
                    text = "$count selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            // "Select all" / "Deselect all" text button
            TextButton(onClick = onToggleSelectAll) {
                Text(
                    text = if (isAllSelected) "Deselect all" else "Select all",
                    style = MaterialTheme.typography.labelMedium,
                    color = TealAccent,
                )
            }

            // Delete selected icon
            IconButton(onClick = onDeleteSelected, enabled = selectedCount > 0) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = "Delete selected",
                    tint = if (selectedCount > 0) RedAccent
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Row
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationRow(
    item: NotificationItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val animSpec = tween<Color>(durationMillis = 250)
    val floatSpec = tween<Float>(durationMillis = 250)

    // Row background: unread tint  →  selected highlight  →  transparent (read)
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            !item.isRead -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f)
            else -> Color.Transparent
        },
        animationSpec = animSpec,
        label = "row_bg_${item.id}",
    )

    val titleColor by animateColorAsState(
        targetValue = if (!item.isRead) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        animationSpec = animSpec,
        label = "title_color_${item.id}",
    )

    val bodyColor by animateColorAsState(
        targetValue = if (!item.isRead) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = animSpec,
        label = "body_color_${item.id}",
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (!item.isRead) 1f else 0.45f,
        animationSpec = floatSpec,
        label = "icon_alpha_${item.id}",
    )

    val dotAlpha by animateFloatAsState(
        targetValue = if (!item.isRead) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dot_alpha_${item.id}",
    )

    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ).background(bgColor).padding(
                horizontal = DashboardDimens.screenPaddingH,
                vertical = DashboardDimens.spaceMd,
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
    ) {
        // ── Leading slot: checkbox (selection mode) or notification icon ──────
        AnimatedContent(
            targetState = isSelectionMode,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(
                    tween(180),
                    initialScale = 0.8f
                )) togetherWith (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.8f))
            },
            label = "leading_slot_${item.id}",
        ) { inSelectionMode ->
            if (inSelectionMode) {
                SelectionCircle(isSelected = isSelected)
            } else {
                Box(modifier = Modifier.alpha(iconAlpha)) {
                    NotificationIcon(type = item.iconType, isRead = item.isRead)
                }
            }
        }

        // ── Text block ────────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
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

        // ── Trailing slot: unread dot (hidden in selection mode) ──────────────
        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
        ) {
            Box(
                modifier = Modifier.padding(top = 4.dp).size(8.dp).alpha(dotAlpha).clip(CircleShape)
                    .background(TealAccent),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Selection circle (replaces icon in selection mode)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectionCircle(isSelected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "sel_circle_scale",
    )

    Box(
        modifier = Modifier.size(44.dp),              // same footprint as NotificationIcon so rows don't shift
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            // Filled teal circle with white tick
            Box(
                modifier = Modifier.size((44 * scale).dp).clip(CircleShape).background(TealAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            // Empty circle outline
            Box(
                modifier = Modifier.size((44 * scale).dp).clip(CircleShape).border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Notification icon (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationIcon(type: NotificationIconType, isRead: Boolean) {
    val baseAlpha = if (isRead) 0.5f else 1f

    data class IconStyle(val bgColor: Color, val iconColor: Color, val iconRes: Int)

    val style = when (type) {
        NotificationIconType.BUDGET_EXCEEDED -> IconStyle(
            bgColor = RedAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = RedAccent.copy(alpha = baseAlpha),
            iconRes = R.drawable.ic_warning,
        )

        NotificationIconType.BUDGET_WARNING -> IconStyle(
            bgColor = AmberAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = AmberAccent.copy(alpha = baseAlpha),
            iconRes = R.drawable.ic_warning,
        )

        NotificationIconType.SPENDING_INSIGHT -> IconStyle(
            bgColor = TealAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = TealAccent.copy(alpha = baseAlpha),
            iconRes = R.drawable.ic_trending_up,
        )

        NotificationIconType.ADD_EXPENSE_PROMPT -> IconStyle(
            bgColor = TealAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = TealAccent.copy(alpha = baseAlpha),
            iconRes = R.drawable.ic_lightbulb,
        )

        NotificationIconType.SYNC_SUCCESS -> IconStyle(
            bgColor = TealAccent.copy(alpha = if (isRead) 0.06f else 0.12f),
            iconColor = TealAccent.copy(alpha = baseAlpha),
            iconRes = R.drawable.ic_sync,
        )
    }

    val animBg by animateColorAsState(style.bgColor, tween(350), "icon_bg_$type")
    val animTint by animateColorAsState(style.iconColor, tween(350), "icon_tint_$type")

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
// Delete confirmation dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Delete notifications",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
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

private fun buildGroups(
    notifications: List<NotificationItem>,
): List<Pair<String, List<NotificationItem>>> {
    val map = LinkedHashMap<String, MutableList<NotificationItem>>()
    notifications.forEach { item -> map.getOrPut(item.timeLabel) { mutableListOf() }.add(item) }
    return map.entries.map { (label, items) -> label to items }
}

private fun handleNavigation(
    destination: NotificationDestination,
    onBudgetDetail: (String, Double, Double) -> Unit,
    onWeeklyAnalytics: () -> Unit,
    onTransactionDetail: (String) -> Unit,
    onAddExpense: () -> Unit,
    onTransactions: () -> Unit,
) = when (destination) {
    is NotificationDestination.BudgetDetail -> onBudgetDetail(
        destination.budgetName,
        destination.monthlyLimit,
        destination.spent
    )

    NotificationDestination.WeeklyAnalytics -> onWeeklyAnalytics()

    is NotificationDestination.TransactionDetail -> onTransactionDetail(destination.transactionId)

    NotificationDestination.AddExpense -> onAddExpense()

    NotificationDestination.TransactionList -> onTransactions()
}