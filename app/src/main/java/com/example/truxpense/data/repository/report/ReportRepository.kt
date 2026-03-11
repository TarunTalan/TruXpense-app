package com.example.truxpense.data.repository.report

import kotlinx.coroutines.flow.Flow

// ── Interface ─────────────────────────────────────────────────────────────────

interface ReportRepository {
    val reports: Flow<List<Report>>
    fun getReportById(id: String): Flow<Report?>
    suspend fun getReportByIdOnce(id: String): Report?
    suspend fun saveReport(report: Report)
    suspend fun deleteReport(id: String)
    suspend fun clear()
}

// ── Room-backed implementation ────────────────────────────────────────────────

class ReportRepositoryImpl(
    private val dao: ReportDao,
) : ReportRepository {

    override val reports: Flow<List<Report>> = dao.getAllReports()

    override fun getReportById(id: String): Flow<Report?> = dao.getReportById(id)

    override suspend fun getReportByIdOnce(id: String): Report? = dao.getReportByIdOnce(id)

    override suspend fun saveReport(report: Report) = dao.upsert(report)

    override suspend fun deleteReport(id: String) = dao.deleteById(id)

    override suspend fun clear() = dao.deleteAll()
}

// ── NOTE ──────────────────────────────────────────────────────────────────────
// 1. Add ReportRepositoryImpl to your Hilt module, binding it to ReportRepository.
// 2. Add the Report entity to your @Database(entities = [...]) list.
// 3. Increment your database version and add a migration (or allowDestructiveMigration
//    in dev builds).
//
// Example Hilt binding (inside your existing RepositoryModule):
//
//   @Binds @Singleton
//   abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository