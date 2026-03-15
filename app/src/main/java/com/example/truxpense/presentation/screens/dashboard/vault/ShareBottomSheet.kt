package com.example.truxpense.presentation.screens.dashboard.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.data.repository.vault.VaultEntry
import androidx.compose.ui.tooling.preview.Preview
import com.example.truxpense.presentation.theme.TruXpenseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    entry: VaultEntry,
    onShareFile: () -> Unit,
    onCopyLink: () -> Unit,
    onSendInApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Text(
                "Share \"${entry.title}\"",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "${entry.format} · ${entry.dateRangeLabel}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
            Spacer(Modifier.height(4.dp))

            // ── Share file ────────────────────────────────────────────────────
            ShareRow(
                icon = Icons.Outlined.Share,
                title = "Share File",
                subtitle = "Send via WhatsApp, Gmail, Drive…",
                onClick = { onShareFile(); onDismiss() },
            )

            // ── Copy link ─────────────────────────────────────────────────────
            ShareRow(
                icon = Icons.Outlined.Link,
                title = "Copy Cloud Link",
                subtitle = if (entry.cloudUrl.isNotEmpty()) "Share Firebase download URL"
                else "Not uploaded to cloud yet",
                enabled = entry.cloudUrl.isNotEmpty(),
                onClick = { onCopyLink(); onDismiss() },
            )

            // ── Send in app (future) ───────────────────────────────────────────
            ShareRow(
                icon = Icons.Outlined.Email,
                title = "Send In App",
                subtitle = "Coming soon — send to another TruXpense user",
                enabled = false,
                onClick = { onSendInApp(); onDismiss() },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareBottomSheetPreview() {
    TruXpenseTheme {
        val sample = VaultEntry(
            id = "preview-1",
            reportId = "r1",
            title = "Monthly Summary",
            format = "PDF",
            reportType = "ALL",
            dateRangeLabel = "1 Jan - 31 Jan",
            localFilePath = "",
            cloudUrl = "https://example.com/report.pdf",
            storagePath = "users/uid/reports/preview-1/report.pdf",
            fileSizeBytes = 1024 * 256,
            tags = "finance|monthly",
            syncStatus = com.example.truxpense.data.repository.vault.SyncStatus.CLOUD_ONLY.name,
            storageOption = com.example.truxpense.data.repository.vault.StorageOption.CLOUD_ONLY.name,
            savedAt = System.currentTimeMillis(),
        )

        ShareBottomSheet(
            entry = sample,
            onShareFile = {},
            onCopyLink = {},
            onSendInApp = {},
            onDismiss = {},
        )
    }
}

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun ShareRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val contentAlpha = if (enabled) 1f else 0.4f
    val bgColor = primary.copy(alpha = if (enabled) 0.08f else 0.04f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary.copy(alpha = contentAlpha),
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
            if (!enabled) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("Soon", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}