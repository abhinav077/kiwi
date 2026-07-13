package com.abhinavsirohi.kiwi.core.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KiwiDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = KiwiDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_preservesRecordsAndCreatesQueue() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO tasks (localId, title, notes, scheduledDate, isCompleted, position, " +
                    "remote_id, user_id, created_at, updated_at, deleted_at, sync_status, " +
                    "last_sync_error, device_id) VALUES " +
                    "('task-1', 'Plan day', NULL, NULL, 0, 0, NULL, 'user-1', 1000, 1000, " +
                    "NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            KiwiDatabase.MIGRATION_1_2,
        ).use { database ->
            database.query("SELECT title FROM tasks WHERE localId = 'task-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Plan day", cursor.getString(0))
            }
            database.query("SELECT COUNT(*) FROM pending_changes").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "kiwi-migration-test"
    }
}
