package com.example.truxpense.presentation.screens.dashboard.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.truxpense.R
import com.example.truxpense.data.repository.vault.StorageOption
import com.example.truxpense.data.repository.vault.SyncStatus
import com.example.truxpense.data.repository.vault.VaultEntry
import com.example.truxpense.presentation.screens.dashboard.components.AppConfirmDialog
import com.example.truxpense.presentation.screens.dashboard.components.GradientCard
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import com.example.truxpense.presentation.utils.SimpleTextField
import java.text.SimpleDateFormat
import java.util.*

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBack: () -> Unit = {},
    vm: VaultViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.snack) {
        val s = state.snack ?: return@LaunchedEffect
        val result = snackbarHost.showSnackbar(
            message = s.message,
            actionLabel = s.actionLabel,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed && s.actionTag != null) {
            vm.handleSnackAction(s.actionTag)
        }
        vm.clearSnack()
    }

    var entryForDelete by remember { mutableStateOf<VaultEntry?>(null) }
    var entryForRename by remember { mutableStateOf<VaultEntry?>(null) }
    var entryForContext by remember { mutableStateOf<VaultEntry?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }

    entryForDelete?.let { entry ->
        AppConfirmDialog(
            title = "Delete from Vault?",
            message = "\"${entry.title}\" will be removed from all devices and cloud storage.",
            confirmLabel = "Delete",
            onConfirm = { vm.delete(entry); entryForDelete = null },
            onDismiss = { entryForDelete = null },
        )
    }

    entryForRename?.let { entry ->
        RenameDialog(
            current = entry.title,
            onConfirm = { vm.rename(entry, it); entryForRename = null },
            onDismiss = { entryForRename = null },
        )
    }

    // Direct sharing: no intermediate bottom sheet. Sharing is invoked from the item actions or context menu.

    entryForContext?.let { entry ->
        VaultContextSheet(
            entry = entry,
            onAction = { action ->
                entryForContext = null
                when (action) {
                    VaultContextAction.OPEN -> vm.openFile(entry)
                    VaultContextAction.DOWNLOAD -> vm.downloadToPublic(entry)
                    VaultContextAction.SHARE -> vm.shareFile(entry)
                    VaultContextAction.SYNC_TO_CLOUD -> vm.syncToCloud(entry)
                    VaultContextAction.RENAME -> entryForRename = entry
                    VaultContextAction.DELETE -> entryForDelete = entry
                }
            },
            onDismiss = { entryForContext = null },
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            current = state.sortOrder,
            onPick = { vm.setSortOrder(it); showSortSheet = false },
            onDismiss = { showSortSheet = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ScreenTopBar(
                headerTitle = "Report Vault",
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.filter),
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(DashboardDimens.iconMd),
                        )
                    }
                    IconButton(onClick = { vm.sync() }, enabled = !state.isSyncing) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(DashboardDimens.iconMd),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            item {
                VaultHero(
                    reportCount = state.entries.size,
                    storageUsedBytes = state.storageUsedBytes,
                )
            }

            val pendingCount = state.entries.count {
                it.parsedSyncStatus() in listOf(
                    SyncStatus.LOCAL_ONLY, SyncStatus.ERROR
                ) && it.parsedStorageOption() != StorageOption.LOCAL_ONLY
            }
            if (pendingCount > 0) {
                item { PendingBanner(count = pendingCount, onRetry = { vm.retryPendingUploads() }) }
            }

            item {
                SearchBar(
                    query = state.searchQuery,
                    onChange = { vm.setSearch(it) },
                    modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp),
                )
            }

            item {
                FilterChipRow(
                    filterStorage = state.filterStorage,
                    filterFormat = state.filterFormat,
                    onStoragePick = { vm.setFilterStorage(it) },
                    onFormatPick = { vm.setFilterFormat(it) },
                )
            }

            if (state.entries.isEmpty()) {
                item { VaultEmptyState(hasSearch = state.searchQuery.isNotEmpty()) }
            }

            items(state.entries, key = { it.id }) { entry ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)),
                ) {
                    VaultItemCard(
                        entry = entry,
                        isBusy = entry.id in state.busyIds,
                        onLongPress = { entryForContext = entry },
                        onOpen = { vm.openFile(entry) },
                        onShare = { vm.shareFile(entry) },
                        onRetry = { vm.syncToCloud(entry) },
                        modifier = Modifier.padding(
                            horizontal = DashboardDimens.screenPaddingH,
                            vertical = 5.dp,
                        ),
                    )
                }
            }
        }
    }
}

