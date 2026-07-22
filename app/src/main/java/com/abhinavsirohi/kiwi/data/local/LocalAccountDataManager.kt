package com.abhinavsirohi.kiwi.data.local

import android.content.Context
import android.net.Uri
import android.util.JsonWriter
import com.abhinavsirohi.kiwi.core.database.KiwiDatabase
import java.io.OutputStreamWriter
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAccountDataManager(
    private val context: Context,
    private val database: KiwiDatabase,
    private val settingsStore: KiwiSettingsStore,
) {
    suspend fun exportJson(destination: Uri, userId: String) = withContext(Dispatchers.IO) {
        val output = requireNotNull(context.contentResolver.openOutputStream(destination)) {
            "The selected export file could not be opened"
        }
        output.use { stream ->
            JsonWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                writer.setIndent("  ")
                writer.beginObject()
                writer.name("format").value("kiwi-local-export-v1")
                writer.name("exported_at").value(Instant.now().toString())
                writer.name("photo_files_included").value(false)
                writer.name("records").beginObject()
                TABLES.forEach { table -> writeTable(writer, table, userId) }
                writer.endObject()
                writer.endObject()
            }
        }
    }

    suspend fun deleteAllLocalData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        context.filesDir.resolve(DIARY_PHOTO_DIRECTORY).deleteRecursively()
        settingsStore.clearAll()
        RestorePreferencesStore(context).clearAll()
    }

    private fun writeTable(writer: JsonWriter, table: String, userId: String) {
        val cursor = database.openHelper.readableDatabase.query(
            "SELECT * FROM `$table` WHERE user_id = ?",
            arrayOf(userId),
        )
        cursor.use {
            writer.name(table).beginArray()
            while (it.moveToNext()) {
                writer.beginObject()
                for (index in 0 until it.columnCount) {
                    writer.name(it.getColumnName(index))
                    when (it.getType(index)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> writer.nullValue()
                        android.database.Cursor.FIELD_TYPE_INTEGER -> writer.value(it.getLong(index))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> writer.value(it.getDouble(index))
                        android.database.Cursor.FIELD_TYPE_STRING -> writer.value(it.getString(index))
                        android.database.Cursor.FIELD_TYPE_BLOB -> writer.value(
                            android.util.Base64.encodeToString(it.getBlob(index), android.util.Base64.NO_WRAP),
                        )
                    }
                }
                writer.endObject()
            }
            writer.endArray()
        }
    }

    private companion object {
        const val DIARY_PHOTO_DIRECTORY = "diary-photos"
        val TABLES = listOf(
            "profiles",
            "tasks",
            "subtasks",
            "cycle_records",
            "wellness_daily_records",
            "health_alert_episodes",
            "diary_entries",
            "diary_photos",
            "self_care_routines",
            "task_postponements",
            "weekly_reflections",
        )
    }
}
