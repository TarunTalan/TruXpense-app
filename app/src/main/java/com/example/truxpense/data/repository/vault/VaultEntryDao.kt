package com.example.truxpense.data.repository.vault

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {

    // ── Live queries (observed by UI) ─────────────────────────────────────────

    @Query("SELECT * FROM vault_entries ORDER BY savedAt DESC")
    fun getAll(): Flow<List<VaultEntry>>

    @Query("""
        SELECT * FROM vault_entries
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        AND   (:format = '' OR format = :format)
        AND   (:storage = '' OR syncStatus = :storage)
        ORDER BY
            CASE WHEN :sort = 'NAME' THEN title END ASC,
            CASE WHEN :sort = 'SIZE' THEN fileSizeBytes END DESC,
            savedAt DESC
    """)
    fun search(
        query: String   = "",
        format: String  = "",   // "" = all formats
        storage: String = "",   // "" = all storage types
        sort: String    = "DATE",
    ): Flow<List<VaultEntry>>

    // ── Suspend (one-shot) ────────────────────────────────────────────────────

    @Query("SELECT * FROM vault_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultEntry?

    @Query("SELECT * FROM vault_entries WHERE syncStatus IN (:states)")
    suspend fun getByStates(states: List<String>): List<VaultEntry>

    @Query("SELECT * FROM vault_entries WHERE storageOption = :opt AND syncStatus != :synced")
    suspend fun getLocalOnlyPending(
        opt: String    = StorageOption.BOTH.name,
        synced: String = SyncStatus.SYNCED.name,
    ): List<VaultEntry>

    // ── Write ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(entry: VaultEntry)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsertAll(entries: List<VaultEntry>)

    @Query("UPDATE vault_entries SET title = :title WHERE id = :id")
    suspend fun rename(id: String, title: String)

    @Query("""
        UPDATE vault_entries
        SET syncStatus = :status, cloudUrl = :url, storagePath = :path, uploadedAt = :ts
        WHERE id = :id
    """)
    suspend fun markUploaded(
        id: String,
        url: String,
        path: String,
        ts: Long,
        status: String = SyncStatus.SYNCED.name,
    )

    @Query("UPDATE vault_entries SET syncStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE vault_entries SET localFilePath = :path, syncStatus = :status WHERE id = :id")
    suspend fun markDownloaded(
        id: String,
        path: String,
        status: String = SyncStatus.SYNCED.name,
    )

    @Query("UPDATE vault_entries SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: String, tags: String)

    // ── Delete ────────────────────────────────────────────────────────────────

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()
}