// ─── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun VaultHero(reportCount: Int, storageUsedBytes: Long) {
    val primary = MaterialTheme.colorScheme.primary


    GradientCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp),
        elevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Cloud Vault",
                    color = MaterialTheme.colorScheme.onBackground.copy(0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "$reportCount ${if (reportCount == 1) "Report" else "Reports"}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatBytes(storageUsedBytes),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                )
            }

            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

// ─── Pending upload banner ────────────────────────────────────────────────────

@Composable
private fun PendingBanner(count: Int, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "$count ${if (count == 1) "report" else "reports"} waiting to upload",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(
                    "Retry",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ─── Search bar ───────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    SimpleTextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth().height(48.dp),
        bgColor = MaterialTheme.colorScheme.surfaceContainer,
        placeholder = "Search reports or tags…",
        prefix = {
            Icon(
                painterResource(R.drawable.search),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onChange("") }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = "Clear",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
    )
}

// ─── Filter chip row ──────────────────────────────────────────────────────────

@Composable
private fun FilterChipRow(
    filterStorage: VaultFilterStorage,
    filterFormat: VaultFilterFormat,
    onStoragePick: (VaultFilterStorage) -> Unit,
    onFormatPick: (VaultFilterFormat) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = DashboardDimens.screenPaddingH),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 10.dp),
    ) {
        items(VaultFilterStorage.entries) { f ->
            val label = when (f) {
                VaultFilterStorage.ALL -> "All"
                VaultFilterStorage.LOCAL -> "Local"
                VaultFilterStorage.CLOUD -> "Cloud"
                VaultFilterStorage.SYNCED -> "Synced"
            }
            val selected = filterStorage == f
            FilterChip(
                selected = selected,
                onClick = { onStoragePick(f) },
                label = {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.outline.copy(0.25f),
                ),
            )
        }
        items(VaultFilterFormat.entries.drop(1)) { f ->
            val fmtColor = fmtChipColor(f)
            val selected = filterFormat == f
            FilterChip(
                selected = selected,
                onClick = { onFormatPick(if (selected) VaultFilterFormat.ALL else f) },
                label = {
                    Text(
                        f.name,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = fmtColor,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = fmtColor,
                    borderColor = MaterialTheme.colorScheme.outline.copy(0.25f),
                ),
            )
        }
    }
}

// ─── Vault item card ──────────────────────────────────────────────────────────

