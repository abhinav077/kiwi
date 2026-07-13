package com.abhinavsirohi.kiwi.domain.usecase.wellness

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewCycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.repository.WellnessRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

class ObserveCycleRecords(
    private val repository: WellnessRepository,
) {
    operator fun invoke(): Flow<AppResult<List<CycleRecord>>> = repository.observeCycleRecords()
}

class ObserveWellnessDailyRecords(
    private val repository: WellnessRepository,
) {
    operator fun invoke(): Flow<AppResult<List<WellnessDailyRecord>>> = repository.observeDailyRecords()
}

class CreateCycleRecord(
    private val repository: WellnessRepository,
) : UseCase<NewCycleRecord, AppResult<CycleRecord>> {
    override suspend fun invoke(parameters: NewCycleRecord): AppResult<CycleRecord> =
        repository.createCycleRecord(parameters)
}

class SaveCycleRecord(
    private val repository: WellnessRepository,
) : UseCase<CycleRecord, AppResult<Unit>> {
    override suspend fun invoke(parameters: CycleRecord): AppResult<Unit> = repository.saveCycleRecord(parameters)
}

data class TombstoneWellnessRecordParameters(
    val localId: String,
    val deletedAt: Long,
)

class TombstoneCycleRecord(
    private val repository: WellnessRepository,
) : UseCase<TombstoneWellnessRecordParameters, AppResult<Unit>> {
    override suspend fun invoke(parameters: TombstoneWellnessRecordParameters): AppResult<Unit> =
        repository.tombstoneCycleRecord(parameters.localId, parameters.deletedAt)
}

class CreateWellnessDailyRecord(
    private val repository: WellnessRepository,
) : UseCase<NewWellnessDailyRecord, AppResult<WellnessDailyRecord>> {
    override suspend fun invoke(parameters: NewWellnessDailyRecord): AppResult<WellnessDailyRecord> =
        repository.createDailyRecord(parameters)
}

class SaveWellnessDailyRecord(
    private val repository: WellnessRepository,
) : UseCase<WellnessDailyRecord, AppResult<Unit>> {
    override suspend fun invoke(parameters: WellnessDailyRecord): AppResult<Unit> =
        repository.saveDailyRecord(parameters)
}

class TombstoneWellnessDailyRecord(
    private val repository: WellnessRepository,
) : UseCase<TombstoneWellnessRecordParameters, AppResult<Unit>> {
    override suspend fun invoke(parameters: TombstoneWellnessRecordParameters): AppResult<Unit> =
        repository.tombstoneDailyRecord(parameters.localId, parameters.deletedAt)
}
