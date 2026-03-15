package com.example.truxpense.data.repository.vault

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.truxpense.presentation.screens.dashboard.report.ExportFormat
import com.example.truxpense.presentation.screens.dashboard.report.ReportDetailUiState
import com.example.truxpense.presentation.screens.dashboard.report.ReportExporter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

// ── Operation results ─────────────────────────────────────────────────────────

sealed class SaveResult {
    data class Success(val entry: VaultEntry) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

sealed class DownloadResult {
    data class Success(val publicPath: String, val fileName: String) : DownloadResult()
    data class AlreadyExists(val localPath: String, val fileName: String) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}

sealed class UploadResult {
    data class Success(val entry: VaultEntry) : UploadResult()
    data class Error(val message: String) : UploadResult()
}

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class ReportVaultRepository @Inject constructor(
    private val dao: VaultEntryDao,
    @ApplicationContext private val context: Context,
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String? get() = auth.currentUser?.uid

    // ── Live streams ──────────────────────────────────────────────────────────

    val allEntries: Flow<List<VaultEntry>> = dao.getAll()

    fun search(
        query: String = "",
        format: String = "",
        storage: String = "",
        sort: String = "DATE",
    ): Flow<List<VaultEntry>> = dao.search(query, format, storage, sort)

    // ── Save (export + store) ─────────────────────────────────────────────────

    suspend fun save(
        reportState: ReportDetailUiState,
        format: ExportFormat,
        storageOption: StorageOption,
        tags: List<String>,
        reportId: String = "",
    ): SaveResult {
        // 1. Export file to app-private storage
        val exportResult = ReportExporter.export(context, format, reportState)
        if (exportResult.error != null) return SaveResult.Error(exportResult.error)

        val localFile = File(exportResult.filePath)

        val entry = VaultEntry(
            reportId = reportId,
            title = reportState.title,
            format = format.name,
            reportType = reportState.reportType.name,
            dateRangeLabel = reportState.dateRangeLabel,
            localFilePath = if (storageOption != StorageOption.CLOUD_ONLY) localFile.absolutePath else "",
            fileSizeBytes = localFile.length(),
            tags = tags.joinToString("|"),
            syncStatus = when (storageOption) {
                StorageOption.LOCAL_ONLY -> SyncStatus.LOCAL_ONLY.name
                else -> SyncStatus.SYNCING.name
            },
            storageOption = storageOption.name,
            savedAt = System.currentTimeMillis(),
        )

        // 2. Insert placeholder — UI sees it instantly with SYNCING/LOCAL_ONLY badge
        dao.upsert(entry)

        return when (storageOption) {
            StorageOption.LOCAL_ONLY -> {
                dao.updateStatus(entry.id, SyncStatus.LOCAL_ONLY.name)
                SaveResult.Success(dao.getById(entry.id) ?: entry)
            }

            StorageOption.CLOUD_ONLY -> {
                val result = uploadToFirebase(entry, localFile)
                localFile.delete() // remove local copy — user chose cloud-only
                result.toSaveResult()
            }

            StorageOption.BOTH -> uploadToFirebase(entry, localFile).toSaveResult()
        }
    }

    // ── Firebase upload (called by save() and WorkManager) ────────────────────

    suspend fun uploadToFirebase(entry: VaultEntry, localFile: File): UploadResult {
        val currentUid = uid ?: return UploadResult.Error("User not signed in")
        return try {
            dao.updateStatus(entry.id, SyncStatus.SYNCING.name)

            val ext = entry.format.lowercase().let { if (it == "excel") "xls" else it }
            val fileName = "${entry.title.sanitize()}_${entry.id.take(8)}.$ext"
            val storagePath = "users/$currentUid/reports/${entry.id}/$fileName"
            val ref = storage.reference.child(storagePath)

            ref.putFile(Uri.fromFile(localFile)).await<UploadTask.TaskSnapshot>()
            val cloudUrl = ref.downloadUrl.await<Uri>().toString()
            val now = System.currentTimeMillis()

            // Firestore metadata
            val dto = VaultEntryDto.fromEntity(
                entry.copy(cloudUrl = cloudUrl, storagePath = storagePath, uploadedAt = now)
            )
            firestoreCol(currentUid).document(entry.id).set(dto.toMap()).await<Void>()

            // Room — mark synced
            dao.markUploaded(entry.id, cloudUrl, storagePath, now)

            UploadResult.Success(dao.getById(entry.id) ?: entry)
        } catch (e: Exception) {
            dao.updateStatus(entry.id, SyncStatus.ERROR.name)
            UploadResult.Error(e.message ?: "Upload failed")
        }
    }

    // ── Download to public Downloads (MediaStore) ─────────────────────────────

    suspend fun downloadToPublic(entry: VaultEntry): DownloadResult {
        val ext = entry.format.lowercase().let { if (it == "excel") "xls" else it }
        val fileName = "${entry.title.sanitize()}.$ext"

        // If local file already exists, return AlreadyExists immediately
        if (entry.localFilePath.isNotEmpty() && File(entry.localFilePath).exists()) {
            return DownloadResult.AlreadyExists(entry.localFilePath, fileName)
        }

        return try {
            val mime = mimeFor(entry.format)
            val resolver = context.contentResolver

            val publicUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val col = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(col, cv) ?: return DownloadResult.Error("Could not create Downloads entry")

                resolver.openOutputStream(uri)?.use { out ->
                    if (entry.storagePath.isNotEmpty()) {
                        val tmp = File(context.cacheDir, fileName)
                        storage.reference.child(entry.storagePath).getFile(tmp).await()
                        FileInputStream(tmp).use { it.copyTo(out) }
                        tmp.delete()
                    }
                }

                cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, cv, null, null)
                uri
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                if (entry.storagePath.isNotEmpty()) {
                    storage.reference.child(entry.storagePath).getFile(file).await()
                }
                Uri.fromFile(file)
            }

            DownloadResult.Success(publicUri.toString(), fileName)
        } catch (e: Exception) {
            DownloadResult.Error(e.message ?: "Download failed")
        }
    }

    // ── Sync cloud → Room ─────────────────────────────────────────────────────

    suspend fun syncFromCloud() {
        val currentUid = uid ?: return
        try {
            val snap = firestoreCol(currentUid).orderBy("savedAt", Query.Direction.DESCENDING).get().await()

            // First pass — parse DTOs (non-suspend)
            val dtos = snap.documents.mapNotNull { doc ->
                VaultEntryDto.fromMap(doc.data ?: return@mapNotNull null)
            }

            // Second pass — look up local state with suspend DAO calls
            val remote = dtos.map { dto ->
                val existing = dao.getById(dto.id)
                dto.toEntity(
                    localFilePath = existing?.localFilePath ?: "",
                    syncStatus = if (!existing?.localFilePath.isNullOrEmpty()) SyncStatus.SYNCED
                    else SyncStatus.CLOUD_ONLY,
                )
            }
            dao.upsertAll(remote)
        } catch (_: Exception) { /* offline — Room cache serves the UI */
        }
    }

    // ── WorkManager: retry pending uploads ────────────────────────────────────

    suspend fun uploadPending() {
        val pending = dao.getByStates(listOf(SyncStatus.LOCAL_ONLY.name, SyncStatus.ERROR.name))
            .filter { it.parsedStorageOption() != StorageOption.LOCAL_ONLY }
        pending.forEach { entry ->
            val file = File(entry.localFilePath)
            if (file.exists()) uploadToFirebase(entry, file)
        }
    }

    // ── On-demand sync to cloud (from context menu "Sync to Cloud") ───────────

    suspend fun syncToCloud(entry: VaultEntry): UploadResult {
        val file = File(entry.localFilePath)
        if (!file.exists()) return UploadResult.Error("Local file not found")
        return uploadToFirebase(entry, file)
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    suspend fun rename(entry: VaultEntry, newTitle: String) {
        dao.rename(entry.id, newTitle)
        uid?.let {
            runCatching { firestoreCol(it).document(entry.id).update("title", newTitle) }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    suspend fun delete(entry: VaultEntry) {
        uid?.let { currentUid ->
            runCatching {
                if (entry.storagePath.isNotEmpty()) storage.reference.child(entry.storagePath).delete().await()
                firestoreCol(currentUid).document(entry.id).delete().await()
            }
        }
        dao.deleteById(entry.id)
        if (entry.localFilePath.isNotEmpty()) File(entry.localFilePath).delete()
    }

    suspend fun clearAll() = dao.deleteAll()

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun firestoreCol(uid: String) = firestore.collection("users").document(uid).collection("vault_entries")

    private fun mimeFor(format: String) = when (format.uppercase()) {
        "PDF" -> "application/pdf"
        "CSV" -> "text/csv"
        "EXCEL" -> "application/vnd.ms-excel"
        else -> "application/octet-stream"
    }

    private fun String.sanitize() = replace(Regex("[^A-Za-z0-9_\\- ]"), "").trim().replace(" ", "_").take(60)

    private fun UploadResult.toSaveResult(): SaveResult = when (this) {
        is UploadResult.Success -> SaveResult.Success(entry)
        is UploadResult.Error -> SaveResult.Error(message)
    }
}