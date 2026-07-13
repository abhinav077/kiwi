package com.abhinavsirohi.kiwi.core.database

import androidx.room.TypeConverter
import com.abhinavsirohi.kiwi.core.sync.SyncOperation
import com.abhinavsirohi.kiwi.core.sync.SyncQueueState
import com.abhinavsirohi.kiwi.core.sync.SyncRecordType

class KiwiTypeConverters {
    @TypeConverter
    fun syncStatusToString(value: SyncStatus): String = value.name

    @TypeConverter
    fun stringToSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun syncRecordTypeToString(value: SyncRecordType): String = value.name

    @TypeConverter
    fun stringToSyncRecordType(value: String): SyncRecordType = SyncRecordType.valueOf(value)

    @TypeConverter
    fun syncOperationToString(value: SyncOperation): String = value.name

    @TypeConverter
    fun stringToSyncOperation(value: String): SyncOperation = SyncOperation.valueOf(value)

    @TypeConverter
    fun syncQueueStateToString(value: SyncQueueState): String = value.name

    @TypeConverter
    fun stringToSyncQueueState(value: String): SyncQueueState = SyncQueueState.valueOf(value)
}
