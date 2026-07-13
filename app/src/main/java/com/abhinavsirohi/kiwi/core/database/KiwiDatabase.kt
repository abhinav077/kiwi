package com.abhinavsirohi.kiwi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abhinavsirohi.kiwi.data.local.dao.PendingChangeDao
import com.abhinavsirohi.kiwi.data.local.dao.ProfileDao
import com.abhinavsirohi.kiwi.data.local.dao.TaskDao
import com.abhinavsirohi.kiwi.data.local.dao.WellnessDao
import com.abhinavsirohi.kiwi.data.local.dao.HealthAlertDao
import com.abhinavsirohi.kiwi.data.local.entity.CycleRecordEntity
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.ProfileEntity
import com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.WellnessDailyRecordEntity
import com.abhinavsirohi.kiwi.data.local.entity.HealthAlertEpisodeEntity
import com.abhinavsirohi.kiwi.data.local.entity.DiaryEntryEntity
import com.abhinavsirohi.kiwi.data.local.dao.DiaryDao
import com.abhinavsirohi.kiwi.data.local.entity.DiaryPhotoEntity
import com.abhinavsirohi.kiwi.data.local.dao.SelfCareDao
import com.abhinavsirohi.kiwi.data.local.entity.SelfCareRoutineEntity

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        PendingChangeEntity::class,
        ProfileEntity::class,
        CycleRecordEntity::class,
        WellnessDailyRecordEntity::class,
        HealthAlertEpisodeEntity::class,
        DiaryEntryEntity::class,
        DiaryPhotoEntity::class,
        SelfCareRoutineEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
