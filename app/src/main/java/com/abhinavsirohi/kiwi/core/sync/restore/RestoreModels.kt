package com.abhinavsirohi.kiwi.core.sync.restore

import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.Profile
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord

data class RestoreSnapshot(
    val profile: Profile? = null,
    val tasks: List<Task>,
    val subtasks: List<Subtask>,
    val cycleRecords: List<CycleRecord> = emptyList(),
    val wellnessDailyRecords: List<WellnessDailyRecord> = emptyList(),
    val healthAlertEpisodes: List<HealthAlertEpisode> = emptyList(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val selfCareRoutines: List<SelfCareRoutine> = emptyList(),
    val taskPostponements: List<TaskPostponement> = emptyList(),
    val weeklyReflections: List<WeeklyReflection> = emptyList(),
)

sealed interface RestoreState {
    data object Restoring : RestoreState
    data object NotRequired : RestoreState
    data class Completed(val restoredTasks: Int, val restoredSubtasks: Int) : RestoreState
    data class RetryableFailure(val error: RemoteError) : RestoreState
    data class Rejected(val error: RemoteError) : RestoreState
}

fun interface RestoreRemoteDataSource {
    suspend fun fetchSnapshot(userId: String): RemoteResult<RestoreSnapshot>
}

fun interface RestoreDatabaseWriter {
    suspend fun applySnapshot(snapshot: RestoreSnapshot): RestoreWriteResult
}

data class RestoreWriteResult(
    val restoredTasks: Int,
    val restoredSubtasks: Int,
    val restoredRecords: Int = restoredTasks + restoredSubtasks,
)

interface RestoreStateStore {
    suspend fun isComplete(userId: String): Boolean
    suspend fun markComplete(userId: String)
}

fun interface ReminderRecreator {
    suspend fun recreateAfterRestore(snapshot: RestoreSnapshot)
}
