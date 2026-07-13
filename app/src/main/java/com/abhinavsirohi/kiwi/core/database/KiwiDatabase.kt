package com.abhinavsirohi.kiwi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abhinavsirohi.kiwi.data.local.dao.PendingChangeDao
import com.abhinavsirohi.kiwi.data.local.dao.TaskDao
import com.abhinavsirohi.kiwi.data.local.entity.PendingChangeEntity
import com.abhinavsirohi.kiwi.data.local.entity.SubtaskEntity
import com.abhinavsirohi.kiwi.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, SubtaskEntity::class, PendingChangeEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(KiwiTypeConverters::class)
abstract class KiwiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun pendingChangeDao(): PendingChangeDao

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

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
