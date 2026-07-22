package com.abhinavsirohi.kiwi.data.remote

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseRestoreRemoteDataSourceTest {
    @Test
    fun authenticatedOwner_rowsMapToCompleteTaskAndProfileSnapshot() = runBlocking {
        val rows = RecordingRestoreRowsSource(
            mapOf(
                "profiles" to listOf(profileRow()),
                "tasks" to listOf(taskRow()),
            ),
        )
        val source = source(rows)

        val result = source.fetchSnapshot(USER_ID) as RemoteResult.Success
        val snapshot = result.value

        assertEquals("K", snapshot.profile?.preferredName)
        assertEquals(1_000L, snapshot.profile?.createdAt)
        assertEquals("Detailed task", snapshot.tasks.single().description)
        assertEquals(615, snapshot.tasks.single().scheduledTimeMinutes)
        assertEquals("series-1", snapshot.tasks.single().recurrenceSeriesId)
        assertEquals(10, rows.requests.size)
        assertTrue(rows.requests.all { it.third == USER_ID })
    }

    @Test
    fun requestedOwnerMustMatchAuthenticatedSession_beforeAnyRead() = runBlocking {
        val rows = RecordingRestoreRowsSource(emptyMap())

        assertEquals(RemoteResult.Failure(RemoteError.AccessDenied), source(rows).fetchSnapshot("other-user"))
        assertTrue(rows.requests.isEmpty())
    }

    private fun source(rows: RestoreRowsSource) = SupabaseRestoreRemoteDataSource(
        clientResult = RemoteResult.Failure(RemoteError.ConfigurationMissing),
        sessionProvider = object : SessionProvider {
            override fun currentSession() = RemoteResult.Success(AuthenticatedSession(USER_ID, "approved@example.com"))
        },
        rowsSource = rows,
    )

    private fun profileRow() = buildJsonObject {
        put("id", USER_ID); put("preferred_name", "K")
        put("created_at", "1970-01-01T00:00:01Z"); put("updated_at", "1970-01-01T00:00:02Z")
    }

    private fun taskRow(): JsonObject = buildJsonObject {
        put("local_id", "task-1"); put("user_id", USER_ID); put("title", "Restored")
        put("description", "Detailed task"); put("category", "WORK"); put("priority", "HIGH")
        put("notes", "Cloud note"); put("scheduled_date", "2026-07-22"); put("scheduled_time_minutes", 615)
        put("recurrence_frequency", "WEEKLY"); put("recurrence_interval", 2)
        put("recurrence_end_date", "2026-09-01"); put("recurrence_series_id", "series-1")
        put("is_completed", false); put("position", 3); put("created_at", 1_000L); put("updated_at", 2_000L)
        put("device_id", "remote-device")
    }

    private companion object {
        const val USER_ID = "00000000-0000-0000-0000-000000000001"
    }
}

private class RecordingRestoreRowsSource(
    private val rowsByTable: Map<String, List<JsonObject>>,
) : RestoreRowsSource {
    val requests = mutableListOf<Triple<String, String, String>>()

    override suspend fun rows(table: String, ownerColumn: String, userId: String): List<JsonObject> {
        requests += Triple(table, ownerColumn, userId)
        return rowsByTable[table].orEmpty()
    }
}