@TypeConverters(KiwiTypeConverters::class)
abstract class KiwiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun pendingChangeDao(): PendingChangeDao

    abstract fun profileDao(): ProfileDao

    abstract fun wellnessDao(): WellnessDao

    abstract fun healthAlertDao(): HealthAlertDao

    abstract fun diaryDao(): DiaryDao

    abstract fun selfCareDao(): SelfCareDao

    companion object {
        const val DATABASE_NAME = "kiwi.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_changes` (" +
                        "`queue_id` TEXT NOT NULL, `record_type` TEXT NOT NULL, " +
                        "`record_local_id` TEXT NOT NULL, `operation` TEXT NOT NULL, " +
                        "`state` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL, `attempt_count` INTEGER NOT NULL, " +
                        "`next_attempt_at` INTEGER NOT NULL, `last_error` TEXT, " +
                        "PRIMARY KEY(`queue_id`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_pending_changes_record_type_record_local_id` ON " +
                        "`pending_changes` (`record_type`, `record_local_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pending_changes_state_next_attempt_at_updated_at` " +
                        "ON `pending_changes` (`state`, `next_attempt_at`, `updated_at`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (" +
                        "`localId` TEXT NOT NULL, `preferred_name` TEXT NOT NULL, " +
                        "`remote_id` TEXT, `user_id` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                        "`deleted_at` INTEGER, `sync_status` TEXT NOT NULL, " +
                        "`last_sync_error` TEXT, `device_id` TEXT NOT NULL, " +
                        "PRIMARY KEY(`localId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_profiles_user_id` " +
                        "ON `profiles` (`user_id`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `description` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'PERSONAL'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `scheduledTimeMinutes` INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceFrequency` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceInterval` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceEndDate` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceSeriesId` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_recurrenceSeriesId_scheduledDate` " +
                        "ON `tasks` (`recurrenceSeriesId`, `scheduledDate`)",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cycle_records` (" +
                        "`localId` TEXT NOT NULL, `start_date` TEXT NOT NULL, `end_date` TEXT, " +
                        "`remote_id` TEXT, `user_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_status` TEXT NOT NULL, " +
                        "`last_sync_error` TEXT, `device_id` TEXT NOT NULL, PRIMARY KEY(`localId`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cycle_records_user_id_start_date` " +
                        "ON `cycle_records` (`user_id`, `start_date`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wellness_daily_records` (" +
                        "`localId` TEXT NOT NULL, `record_date` TEXT NOT NULL, `cycle_local_id` TEXT, " +
                        "`remote_id` TEXT, `user_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_status` TEXT NOT NULL, " +
                        "`last_sync_error` TEXT, `device_id` TEXT NOT NULL, PRIMARY KEY(`localId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_wellness_daily_records_user_id_record_date` " +
                        "ON `wellness_daily_records` (`user_id`, `record_date`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_wellness_daily_records_cycle_local_id` " +
                        "ON `wellness_daily_records` (`cycle_local_id`)",
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `flow` TEXT")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `pain_level` INTEGER")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `cramps_level` INTEGER")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `symptoms_json` TEXT")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `mood` TEXT")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `energy_level` INTEGER")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `sleep_minutes` INTEGER")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `notes` TEXT")
                db.execSQL("ALTER TABLE `wellness_daily_records` ADD COLUMN `exercise` TEXT")
                db.execSQL(
                    "ALTER TABLE `wellness_daily_records` ADD COLUMN `self_care_medication_notes` TEXT",
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `health_alert_episodes` (" +
                        "`localId` TEXT NOT NULL, `pattern_type` TEXT NOT NULL, `source_cycle_id` TEXT NOT NULL, " +
                        "`evidence_count` INTEGER NOT NULL, `first_evidence_date` TEXT NOT NULL, " +
                        "`last_evidence_date` TEXT NOT NULL, `state` TEXT NOT NULL, `notified_at` INTEGER, " +
                        "`remote_id` TEXT, `user_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_status` TEXT NOT NULL, " +
                        "`last_sync_error` TEXT, `device_id` TEXT NOT NULL, PRIMARY KEY(`localId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_health_alert_episodes_user_id_pattern_type_source_cycle_id` " +
                        "ON `health_alert_episodes` (`user_id`, `pattern_type`, `source_cycle_id`)",
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `diary_entries` (" +
                        "`localId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, " +
                        "`entry_date` TEXT NOT NULL, `best_thing` TEXT, `mood` TEXT, `is_favourite` INTEGER NOT NULL, " +
                        "`remote_id` TEXT, `user_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_status` TEXT NOT NULL, " +
                        "`last_sync_error` TEXT, `device_id` TEXT NOT NULL, PRIMARY KEY(`localId`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_user_id_entry_date` ON `diary_entries` (`user_id`, `entry_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_user_id_updated_at` ON `diary_entries` (`user_id`, `updated_at`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `diary_photos` (" +
                        "`localId` TEXT NOT NULL, `diary_entry_local_id` TEXT NOT NULL, " +
                        "`local_path` TEXT NOT NULL, `remote_path` TEXT, `mime_type` TEXT NOT NULL, " +
                        "`width` INTEGER NOT NULL, `height` INTEGER NOT NULL, `byte_size` INTEGER NOT NULL, " +
                        "`remote_id` TEXT, `user_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_status` TEXT NOT NULL, " +
                        "`last_sync_error` TEXT, `device_id` TEXT NOT NULL, PRIMARY KEY(`localId`), " +
                        "FOREIGN KEY(`diary_entry_local_id`) REFERENCES `diary_entries`(`localId`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_diary_photos_diary_entry_local_id` " +
                        "ON `diary_photos` (`diary_entry_local_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_diary_photos_user_id_updated_at` " +
                        "ON `diary_photos` (`user_id`, `updated_at`)",
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `self_care_routines` (" +
                        "`localId` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, " +
                        "`category` TEXT NOT NULL, `scheduledTimeMinutes` INTEGER, `repeatDays` TEXT NOT NULL, " +
                        "`checklist` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `completionDates` TEXT NOT NULL, " +
                        "`remote_id` TEXT, `user_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `sync_status` TEXT NOT NULL, " +
                        "`last_sync_error` TEXT, `device_id` TEXT NOT NULL, PRIMARY KEY(`localId`))",
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
        )
    }
}
