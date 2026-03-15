package com.example.truxpense.data.repository.vault
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// ── Sync / storage status ─────────────────────────────────────────────────────

enum class SyncStatus {
    /** File exists only on this device — not in Firebase Storage. */
    LOCAL_ONLY,
    /** File exists only in Firebase Storage — not downloaded yet. */
    CLOUD_ONLY,
    /** File exists both locally and in cloud. */
    SYNCED,
    /** Upload or download is in progress. */
    SYNCING,
    /** Last cloud operation failed. */
    ERROR,
}

// ── Storage preference chosen at save time ────────────────────────────────────

enum class StorageOption {
    LOCAL_ONLY,
    CLOUD_ONLY,
    BOTH,
}

// ── Room entity ───────────────────────────────────────────────────────────────

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    // Back-reference to the original Report config (Report.id)
    val reportId: String = "",

    // ── Display metadata ──────────────────────────────────────────────────────
    val title: String,
    val format: String,                     // "PDF" | "CSV" | "EXCEL"
    val reportType: String = "",            // "EXPENSE" | "INCOME" | "ALL"
    val dateRangeLabel: String = "",

    // ── File ──────────────────────────────────────────────────────────────────
    val localFilePath: String = "",         // absolute path; "" = not on device
    val cloudUrl: String = "",              // Firebase Storage https URL; "" = not uploaded
    val storagePath: String = "",           // "users/{uid}/reports/{id}/{file}"
    val fileSizeBytes: Long = 0L,

    // ── User extras ───────────────────────────────────────────────────────────
    /** Pipe-separated tag list, e.g. "Q1|Tax|Work". Empty = no tags. */
    val tags: String = "",

    // ── Status ────────────────────────────────────────────────────────────────
    val syncStatus: String = SyncStatus.LOCAL_ONLY.name,
    val storageOption: String = StorageOption.BOTH.name,

    // ── Timestamps ────────────────────────────────────────────────────────────
    val savedAt: Long = System.currentTimeMillis(),
    val uploadedAt: Long = 0L,
) {
    fun parsedSyncStatus(): SyncStatus =
        runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY)

    fun parsedStorageOption(): StorageOption =
        runCatching { StorageOption.valueOf(storageOption) }.getOrDefault(StorageOption.BOTH)

    fun parsedTags(): List<String> =
        if (tags.isBlank()) emptyList() else tags.split("|").filter { it.isNotBlank() }

    val isLocal: Boolean  get() = localFilePath.isNotEmpty()
    val isCloud: Boolean  get() = cloudUrl.isNotEmpty()
    val isSynced: Boolean get() = parsedSyncStatus() == SyncStatus.SYNCED
}

// ── Firestore DTO ─────────────────────────────────────────────────────────────

data class VaultEntryDto(
    val id: String = "",
    val reportId: String = "",
    val title: String = "",
    val format: String = "",
    val reportType: String = "",
    val dateRangeLabel: String = "",
    val cloudUrl: String = "",
    val storagePath: String = "",
    val fileSizeBytes: Long = 0L,
    val tags: String = "",
    val storageOption: String = StorageOption.BOTH.name,
    val savedAt: Long = 0L,
    val uploadedAt: Long = 0L,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id"             to id,
        "reportId"       to reportId,
        "title"          to title,
        "format"         to format,
        "reportType"     to reportType,
        "dateRangeLabel" to dateRangeLabel,
        "cloudUrl"       to cloudUrl,
        "storagePath"    to storagePath,
        "fileSizeBytes"  to fileSizeBytes,
        "tags"           to tags,
        "storageOption"  to storageOption,
        "savedAt"        to savedAt,
        "uploadedAt"     to uploadedAt,
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(m: Map<String, Any>) = VaultEntryDto(
            id             = m["id"] as? String ?: "",
            reportId       = m["reportId"] as? String ?: "",
            title          = m["title"] as? String ?: "",
            format         = m["format"] as? String ?: "",
            reportType     = m["reportType"] as? String ?: "",
            dateRangeLabel = m["dateRangeLabel"] as? String ?: "",
            cloudUrl       = m["cloudUrl"] as? String ?: "",
            storagePath    = m["storagePath"] as? String ?: "",
            fileSizeBytes  = (m["fileSizeBytes"] as? Long) ?: 0L,
            tags           = m["tags"] as? String ?: "",
            storageOption  = m["storageOption"] as? String ?: StorageOption.BOTH.name,
            savedAt        = (m["savedAt"] as? Long) ?: 0L,
            uploadedAt     = (m["uploadedAt"] as? Long) ?: 0L,
        )

        fun fromEntity(e: VaultEntry) = VaultEntryDto(
            id             = e.id,
            reportId       = e.reportId,
            title          = e.title,
            format         = e.format,
            reportType     = e.reportType,
            dateRangeLabel = e.dateRangeLabel,
            cloudUrl       = e.cloudUrl,
            storagePath    = e.storagePath,
            fileSizeBytes  = e.fileSizeBytes,
            tags           = e.tags,
            storageOption  = e.storageOption,
            savedAt        = e.savedAt,
            uploadedAt     = e.uploadedAt,
        )
    }
}

fun VaultEntryDto.toEntity(localFilePath: String = "", syncStatus: SyncStatus = SyncStatus.CLOUD_ONLY) =
    VaultEntry(
        id             = id,
        reportId       = reportId,
        title          = title,
        format         = format,
        reportType     = reportType,
        dateRangeLabel = dateRangeLabel,
        localFilePath  = localFilePath,
        cloudUrl       = cloudUrl,
        storagePath    = storagePath,
        fileSizeBytes  = fileSizeBytes,
        tags           = tags,
        syncStatus     = syncStatus.name,
        storageOption  = storageOption,
        savedAt        = savedAt,
        uploadedAt     = uploadedAt,
    )