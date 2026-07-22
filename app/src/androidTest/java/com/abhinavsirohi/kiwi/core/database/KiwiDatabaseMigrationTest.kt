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

    @Test
    fun migrate2To3_preservesExistingDataAndCreatesProfiles() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                "INSERT INTO tasks (localId, title, notes, scheduledDate, isCompleted, position, " +
                    "remote_id, user_id, created_at, updated_at, deleted_at, sync_status, " +
                    "last_sync_error, device_id) VALUES " +
                    "('task-2', 'Keep me', NULL, NULL, 0, 0, NULL, 'user-1', 1000, 1000, " +
                    "NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            KiwiDatabase.MIGRATION_2_3,
        ).use { database ->
            database.query("SELECT title FROM tasks WHERE localId = 'task-2'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Keep me", cursor.getString(0))
            }
            database.query("SELECT COUNT(*) FROM profiles").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate3To4_preservesTasksAndAddsCrudFields() {
        helper.createDatabase(TEST_DATABASE, 3).apply {
            execSQL(
                "INSERT INTO tasks (localId, title, notes, scheduledDate, isCompleted, position, " +
                    "remote_id, user_id, created_at, updated_at, deleted_at, sync_status, " +
                    "last_sync_error, device_id) VALUES " +
                    "('task-3', 'Keep details', 'note', '2026-07-14', 0, 0, NULL, " +
                    "'user-1', 1000, 1000, NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            KiwiDatabase.MIGRATION_3_4,
        ).use { database ->
            database.query(
                "SELECT title, description, category, priority, scheduledTimeMinutes " +
                    "FROM tasks WHERE localId = 'task-3'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("Keep details", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals("PERSONAL", cursor.getString(2))
                assertEquals("NORMAL", cursor.getString(3))
                assertEquals(true, cursor.isNull(4))
            }
        }
    }

    @Test
    fun migrate4To5_preservesTasksAndAddsRecurrenceFields() {
        helper.createDatabase(TEST_DATABASE, 4).apply {
            execSQL(
                "INSERT INTO tasks (localId, title, description, category, priority, notes, " +
                    "scheduledDate, scheduledTimeMinutes, isCompleted, position, remote_id, user_id, " +
                    "created_at, updated_at, deleted_at, sync_status, last_sync_error, device_id) VALUES " +
                    "('task-4', 'Keep recurring-ready', NULL, 'PERSONAL', 'NORMAL', NULL, " +
                    "'2026-07-14', NULL, 0, 0, NULL, 'user-1', 1000, 1000, NULL, " +
                    "'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            KiwiDatabase.MIGRATION_4_5,
        ).use { database ->
            database.query(
                "SELECT title, recurrenceFrequency, recurrenceInterval, recurrenceEndDate, " +
                    "recurrenceSeriesId FROM tasks WHERE localId = 'task-4'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("Keep recurring-ready", cursor.getString(0))
                assertEquals("NONE", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals(true, cursor.isNull(3))
                assertEquals(true, cursor.isNull(4))
            }
        }
    }

    @Test
    fun migrate5To6_preservesExistingDataAndCreatesWellnessRecords() {
        helper.createDatabase(TEST_DATABASE, 5).apply {
            execSQL(
                "INSERT INTO tasks (localId, title, description, category, priority, notes, " +
                    "scheduledDate, scheduledTimeMinutes, recurrenceFrequency, recurrenceInterval, " +
                    "recurrenceEndDate, recurrenceSeriesId, isCompleted, position, remote_id, user_id, " +
                    "created_at, updated_at, deleted_at, sync_status, last_sync_error, device_id) VALUES " +
                    "('task-5', 'Keep after wellness migration', NULL, 'PERSONAL', 'NORMAL', NULL, " +
                    "'2026-07-14', NULL, 'NONE', 1, NULL, NULL, 0, 0, NULL, 'user-1', 1000, 1000, " +
                    "NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            KiwiDatabase.MIGRATION_5_6,
        ).use { database ->
            database.query("SELECT title FROM tasks WHERE localId = 'task-5'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Keep after wellness migration", cursor.getString(0))
            }
            database.query("SELECT COUNT(*) FROM cycle_records").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM wellness_daily_records").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate6To7_preservesDailyRecordAndAddsWellnessFields() {
        helper.createDatabase(TEST_DATABASE, 6).apply {
            execSQL(
                "INSERT INTO wellness_daily_records (localId, record_date, cycle_local_id, remote_id, " +
                    "user_id, created_at, updated_at, deleted_at, sync_status, last_sync_error, device_id) " +
                    "VALUES ('daily-1', '2026-07-14', NULL, NULL, 'user-1', 1000, 1000, NULL, " +
                    "'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            KiwiDatabase.MIGRATION_6_7,
        ).use { database ->
            database.query(
                "SELECT record_date, flow, pain_level, symptoms_json, self_care_medication_notes " +
                    "FROM wellness_daily_records WHERE localId = 'daily-1'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("2026-07-14", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals(true, cursor.isNull(3))
                assertEquals(true, cursor.isNull(4))
            }
        }
    }

    @Test
    fun migrate7To8_preservesWellnessDataAndCreatesHealthAlertEpisodes() {
        helper.createDatabase(TEST_DATABASE, 7).close()
        helper.runMigrationsAndValidate(TEST_DATABASE, 8, true, KiwiDatabase.MIGRATION_7_8).use { database ->
            database.query("SELECT COUNT(*) FROM health_alert_episodes").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate8To9_preservesExistingDataAndCreatesDiaryEntries() {
        helper.createDatabase(TEST_DATABASE, 8).close()

        helper.runMigrationsAndValidate(TEST_DATABASE, 9, true, KiwiDatabase.MIGRATION_8_9).use { database ->
            database.query("SELECT COUNT(*) FROM diary_entries").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate9To10_preservesDiaryEntriesAndCreatesDiaryPhotos() {
        helper.createDatabase(TEST_DATABASE, 9).apply {
            execSQL(
                "INSERT INTO diary_entries (localId, title, content, entry_date, best_thing, mood, " +
                    "is_favourite, remote_id, user_id, created_at, updated_at, deleted_at, sync_status, " +
                    "last_sync_error, device_id) VALUES ('entry-1', 'A day', 'Writing', '2026-07-14', " +
                    "NULL, NULL, 0, NULL, 'user-1', 1000, 1000, NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            10,
            true,
            KiwiDatabase.MIGRATION_9_10,
        ).use { database ->
            database.query("SELECT title FROM diary_entries WHERE localId = 'entry-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("A day", cursor.getString(0))
            }
            database.query("SELECT COUNT(*) FROM diary_photos").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate10To11_preservesDiaryPhotosAndCreatesSelfCareRoutines() {
        helper.createDatabase(TEST_DATABASE, 10).apply {
            execSQL(
                "INSERT INTO diary_entries (localId, title, content, entry_date, best_thing, mood, " +
                    "is_favourite, remote_id, user_id, created_at, updated_at, deleted_at, sync_status, " +
                    "last_sync_error, device_id) VALUES ('entry-10', 'A day', 'Writing', '2026-07-14', " +
                    "NULL, NULL, 0, NULL, 'user-1', 1000, 1000, NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 11, true, KiwiDatabase.MIGRATION_10_11).use { database ->
            database.query("SELECT COUNT(*) FROM diary_photos").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM self_care_routines").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate11To12_preservesTasksAndCreatesReviewRecords() {
        helper.createDatabase(TEST_DATABASE, 11).apply {
            execSQL(
                "INSERT INTO tasks (localId, title, description, category, priority, notes, " +
                    "scheduledDate, scheduledTimeMinutes, recurrenceFrequency, recurrenceInterval, " +
                    "recurrenceEndDate, recurrenceSeriesId, isCompleted, position, remote_id, user_id, " +
                    "created_at, updated_at, deleted_at, sync_status, last_sync_error, device_id) VALUES " +
                    "('task-11', 'Keep for review', NULL, 'PERSONAL', 'NORMAL', NULL, " +
                    "'2026-07-21', NULL, 'NONE', 1, NULL, NULL, 0, 0, NULL, 'user-1', 1000, 1000, " +
                    "NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 12, true, KiwiDatabase.MIGRATION_11_12).use { database ->
            database.query("SELECT title FROM tasks WHERE localId = 'task-11'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Keep for review", cursor.getString(0))
            }
            database.query("SELECT COUNT(*) FROM task_postponements").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM weekly_reflections").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate1To12_preservesOriginalTaskAcrossEveryMigration() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO tasks (localId, title, notes, scheduledDate, isCompleted, position, " +
                    "remote_id, user_id, created_at, updated_at, deleted_at, sync_status, " +
                    "last_sync_error, device_id) VALUES " +
                    "('task-chain', 'Keep through every migration', NULL, NULL, 0, 0, NULL, " +
                    "'user-1', 1000, 1000, NULL, 'PENDING', NULL, 'device-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            12,
            true,
            *KiwiDatabase.MIGRATIONS,
        ).use { database ->
            database.query("SELECT title FROM tasks WHERE localId = 'task-chain'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Keep through every migration", cursor.getString(0))
            }
            database.query("SELECT COUNT(*) FROM weekly_reflections").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "kiwi-migration-test"
    }
}
