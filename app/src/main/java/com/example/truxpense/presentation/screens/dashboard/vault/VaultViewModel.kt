package com.example.truxpense.presentation.screens.dashboard.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truxpense.data.repository.vault.*
import com.example.truxpense.presentation.screens.dashboard.report.ExportFormat
import com.example.truxpense.presentation.screens.dashboard.report.ReportDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class VaultFilterStorage { ALL, LOCAL, CLOUD, SYNCED }
enum class VaultFilterFormat  { ALL, PDF, CSV, EXCEL }
enum class VaultSortOrder     { DATE, NAME, SIZE }
enum class VaultContextAction { OPEN, DOWNLOAD, SHARE, SYNC_TO_CLOUD, RENAME, DELETE }

// ── Snack event ───────────────────────────────────────────────────────────────

data class VaultSnack(
    val message: String,
    val actionLabel: String? = null,
    val actionTag: String?   = null,
)

// ── UI state ──────────────────────────────────────────────────────────────────

data class VaultUiState(
    val entries: List<VaultEntry>             = emptyList(),
    val isLoading: Boolean                    = true,
    val isSyncing: Boolean                    = false,
    val searchQuery: String                   = "",
    val filterStorage: VaultFilterStorage     = VaultFilterStorage.ALL,
    val filterFormat: VaultFilterFormat       = VaultFilterFormat.ALL,
    val sortOrder: VaultSortOrder             = VaultSortOrder.DATE,
    val busyIds: Set<String>                  = emptySet(),
    val snack: VaultSnack?                    = null,
    val storageUsedBytes: Long                = 0L,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: ReportVaultRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _search  = MutableStateFlow("")
    private val _fmtFilt = MutableStateFlow(VaultFilterFormat.ALL)
    private val _stoFilt = MutableStateFlow(VaultFilterStorage.ALL)
    private val _sort    = MutableStateFlow(VaultSortOrder.DATE)
    private val _busy    = MutableStateFlow<Set<String>>(emptySet())
    private val _snack   = MutableStateFlow<VaultSnack?>(null)
    private val _syncing = MutableStateFlow(false)

    @OptIn(FlowPreview::class)
    private val _entries: Flow<List<VaultEntry>> =
        combine(_search.debounce(200), _fmtFilt, _stoFilt, _sort) { q, fmt, sto, sort ->
            val fmtStr = if (fmt == VaultFilterFormat.ALL) "" else fmt.name
            val stoStr = when (sto) {
                VaultFilterStorage.ALL    -> ""
                VaultFilterStorage.LOCAL  -> SyncStatus.LOCAL_ONLY.name
                VaultFilterStorage.CLOUD  -> SyncStatus.CLOUD_ONLY.name
                VaultFilterStorage.SYNCED -> SyncStatus.SYNCED.name
            }
            Triple(q, fmtStr, Pair(stoStr, sort.name))
        }.flatMapLatest { (q, fmt, pair) ->
            vaultRepository.search(q, fmt, pair.first, pair.second)
        }

    val uiState: StateFlow<VaultUiState> = combine(
        _entries, _busy, _snack, _syncing, _search, _fmtFilt, _stoFilt, _sort,
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        VaultUiState(
            entries          = arr[0] as List<VaultEntry>,
            isLoading        = false,
            isSyncing        = arr[3] as Boolean,
            searchQuery      = arr[4] as String,
            filterFormat     = arr[5] as VaultFilterFormat,
            filterStorage    = arr[6] as VaultFilterStorage,
            sortOrder        = arr[7] as VaultSortOrder,
            busyIds          = arr[1] as Set<String>,
            snack            = arr[2] as VaultSnack?,
            storageUsedBytes = (arr[0] as List<VaultEntry>).sumOf { it.fileSizeBytes },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

    init { sync() }

    // ── Filters ───────────────────────────────────────────────────────────────

    fun setSearch(q: String)                   { _search.value  = q }
    fun setFilterFormat(f: VaultFilterFormat)  { _fmtFilt.value = f }
    fun setFilterStorage(s: VaultFilterStorage){ _stoFilt.value = s }
    fun setSortOrder(s: VaultSortOrder)        { _sort.value    = s }

    // ── Sync ──────────────────────────────────────────────────────────────────

    fun sync() {
        viewModelScope.launch {
            _syncing.value = true
            vaultRepository.syncFromCloud()
            _syncing.value = false
        }
    }

    fun retryPendingUploads() {
        VaultSyncWorker.scheduleImmediate(context)
        snack("Uploading pending reports…")
    }

    // ── Save (from VaultSaveBottomSheet) ──────────────────────────────────────

    fun saveToVault(
        reportState: ReportDetailUiState,
        format: ExportFormat,
        storageOption: StorageOption,
        tags: List<String>,
        reportId: String = "",
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            when (val r = vaultRepository.save(reportState, format, storageOption, tags, reportId)) {
                is SaveResult.Success -> { snack("Saved to Vault ✓"); onDone(true) }
                is SaveResult.Error   -> {
                    snack("Save failed: ${r.message}", "Retry", "RETRY_SAVE")
                    onDone(false)
                }
            }
        }
    }

    // ── Context actions ───────────────────────────────────────────────────────

    fun openFile(entry: VaultEntry) {
        val path = entry.localFilePath
        if (path.isEmpty() || !File(path).exists()) { snack("File not on device — download first"); return }
        val uri    = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(path))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeFor(entry.format))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { snack("No app found to open ${entry.format} files") }
    }

    fun downloadToPublic(entry: VaultEntry) {
        setBusy(entry.id, true)
        viewModelScope.launch {
            snack("Downloading…")
            when (val r = vaultRepository.downloadToPublic(entry)) {
                is DownloadResult.Success      ->
                    snack("Saved to Downloads: ${r.fileName}", "OPEN", "OPEN_PUBLIC:${r.publicPath}")
                is DownloadResult.AlreadyExists ->
                    snack("Already downloaded. Open in Files?", "OPEN", "OPEN_LOCAL:${r.localPath}")
                is DownloadResult.Error        -> snack("Download failed: ${r.message}")
            }
            setBusy(entry.id, false)
        }
    }

    fun shareFile(entry: VaultEntry) {
        val path = entry.localFilePath
        if (path.isEmpty() || !File(path).exists()) { snack("File not on device — download first"); return }
        val uri    = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(path))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeFor(entry.format)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${entry.title}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun copyCloudLink(entry: VaultEntry) {
        if (entry.cloudUrl.isEmpty()) { snack("No cloud link — upload first"); return }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Report link", entry.cloudUrl))
        snack("Link copied to clipboard")
    }

    fun syncToCloud(entry: VaultEntry) {
        setBusy(entry.id, true)
        viewModelScope.launch {
            when (val r = vaultRepository.syncToCloud(entry)) {
                is UploadResult.Success -> snack("Synced to cloud ✓")
                is UploadResult.Error   -> snack("Sync failed: ${r.message}")
            }
            setBusy(entry.id, false)
        }
    }

    fun rename(entry: VaultEntry, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            vaultRepository.rename(entry, newTitle.trim())
            snack("Renamed successfully")
        }
    }

    fun delete(entry: VaultEntry) {
        viewModelScope.launch {
            vaultRepository.delete(entry)
            snack("Deleted \"${entry.title}\"")
        }
    }

    // ── Snack action ──────────────────────────────────────────────────────────

    fun handleSnackAction(tag: String) {
        when {
            tag.startsWith("OPEN_PUBLIC:") -> openPublicUri(tag.removePrefix("OPEN_PUBLIC:"))
            tag.startsWith("OPEN_LOCAL:")  -> openLocalFile(tag.removePrefix("OPEN_LOCAL:"))
        }
    }

    fun clearSnack() { _snack.value = null }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun setBusy(id: String, busy: Boolean) =
        _busy.update { if (busy) it + id else it - id }

    private fun snack(msg: String, actionLabel: String? = null, actionTag: String? = null) {
        _snack.value = VaultSnack(msg, actionLabel, actionTag)
    }

    private fun openPublicUri(uriStr: String) = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun openLocalFile(path: String) = runCatching {
        val file = File(path); if (!file.exists()) return@runCatching
        val uri  = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun mimeFor(format: String) = when (format.uppercase()) {
        "PDF"   -> "application/pdf"
        "CSV"   -> "text/csv"
        "EXCEL" -> "application/vnd.ms-excel"
        else    -> "application/octet-stream"
    }
}