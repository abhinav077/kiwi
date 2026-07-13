package com.abhinavsirohi.kiwi.feature.calendar

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewSubtask
import com.abhinavsirohi.kiwi.domain.model.NewTask
import com.abhinavsirohi.kiwi.domain.model.PlannerSyncState
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import com.abhinavsirohi.kiwi.domain.repository.TaskRepository
import com.abhinavsirohi.kiwi.domain.usecase.task.CreateTask
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveTasks
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun monthNavigation_andDateSelection_supportPastAndFutureDates() = runTest {
        val initialDate = LocalDate.of(2026, 7, 14)
        val viewModel = CalendarViewModel(ObserveTasks(FakeCalendarTaskRepository()), CreateTask(FakeCalendarTaskRepository()), dispatcher, initialDate)

        viewModel.showPreviousMonth()
        assertEquals(YearMonth.of(2026, 6), viewModel.state.value.displayedMonth)

        viewModel.showNextMonth()
        viewModel.showNextMonth()
        assertEquals(YearMonth.of(2026, 8), viewModel.state.value.displayedMonth)

        val futureDate = LocalDate.of(2027, 2, 3)
        viewModel.selectDate(futureDate)
        assertEquals(futureDate, viewModel.state.value.selectedDate)
        assertEquals(YearMonth.of(2027, 2), viewModel.state.value.displayedMonth)
    }

    @Test
    fun savingTask_usesTheSelectedDate() = runTest {
        val repository = FakeCalendarTaskRepository()
        val selectedDate = LocalDate.of(2026, 12, 25)
        val viewModel = CalendarViewModel(ObserveTasks(repository), CreateTask(repository), dispatcher, selectedDate)
        advanceUntilIdle()

        viewModel.startCreatingTask()
        viewModel.updateTaskTitle("Wrap gifts")
        viewModel.saveTask()
        advanceUntilIdle()

        assertEquals("Wrap gifts", repository.createdTask?.title)
        assertEquals("2026-12-25", repository.createdTask?.scheduledDate)
    }

    @Test
    fun timeline_ordersTimedTasks_thenUntimedTasks() {
        val tasks = listOf(
            task("Untimed", null),
            task("Late", 17 * 60),
            task("Early", 9 * 60),
        )

        val entries = buildTimelineItems(tasks).filterIsInstance<CalendarTimelineItem.TaskEntry>()

        assertEquals(listOf("Early", "Late", "Untimed"), entries.map { it.task.title })
    }

    @Test
    fun timeline_insertsBreaksForGapsOfAtLeastOneHour() {
        val entries = buildTimelineItems(
            listOf(task("Morning focus", 9 * 60), task("Afternoon focus", 11 * 60)),
        )

        assertEquals(
            CalendarTimelineItem.BreakEntry(9 * 60, 11 * 60),
            entries[1],
        )
    }

    private fun task(title: String, scheduledTimeMinutes: Int?): Task = Task(
        localId = title,
        title = title,
        scheduledDate = "2026-07-14",
        scheduledTimeMinutes = scheduledTimeMinutes,
        category = TaskCategory.Personal,
        metadata = RecordMetadata(userId = "user-1", createdAt = 1L, updatedAt = 1L, deviceId = "device-1"),
    )
}

private class FakeCalendarTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<AppResult<List<Task>>>(AppResult.Success(emptyList()))
    var createdTask: NewTask? = null

    override fun observeTasks(): Flow<AppResult<List<Task>>> = tasks
    override fun observeSubtasks(taskLocalId: String): Flow<AppResult<List<Subtask>>> = emptyFlow()
    override fun observePlannerSyncState(): Flow<PlannerSyncState> = flowOf(PlannerSyncState())
    override suspend fun createTask(task: NewTask): AppResult<Task> {
        createdTask = task
        return AppResult.Success(Task("task-1", task.title, scheduledDate = task.scheduledDate, metadata = RecordMetadata(userId = "user-1", createdAt = 1L, updatedAt = 1L, deviceId = "device-1")))
    }
    override suspend fun createSubtask(subtask: NewSubtask): AppResult<Subtask> = AppResult.Failure(UnsupportedOperationException())
    override suspend fun saveTask(task: Task): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun saveSubtask(subtask: Subtask): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun tombstoneTask(localId: String, deletedAt: Long): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun tombstoneSubtask(localId: String, deletedAt: Long): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun moveSubtask(localId: String, direction: Int): AppResult<Unit> = AppResult.Success(Unit)
}
