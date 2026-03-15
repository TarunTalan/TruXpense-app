package com.example.truxpense.presentation.screens.dashboard.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.data.repository.vault.StorageOption
import com.example.truxpense.presentation.screens.dashboard.report.ExportFormat
import com.example.truxpense.presentation.utils.SimpleTextField

// ── Bottom Sheet ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSaveBottomSheet(
    reportTitle: String,
    onSave: (format: ExportFormat, storageOption: StorageOption, tags: List<String>) -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val sheetHandleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedStorage by remember { mutableStateOf(StorageOption.BOTH) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    val keyboard = LocalSoftwareKeyboardController.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.95f

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(sheetHandleColor),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        "Save to Vault",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        reportTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                    )
                }
            }

            HorizontalDivider(color = dividerColor)

            // ── Storage option ────────────────────────────────────────────────
            SheetSection(title = "Storage") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StorageOption.entries.forEach { option ->
                        StorageOptionRow(
                            option = option,
                            selected = selectedStorage == option,
                            onSelect = { selectedStorage = option },
                            primary = primary,
                        )
                    }
                }
            }

            // ── Format ────────────────────────────────────────────────────────
            SheetSection(title = "Format") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { fmt ->
                        val fmtColor = fmtColor(fmt)
                        val selected = selectedFormat == fmt
                        Box(
                            modifier = Modifier.weight(1f).height(48.dp).background(
                                if (selected) fmtColor.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium
                            ).border(
                                1.dp,
                                if (selected) fmtColor else MaterialTheme.colorScheme.outline.copy(0.2f),
                                RoundedCornerShape(10.dp),
                            ).clickable { selectedFormat = fmt },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                fmt.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) fmtColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Tags ──────────────────────────────────────────────────────────
            SheetSection(title = "Tags (optional)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleTextField(
                        value = tagInput,
                        bgColor = MaterialTheme.colorScheme.surfaceContainer,
                        onValueChange = { tagInput = it },
                        placeholder = "e.g. Tax, Work - press Enter to add",
                        modifier = Modifier.fillMaxWidth(),
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            val t = tagInput.trim()
                            if (t.isNotBlank() && t !in tags) tags = tags + t
                            tagInput = ""
                            keyboard?.hide()
                        },
                        singleLine = true,
                    )
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                        .background(primary.copy(alpha = 0.1f)).clickable { tags = tags - tag }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(tag, fontSize = 12.sp, color = primary)
                                        Text("×", fontSize = 12.sp, color = primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Error banner ──────────────────────────────────────────────────
            AnimatedVisibility(visible = saveError != null) {
                if (saveError != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                saveError,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.weight(1f),
                            )
                            if (onRetry != null) {
                                TextButton(onClick = onRetry) {
                                    Text(
                                        "Retry",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick = { onSave(selectedFormat, selectedStorage, tags) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
            ) {
                if (isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Text("Saving…", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        "Save to Vault",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

// ── Storage option row ────────────────────────────────────────────────────────

@Composable
private fun StorageOptionRow(
    option: StorageOption,
    selected: Boolean,
    onSelect: () -> Unit,
    primary: Color,
) {
    val (icon, title, subtitle) = when (option) {
        StorageOption.LOCAL_ONLY -> Triple(Icons.Outlined.PhoneAndroid, "Local only", "Saved on this device (Room DB)")
        StorageOption.CLOUD_ONLY -> Triple(Icons.Outlined.Cloud, "Cloud only", "Stored in Firebase - no local copy")
        StorageOption.BOTH -> Triple(Icons.Outlined.Sync, "Both (recommended)", "Local + Firebase Storage")
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        border = if (selected) BorderStroke(1.5.dp, primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 18.sp
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = primary),
            )
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

// ── Format accent colours — mapped to theme ───────────────────────────────────

@Composable
private fun fmtColor(fmt: ExportFormat): Color = when (fmt) {
    ExportFormat.PDF -> MaterialTheme.colorScheme.error
    ExportFormat.CSV -> MaterialTheme.colorScheme.secondary
    ExportFormat.EXCEL -> MaterialTheme.colorScheme.tertiary
}