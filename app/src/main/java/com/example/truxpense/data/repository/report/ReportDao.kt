package com.example.truxpense.data.repository.report

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    // ── Queries ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    fun getReportById(id: String): Flow<Report?>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getReportByIdOnce(id: String): Report?

    // ── Mutations ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: Report)

    @Delete
    suspend fun delete(report: Report)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reports")
    suspend fun deleteAll()
}