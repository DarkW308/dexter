package com.aistudio.dexter.engine

import java.io.Serializable

data class PipelineToken(
    val raw: String,
    val normalized: String,
    val languageTag: String, // e.g. "English (US/UK)", "Chinese (中文)", "Russian (Русский)", "Hindi (हिन्दी)", "Hinglish / Accent"
    val accentNote: String = "Clean Speech"
) : Serializable

data class ExtractedSlot(
    val key: String,
    val value: String,
    val slotType: String // e.g. "METRIC", "CURRENCY", "TEMPORAL", "ENTITY", "ACTION"
) : Serializable

data class DatabaseRowTrace(
    val tableName: String,
    val segmentNumber: Int,
    val segmentTitle: String,
    val operation: String, // "INSERT", "UPDATE", "BATCH"
    val rowData: Map<String, String>
) : Serializable

data class VoicePipelineTrace(
    val query: String,
    val tokenTrace: PipelineToken,
    val intent: String,
    val confidence: Float,
    val targetSegment: Int,
    val segmentName: String,
    val slots: List<ExtractedSlot>,
    val dbTraces: List<DatabaseRowTrace>,
    val responseText: String,
    val executionTimeMs: Long,
    val isSuccess: Boolean
) : Serializable