@Composable
private fun VaultItemCard(
    entry: VaultEntry,
    isBusy: Boolean,
    onLongPress: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val syncStatus = entry.parsedSyncStatus()
    val fmtAccent = fmtAccentColor(entry.format)
    val tags = entry.parsedTags()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth().pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { onLongPress() },
                onTap = { onOpen() },
            )
        },
    ) {
        Column {

            // ── Top body ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                // Format badge
                Box(
                    modifier = Modifier.size(width = 44.dp, height = 52.dp).clip(RoundedCornerShape(12.dp))
                        .background(fmtAccent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        entry.format.take(3).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = fmtAccent,
                        letterSpacing = 0.5.sp,
                    )
                }

                // Metadata column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Title
                    Text(
                        entry.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Date range
                    Text(
                        entry.dateRangeLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Status row: sync pill · size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SyncStatusPill(syncStatus = syncStatus, isBusy = isBusy, onRetry = onRetry)

                        if (entry.fileSizeBytes > 0) {
                            Text(
                                "·",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            Text(
                                formatBytes(entry.fileSizeBytes),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Tags
                    if (tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            tags.take(3).forEach { tag ->
                                Box(
                                    Modifier.clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        tag,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            if (tags.size > 3) {
                                Text(
                                    "+${tags.size - 3}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // More button
                IconButton(
                    onClick = onLongPress,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── Bottom action bar ─────────────────────────────────────────────
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                thickness = 0.5.dp,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Saved date
                Text(
                    formatDate(entry.savedAt),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )

                // Share action
                TextButton(
                    onClick = onShare,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Share",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Syncing progress bar
            AnimatedVisibility(visible = isBusy || syncStatus == SyncStatus.SYNCING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                )
            }
        }
    }
}

// ─── Sync status pill ────────────────────────────────────────────────────────

@Composable
private fun SyncIconFor(status: SyncStatus): ImageVector = when (status) {
    SyncStatus.SYNCED -> Icons.Outlined.CheckCircle
    SyncStatus.CLOUD_ONLY -> Icons.Outlined.Cloud
    SyncStatus.LOCAL_ONLY -> Icons.Outlined.PhoneAndroid
    SyncStatus.ERROR -> Icons.Outlined.Error
    else -> Icons.AutoMirrored.Outlined.HelpOutline
}

@Composable
private fun SyncStatusPill(
    syncStatus: SyncStatus,
    isBusy: Boolean,
    onRetry: (() -> Unit)? = null,
) {
    val color = when {
        isBusy || syncStatus == SyncStatus.SYNCING -> MaterialTheme.colorScheme.tertiary
        syncStatus == SyncStatus.SYNCED -> MaterialTheme.colorScheme.secondary
        syncStatus == SyncStatus.LOCAL_ONLY -> MaterialTheme.colorScheme.onSurfaceVariant
        syncStatus == SyncStatus.CLOUD_ONLY -> MaterialTheme.colorScheme.primary
        syncStatus == SyncStatus.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when {
        isBusy || syncStatus == SyncStatus.SYNCING -> "Syncing"
        syncStatus == SyncStatus.SYNCED -> "Synced"
        syncStatus == SyncStatus.LOCAL_ONLY -> "Local only"
        syncStatus == SyncStatus.CLOUD_ONLY -> "Cloud only"
        syncStatus == SyncStatus.ERROR -> "Sync error"
        else -> "Unknown"
    }
    val icon: ImageVector = SyncIconFor(syncStatus)
    val clickable = onRetry != null && (syncStatus == SyncStatus.ERROR || syncStatus == SyncStatus.LOCAL_ONLY)

    Row(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.10f))
            .then(if (clickable) Modifier.clickable { onRetry?.invoke() } else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isBusy || syncStatus == SyncStatus.SYNCING) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                color = color,
                strokeWidth = 1.5.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp),
            )
        }
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun VaultEmptyState(hasSearch: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (hasSearch) Icons.Outlined.SearchOff else Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            if (hasSearch) "No reports match" else "Your vault is empty",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            if (hasSearch) "Try a different search or filter."
            else "Export a report and tap \"Save to Vault\" to back it up.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Context bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultContextSheet(
    entry: VaultEntry,
    onAction: (VaultContextAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val isLocal = entry.isLocal
    val isCloud = entry.isCloud
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "${entry.format} · ${formatBytes(entry.fileSizeBytes)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
            Spacer(Modifier.height(4.dp))

            ContextItem(
                "Open / Preview", Icons.Outlined.FolderOpen, enabled = isLocal
            ) { onAction(VaultContextAction.OPEN) }
            ContextItem("Download to Device", Icons.Outlined.Download) { onAction(VaultContextAction.DOWNLOAD) }
            if (!isCloud) {
                ContextItem("Sync to Cloud", Icons.Outlined.CloudUpload) { onAction(VaultContextAction.SYNC_TO_CLOUD) }
            }
            ContextItem("Rename", Icons.Outlined.Edit) { onAction(VaultContextAction.RENAME) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
            ContextItem("Delete", Icons.Outlined.Delete, color = MaterialTheme.colorScheme.error) {
                onAction(
                    VaultContextAction.DELETE
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ContextItem(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    color: Color? = null,
    onClick: () -> Unit,
) {
    val labelColor = color ?: MaterialTheme.colorScheme.onBackground
    val contentAlpha = if (enabled) 1f else 0.4f

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = labelColor.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp),
            )
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor.copy(alpha = contentAlpha),
            )
        }
    }
}

// ─── Rename dialog ────────────────────────────────────────────────────────────

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Report", fontWeight = FontWeight.SemiBold) },
        text = {
            SimpleTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                bgColor = MaterialTheme.colorScheme.surfaceContainer,
                contentPadding = 12,
                prefix = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                },
                trailingIcon = if (value.isNotEmpty()) {
                    {
                        IconButton(onClick = { value = "" }) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = "Clear",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                } else null,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text("Rename", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ─── Sort bottom sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    current: VaultSortOrder,
    onPick: (VaultSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Sort by", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            listOf(
                VaultSortOrder.DATE to "Date saved (newest first)",
                VaultSortOrder.NAME to "Name (A → Z)",
                VaultSortOrder.SIZE to "File size (largest first)",
            ).forEach { (order, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onPick(order) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    RadioButton(
                        selected = current == order,
                        onClick = { onPick(order) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatBytes(b: Long): String = when {
    b < 1_024L -> "$b B"
    b < 1_048_576L -> "${"%.1f".format(b / 1_024.0)} KB"
    else -> "${"%.1f".format(b / 1_048_576.0)} MB"
}

private fun formatDate(ts: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))

/** Theme-mapped accent colour for a file format label. */
@Composable
private fun fmtAccentColor(format: String): Color = when (format.uppercase()) {
    "PDF" -> MaterialTheme.colorScheme.error
    "CSV" -> MaterialTheme.colorScheme.secondary
    "EXCEL" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

/** Same mapping used for filter chips (non-composable context). */
@Composable
private fun fmtChipColor(f: VaultFilterFormat): Color = when (f) {
    VaultFilterFormat.PDF -> MaterialTheme.colorScheme.error
    VaultFilterFormat.CSV -> MaterialTheme.colorScheme.secondary
    VaultFilterFormat.EXCEL -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun VaultScreenPreview() {
    TruXpenseTheme {
        val now = System.currentTimeMillis()
        val sample = listOf(
            VaultEntry(
                id = "1", reportId = "r1", title = "January Report", format = "PDF",
                reportType = "ALL", dateRangeLabel = "1 Jan - 31 Jan",
                localFilePath = "/storage/emulated/0/Download/jan_report.pdf",
                cloudUrl = "", storagePath = "", fileSizeBytes = 256_000,
                tags = "Q1|Tax",
                syncStatus = SyncStatus.LOCAL_ONLY.name,
                storageOption = StorageOption.BOTH.name,
                savedAt = now - 86_400_000L,
            ),
            VaultEntry(
                id = "2", reportId = "r2", title = "Travel CSV", format = "CSV",
                reportType = "EXPENSE", dateRangeLabel = "15 Feb - 20 Feb",
                localFilePath = "", cloudUrl = "https://example.com/report2",
                storagePath = "users/uid/reports/2/report.csv", fileSizeBytes = 64_000,
                tags = "Travel",
                syncStatus = SyncStatus.CLOUD_ONLY.name,
                storageOption = StorageOption.CLOUD_ONLY.name,
                savedAt = now - 3_600_000L,
            ),
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { ScreenTopBar(headerTitle = "Report Vault", showBack = true, onBack = {}) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item { VaultHero(reportCount = sample.size, storageUsedBytes = sample.sumOf { it.fileSizeBytes }) }
                val pendingCount = sample.count {
                    it.parsedSyncStatus() in listOf(
                        SyncStatus.LOCAL_ONLY, SyncStatus.ERROR
                    ) && it.parsedStorageOption() != StorageOption.LOCAL_ONLY
                }
                if (pendingCount > 0) item { PendingBanner(count = pendingCount, onRetry = {}) }
                item {
                    SearchBar(
                        query = "",
                        onChange = {},
                        modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH, vertical = 12.dp)
                    )
                }
                item {
                    FilterChipRow(
                        filterStorage = VaultFilterStorage.ALL, filterFormat = VaultFilterFormat.ALL,
                        onStoragePick = {}, onFormatPick = {},
                    )
                }
                items(sample) { entry ->
                    VaultItemCard(
                        entry = entry, isBusy = false, onLongPress = {}, onOpen = {}, onShare = {}, onRetry = {},
                        modifier = Modifier.padding(horizontal = DashboardDimens.screenPaddingH, vertical = 5.dp),
                    )
                }
            }
        }
    }
}