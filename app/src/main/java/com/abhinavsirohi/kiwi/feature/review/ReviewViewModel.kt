package com.abhinavsirohi.kiwi.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.PlannerAnalytics
import com.abhinavsirohi.kiwi.domain.model.NewWeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.WellnessAnalytics
import com.abhinavsirohi.kiwi.domain.usecase.task.CalculatePlannerAnalytics
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveTasks
import com.abhinavsirohi.kiwi.domain.usecase.wellness.CalculateWellnessAnalytics
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveCycleRecords
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveWellnessDailyRecords
import com.abhinavsirohi.kiwi.domain.usecase.review.ObserveTaskPostponements
import com.abhinavsirohi.kiwi.domain.usecase.review.ObserveWeeklyReflections
import com.abhinavsirohi.kiwi.domain.usecase.review.SaveWeeklyReflection
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ReviewUiState(
    val planner: PlannerAnalytics = PlannerAnalytics(),
    val wellness: WellnessAnalytics = WellnessAnalytics(),
    val postponements: List<TaskPostponement> = emptyList(),
    val reflections: List<WeeklyReflection> = emptyList(),
    val reflectionDraft: String? = null,
    val isSavingReflection: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null,
)

class ReviewViewModel(
    private val observeTasks: ObserveTasks,
    private val observeCycles: ObserveCycleRecords,
    private val observeDailyRecords: ObserveWellnessDailyRecords,
    private val observePostponements: ObserveTaskPostponements,
    private val observeReflections: ObserveWeeklyReflections,
    private val saveReflection: SaveWeeklyReflection,
    private val calculatePlanner: CalculatePlannerAnalytics = CalculatePlannerAnalytics(),
    private val calculateWellness: CalculateWellnessAnalytics = CalculateWellnessAnalytics(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            combine(
                observeTasks(),
                observeCycles(),
                observeDailyRecords(),
                observePostponements(),
                observeReflections(),
            ) { tasks, cycles, daily, postponements, reflections ->
                ReviewSources(tasks, cycles, daily, postponements, reflections)
            }.collect { sources ->
                val failure = listOf(
                    sources.tasks,
                    sources.cycles,
                    sources.daily,
                    sources.postponements,
                    sources.reflections,
                ).filterIsInstance<AppResult.Failure>().firstOrNull()
                if (failure != null) {
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        message = "Kiwi couldn’t prepare your review. Please try again.",
                    )
                } else {
                    val taskValues = (sources.tasks as AppResult.Success).value
                    val cycleValues = (sources.cycles as AppResult.Success).value
                    val dailyValues = (sources.daily as AppResult.Success).value
                    mutableState.value = mutableState.value.copy(
                        planner = calculatePlanner(taskValues, today()),
                        wellness = calculateWellness(cycleValues, dailyValues),
                        postponements = (sources.postponements as AppResult.Success).value,
                        reflections = (sources.reflections as AppResult.Success).value,
                        isLoading = false,
                        message = null,
                    )
                }
            }
        }
    }

    fun startReflection() {
        val weekStart = currentWeekStart().toString()
        val existing = mutableState.value.reflections.firstOrNull { it.weekStart == weekStart }
        mutableState.value = mutableState.value.copy(reflectionDraft = existing?.content.orEmpty(), message = null)
    }

    fun updateReflectionDraft(content: String) {
        if (mutableState.value.reflectionDraft != null) {
            mutableState.value = mutableState.value.copy(reflectionDraft = content, message = null)
        }
    }

    fun dismissReflection() {
        if (!mutableState.value.isSavingReflection) {
            mutableState.value = mutableState.value.copy(reflectionDraft = null, message = null)
        }
    }

    fun saveReflection() {
        val content = mutableState.value.reflectionDraft ?: return
        if (content.isBlank()) {
            mutableState.value = mutableState.value.copy(message = "Write a few words before saving.")
            return
        }
        mutableState.value = mutableState.value.copy(isSavingReflection = true, message = null)
        viewModelScope.launch(dispatcher) {
            when (saveReflection(NewWeeklyReflection(currentWeekStart().toString(), content))) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(
                    reflectionDraft = null,
                    isSavingReflection = false,
                    message = "Reflection saved on this device.",
                )
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(
                    isSavingReflection = false,
                    message = "Kiwi couldn’t save your reflection. Please try again.",
                )
            }
        }
    }

    private fun currentWeekStart(): LocalDate =
        today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    class Factory(
        private val observeTasks: ObserveTasks,
        private val observeCycles: ObserveCycleRecords,
        private val observeDailyRecords: ObserveWellnessDailyRecords,
        private val observePostponements: ObserveTaskPostponements,
        private val observeReflections: ObserveWeeklyReflections,
        private val saveReflection: SaveWeeklyReflection,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReviewViewModel::class.java))
            return ReviewViewModel(
                observeTasks,
                observeCycles,
                observeDailyRecords,
                observePostponements,
                observeReflections,
                saveReflection,
            ) as T
        }
    }
}

private data class ReviewSources(
    val tasks: AppResult<List<com.abhinavsirohi.kiwi.domain.model.Task>>,
    val cycles: AppResult<List<com.abhinavsirohi.kiwi.domain.model.CycleRecord>>,
    val daily: AppResult<List<com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord>>,
    val postponements: AppResult<List<TaskPostponement>>,
    val reflections: AppResult<List<WeeklyReflection>>,
)
