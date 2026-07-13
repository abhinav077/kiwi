package com.abhinavsirohi.kiwi.feature.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewCycleRecord
import com.abhinavsirohi.kiwi.domain.model.NewWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import com.abhinavsirohi.kiwi.domain.model.WellnessAnalytics
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveHealthAlertEpisodes
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ReconcileHealthAlerts
import com.abhinavsirohi.kiwi.domain.usecase.wellness.AcknowledgeHealthAlert
import com.abhinavsirohi.kiwi.domain.usecase.wellness.DismissHealthAlert
import com.abhinavsirohi.kiwi.domain.usecase.wellness.CalculateWellnessAnalytics
import com.abhinavsirohi.kiwi.domain.usecase.wellness.CreateCycleRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.CreateWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveCycleRecords
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveWellnessDailyRecords
import com.abhinavsirohi.kiwi.domain.usecase.wellness.SaveCycleRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.SaveWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.TombstoneCycleRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.TombstoneWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.TombstoneWellnessRecordParameters
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class WellnessUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val cycleRecords: List<CycleRecord> = emptyList(),
    val dailyRecords: List<WellnessDailyRecord> = emptyList(),
    val analytics: WellnessAnalytics = WellnessAnalytics(),
    val healthAlerts: List<HealthAlertEpisode> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val dailyDraft: WellnessDailyDraft? = null,
    val cycleDraft: CycleRecordDraft? = null,
    val pendingDeletion: WellnessDeletion? = null,
    val isSaving: Boolean = false,
)

data class WellnessDailyDraft(
    val existing: WellnessDailyRecord? = null,
    val recordDate: String,
    val flow: WellnessFlow? = null,
    val painLevel: String = "",
    val crampsLevel: String = "",
    val symptoms: String = "",
    val mood: String = "",
    val energyLevel: String = "",
    val sleepMinutes: String = "",
    val notes: String = "",
    val exercise: String = "",
    val selfCareMedicationNotes: String = "",
)

data class CycleRecordDraft(
    val existing: CycleRecord? = null,
    val startDate: String,
    val endDate: String = "",
)

sealed interface WellnessDeletion {
    data class Daily(val record: WellnessDailyRecord) : WellnessDeletion
    data class Cycle(val record: CycleRecord) : WellnessDeletion
}

