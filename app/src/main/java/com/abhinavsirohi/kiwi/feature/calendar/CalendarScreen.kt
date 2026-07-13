package com.abhinavsirohi.kiwi.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.design.KiwiButton
import com.abhinavsirohi.kiwi.core.design.KiwiCard
import com.abhinavsirohi.kiwi.data.repository.RoomTaskRepository
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.usecase.task.CreateTask
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveTasks
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiButter
import com.abhinavsirohi.kiwi.ui.theme.KiwiForest
import com.abhinavsirohi.kiwi.ui.theme.KiwiLavender
import com.abhinavsirohi.kiwi.ui.theme.KiwiPowderBlue
import com.abhinavsirohi.kiwi.ui.theme.KiwiSage
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarRoute(modifier: Modifier = Modifier) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as KiwiApplication
    val repository = RoomTaskRepository(
        application.database,
        application.supabaseClient,
        application.deviceId,
        application.taskReminderScheduler,
    )
    val calendarViewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.Factory(ObserveTasks(repository), CreateTask(repository)),
    )
    val state by calendarViewModel.state.collectAsState()
    CalendarScreen(
        state = state,
        onPreviousMonth = calendarViewModel::showPreviousMonth,
        onNextMonth = calendarViewModel::showNextMonth,
        onDateSelected = calendarViewModel::selectDate,
        onAddTask = calendarViewModel::startCreatingTask,
        onTaskTitleChanged = calendarViewModel::updateTaskTitle,
        onSaveTask = calendarViewModel::saveTask,
        onDismissEditor = calendarViewModel::dismissTaskEditor,
        modifier = modifier,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onAddTask: () -> Unit,
    onTaskTitleChanged: (String) -> Unit,
    onSaveTask: () -> Unit,
    onDismissEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = KiwiSpacing.lg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = KiwiSpacing.xl, bottom = KiwiSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md),
        ) {
            item {
                Text("Calendar", style = MaterialTheme.typography.displayLarge, color = KiwiCharcoal)
                Text("Choose a day and leave room for what matters.", style = MaterialTheme.typography.bodyLarge, color = KiwiWarmGray)
            }
            item {
                KiwiCard(Modifier.fillMaxWidth()) {
                    MonthHeader(state.displayedMonth, onPreviousMonth, onNextMonth)
                    Spacer(Modifier.height(KiwiSpacing.sm))
                    CalendarGrid(state.displayedMonth, state.selectedDate, onDateSelected)
                }
            }
            item {
                KiwiCard(Modifier.fillMaxWidth()) {
                    Text(
                        state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.titleLarge,
                        color = KiwiCharcoal,
                    )
                    Spacer(Modifier.height(KiwiSpacing.xs))
                    KiwiButton(onClick = onAddTask) { Text("Add a task") }
                }
            }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.tasks.isEmpty()) {
                item {
                    Text("Nothing planned for this day yet.", style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
                }
            } else {
                items(buildTimelineItems(state.tasks)) { item ->
                    when (item) {
                        is CalendarTimelineItem.TaskEntry -> TimelineTaskCard(item.task)
                        is CalendarTimelineItem.BreakEntry -> TimelineBreakCard(item.startMinutes, item.endMinutes)
                    }
                }
            }
        }
    }
    if (state.isCreatingTask) {
        AlertDialog(
            onDismissRequest = onDismissEditor,
            title = { Text("Add a task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
                    Text("This task will be planned for ${state.selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}.")
                    OutlinedTextField(
                        value = state.taskTitle,
                        onValueChange = onTaskTitleChanged,
                        label = { Text("Task title") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = { TextButton(onClick = onSaveTask, enabled = !state.isSaving) { Text(if (state.isSaving) "Saving…" else "Save") } },
            dismissButton = { OutlinedButton(onClick = onDismissEditor, enabled = !state.isSaving) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TimelineTaskCard(task: Task) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(task.category.timelineColor(), MaterialTheme.shapes.small),
            )
            Spacer(Modifier.size(KiwiSpacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted) KiwiWarmGray else KiwiCharcoal,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                )
                Text(
                    task.scheduledTimeMinutes?.toTimeLabel() ?: "Any time",
                    style = MaterialTheme.typography.labelSmall,
                    color = KiwiWarmGray,
                )
            }
            Text(
                if (task.isCompleted) "Completed" else task.category.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (task.isCompleted) KiwiWarmGray else task.category.timelineColor(),
            )
        }
        task.description?.takeIf(String::isNotBlank)?.let { description ->
            Spacer(Modifier.height(KiwiSpacing.xs))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
        }
    }
}

@Composable
private fun TimelineBreakCard(startMinutes: Int, endMinutes: Int) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BREAK", style = MaterialTheme.typography.labelSmall, color = KiwiForest)
            Spacer(Modifier.size(KiwiSpacing.sm))
            Text(
                "${startMinutes.toTimeLabel()} – ${endMinutes.toTimeLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = KiwiWarmGray,
            )
            Spacer(Modifier.weight(1f))
            Text("Room to breathe", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
        }
    }
}

private fun Int.toTimeLabel(): String = "%02d:%02d".format(this / 60, this % 60)

private fun com.abhinavsirohi.kiwi.domain.model.TaskCategory.timelineColor(): Color = when (this) {
    com.abhinavsirohi.kiwi.domain.model.TaskCategory.Personal -> KiwiLavender
    com.abhinavsirohi.kiwi.domain.model.TaskCategory.Work -> KiwiPowderBlue
    com.abhinavsirohi.kiwi.domain.model.TaskCategory.Wellness -> KiwiSage
    com.abhinavsirohi.kiwi.domain.model.TaskCategory.Home -> KiwiButter
}

@Composable
private fun MonthHeader(month: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = onPreviousMonth,
            modifier = Modifier.semantics { contentDescription = "Previous month" },
        ) { Text("‹") }
        Text(
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = KiwiCharcoal,
        )
        TextButton(
            onClick = onNextMonth,
            modifier = Modifier.semantics { contentDescription = "Next month" },
        ) { Text("›") }
    }
}

@Composable
private fun CalendarGrid(month: YearMonth, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
        }
    }
    val blanks = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells = List<LocalDate?>(blanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                Box(Modifier.weight(1f).size(44.dp), contentAlignment = Alignment.Center) {
                    if (date != null) {
                        val selected = date == selectedDate
                        Text(
                            text = date.dayOfMonth.toString(),
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (selected) KiwiForest else Color.Transparent, MaterialTheme.shapes.small)
                                .clickable { onDateSelected(date) }
                                .semantics { contentDescription = date.toString() }
                                .padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                            color = if (selected) Color.White else KiwiCharcoal,
                        )
                    }
                }
            }
            repeat(7 - week.size) { Spacer(Modifier.weight(1f).size(44.dp)) }
        }
    }
}
