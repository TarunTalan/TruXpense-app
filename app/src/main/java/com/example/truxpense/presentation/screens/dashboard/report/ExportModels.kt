package com.example.truxpense.presentation.screens.dashboard.report

// ─── Export format ────────────────────────────────────────────────────────────

enum class ExportFormat { PDF, CSV, EXCEL }

// ─── Export status ────────────────────────────────────────────────────────────

enum class ExportStatus { IDLE, EXPORTING, SUCCESS, ERROR }

// ─── Export result ────────────────────────────────────────────────────────────

data class ExportResult(
    /** Absolute path to the generated file. */
    val filePath: String,
    /** MIME type for the share intent. */
    val mimeType: String,
    /** User-facing file name (without directory). */
    val fileName: String,
    /** Non-null if generation failed. */
    val error: String? = null,
)