class WellnessViewModel(
    private val observeCycles: ObserveCycleRecords,
    private val observeDailyRecords: ObserveWellnessDailyRecords,
    private val createCycle: CreateCycleRecord,
    private val saveCycle: SaveCycleRecord,
    private val deleteCycle: TombstoneCycleRecord,
    private val createDaily: CreateWellnessDailyRecord,
    private val saveDaily: SaveWellnessDailyRecord,
    private val deleteDaily: TombstoneWellnessDailyRecord,
    private val calculateAnalytics: CalculateWellnessAnalytics = CalculateWellnessAnalytics(),
    private val observeHealthAlerts: ObserveHealthAlertEpisodes? = null,
    private val reconcileHealthAlerts: ReconcileHealthAlerts? = null,
    private val acknowledgeHealthAlert: AcknowledgeHealthAlert? = null,
    private val dismissHealthAlert: DismissHealthAlert? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val today: () -> LocalDate = LocalDate::now,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        WellnessUiState(displayedMonth = YearMonth.from(today()), selectedDate = today()),
    )
    val state: StateFlow<WellnessUiState> = mutableState.asStateFlow()

    init {
        observeRecords()
        observeAlerts()
    }

    fun selectDate(date: LocalDate) {
        mutableState.value = mutableState.value.copy(
            selectedDate = date,
            displayedMonth = YearMonth.from(date),
            message = null,
        )
    }

    fun previousMonth() {
        mutableState.value = mutableState.value.copy(displayedMonth = mutableState.value.displayedMonth.minusMonths(1))
    }

    fun nextMonth() {
        mutableState.value = mutableState.value.copy(displayedMonth = mutableState.value.displayedMonth.plusMonths(1))
    }

    fun startQuickLog() {
        val selected = mutableState.value.selectedDate.toString()
        val existing = mutableState.value.dailyRecords.firstOrNull { it.recordDate == selected }
        mutableState.value = mutableState.value.copy(dailyDraft = existing?.toDraft() ?: WellnessDailyDraft(recordDate = selected))
    }

    fun editDaily(record: WellnessDailyRecord) {
        mutableState.value = mutableState.value.copy(dailyDraft = record.toDraft(), message = null)
    }

    fun updateDailyDraft(draft: WellnessDailyDraft) {
        mutableState.value = mutableState.value.copy(dailyDraft = draft, message = null)
    }

    fun dismissDailyEditor() {
        if (!mutableState.value.isSaving) mutableState.value = mutableState.value.copy(dailyDraft = null)
    }

    fun startCycleRecord() {
        mutableState.value = mutableState.value.copy(cycleDraft = CycleRecordDraft(startDate = mutableState.value.selectedDate.toString()))
    }

    fun editCycle(record: CycleRecord) {
        mutableState.value = mutableState.value.copy(cycleDraft = CycleRecordDraft(record, record.startDate, record.endDate.orEmpty()))
    }

    fun updateCycleDraft(draft: CycleRecordDraft) {
        mutableState.value = mutableState.value.copy(cycleDraft = draft, message = null)
    }

    fun dismissCycleEditor() {
        if (!mutableState.value.isSaving) mutableState.value = mutableState.value.copy(cycleDraft = null)
    }

    fun saveDailyRecord() {
        val draft = mutableState.value.dailyDraft ?: return
        if (mutableState.value.isSaving) return
        mutableState.value = mutableState.value.copy(isSaving = true, message = null)
        viewModelScope.launch(dispatcher) {
            val result = draft.existing?.let { existing ->
                saveDaily(existing.copy(
                    recordDate = draft.recordDate,
                    flow = draft.flow,
                    painLevel = draft.painLevel.toNullableInt(),
                    crampsLevel = draft.crampsLevel.toNullableInt(),
                    symptoms = draft.symptoms.toSymptoms(),
                    mood = draft.mood,
                    energyLevel = draft.energyLevel.toNullableInt(),
                    sleepMinutes = draft.sleepMinutes.toNullableInt(),
                    notes = draft.notes,
                    exercise = draft.exercise,
                    selfCareMedicationNotes = draft.selfCareMedicationNotes,
                ))
            } ?: createDaily(NewWellnessDailyRecord(
                recordDate = draft.recordDate,
                flow = draft.flow,
                painLevel = draft.painLevel.toNullableInt(),
                crampsLevel = draft.crampsLevel.toNullableInt(),
                symptoms = draft.symptoms.toSymptoms(),
                mood = draft.mood,
                energyLevel = draft.energyLevel.toNullableInt(),
                sleepMinutes = draft.sleepMinutes.toNullableInt(),
                notes = draft.notes,
                exercise = draft.exercise,
                selfCareMedicationNotes = draft.selfCareMedicationNotes,
            ))
            completeSave(result)
        }
    }

    fun saveCycleRecord() {
        val draft = mutableState.value.cycleDraft ?: return
        if (mutableState.value.isSaving) return
        mutableState.value = mutableState.value.copy(isSaving = true, message = null)
        viewModelScope.launch(dispatcher) {
            val result = draft.existing?.let { saveCycle(it.copy(startDate = draft.startDate, endDate = draft.endDate.ifBlank { null })) }
                ?: createCycle(NewCycleRecord(draft.startDate, draft.endDate.ifBlank { null }))
            completeSave(result)
        }
    }

    fun requestDeleteDaily(record: WellnessDailyRecord) {
        mutableState.value = mutableState.value.copy(pendingDeletion = WellnessDeletion.Daily(record))
    }

    fun requestDeleteCycle(record: CycleRecord) {
        mutableState.value = mutableState.value.copy(pendingDeletion = WellnessDeletion.Cycle(record))
    }

    fun dismissDeletion() {
        if (!mutableState.value.isSaving) mutableState.value = mutableState.value.copy(pendingDeletion = null)
    }

    fun confirmDeletion() {
        val deletion = mutableState.value.pendingDeletion ?: return
        if (mutableState.value.isSaving) return
        mutableState.value = mutableState.value.copy(isSaving = true, message = null)
        viewModelScope.launch(dispatcher) {
            val result = when (deletion) {
                is WellnessDeletion.Cycle -> deleteCycle(TombstoneWellnessRecordParameters(deletion.record.localId, now()))
                is WellnessDeletion.Daily -> deleteDaily(TombstoneWellnessRecordParameters(deletion.record.localId, now()))
            }
            when (result) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(
                    isSaving = false,
                    pendingDeletion = null,
                    message = "Entry removed from this device and queued to sync.",
                )
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(
                    isSaving = false,
                    message = "Kiwi couldn’t remove that entry. Please try again.",
                )
            }
        }
    }

    fun acknowledgeAlert(localId: String) = updateAlert(localId, acknowledgeHealthAlert)

    fun dismissAlert(localId: String) = updateAlert(localId, dismissHealthAlert)

    private fun observeRecords() {
        viewModelScope.launch(dispatcher) {
            observeCycles().collectLatest { result ->
                mutableState.value = when (result) {
                    is AppResult.Success -> mutableState.value.copy(cycleRecords = result.value, isLoading = false).withAnalytics().also { reconcileAlerts(it) }
                    is AppResult.Failure -> mutableState.value.copy(isLoading = false, message = "Kiwi couldn’t load cycle records.")
                }
            }
        }
        viewModelScope.launch(dispatcher) {
            observeDailyRecords().collectLatest { result ->
                mutableState.value = when (result) {
                    is AppResult.Success -> mutableState.value.copy(dailyRecords = result.value, isLoading = false).withAnalytics().also { reconcileAlerts(it) }
                    is AppResult.Failure -> mutableState.value.copy(isLoading = false, message = "Kiwi couldn’t load daily records.")
                }
            }
        }
    }

    private fun observeAlerts() {
        val observe = observeHealthAlerts ?: return
        viewModelScope.launch(dispatcher) {
            observe().collectLatest { result ->
                if (result is AppResult.Success) mutableState.value = mutableState.value.copy(healthAlerts = result.value)
            }
        }
    }

    private fun reconcileAlerts(state: WellnessUiState) {
        val reconcile = reconcileHealthAlerts ?: return
        viewModelScope.launch(dispatcher) { reconcile(state.cycleRecords, state.dailyRecords) }
    }

    private fun updateAlert(localId: String, action: Any?) {
        viewModelScope.launch(dispatcher) {
            val result = when (action) {
                is AcknowledgeHealthAlert -> action(localId)
                is DismissHealthAlert -> action(localId)
                else -> return@launch
            }
            if (result is AppResult.Failure) {
                mutableState.value = mutableState.value.copy(message = "Kiwi couldn’t update that wellness note.")
            }
        }
    }

    private fun completeSave(result: AppResult<*>) {
        mutableState.value = when (result) {
            is AppResult.Success -> mutableState.value.copy(
                isSaving = false,
                dailyDraft = null,
                cycleDraft = null,
                message = "Saved on this device. Sync will happen when available.",
            )
            is AppResult.Failure -> mutableState.value.copy(
                isSaving = false,
                message = "Kiwi couldn’t save that entry. Please check the values and try again.",
            )
        }
    }

    private fun WellnessUiState.withAnalytics() = copy(
        analytics = calculateAnalytics(cycleRecords, dailyRecords),
    )

    private fun WellnessDailyRecord.toDraft() = WellnessDailyDraft(
        existing = this,
        recordDate = recordDate,
        flow = flow,
        painLevel = painLevel?.toString().orEmpty(),
        crampsLevel = crampsLevel?.toString().orEmpty(),
        symptoms = symptoms.joinToString(", "),
        mood = mood.orEmpty(),
        energyLevel = energyLevel?.toString().orEmpty(),
        sleepMinutes = sleepMinutes?.toString().orEmpty(),
        notes = notes.orEmpty(),
        exercise = exercise.orEmpty(),
        selfCareMedicationNotes = selfCareMedicationNotes.orEmpty(),
    )

    private fun String.toNullableInt(): Int? = trim().takeIf(String::isNotEmpty)?.toIntOrNull()

    private fun String.toSymptoms(): List<String> = split(',').map(String::trim).filter(String::isNotEmpty)

    class Factory(
        private val observeCycles: ObserveCycleRecords,
        private val observeDailyRecords: ObserveWellnessDailyRecords,
        private val createCycle: CreateCycleRecord,
        private val saveCycle: SaveCycleRecord,
        private val deleteCycle: TombstoneCycleRecord,
        private val createDaily: CreateWellnessDailyRecord,
        private val saveDaily: SaveWellnessDailyRecord,
        private val deleteDaily: TombstoneWellnessDailyRecord,
        private val calculateAnalytics: CalculateWellnessAnalytics = CalculateWellnessAnalytics(),
        private val observeHealthAlerts: ObserveHealthAlertEpisodes? = null,
        private val reconcileHealthAlerts: ReconcileHealthAlerts? = null,
        private val acknowledgeHealthAlert: AcknowledgeHealthAlert? = null,
        private val dismissHealthAlert: DismissHealthAlert? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WellnessViewModel::class.java))
            return WellnessViewModel(
                observeCycles, observeDailyRecords, createCycle, saveCycle, deleteCycle,
                createDaily, saveDaily, deleteDaily, calculateAnalytics,
                observeHealthAlerts, reconcileHealthAlerts, acknowledgeHealthAlert, dismissHealthAlert,
            ) as T
        }
    }
}
