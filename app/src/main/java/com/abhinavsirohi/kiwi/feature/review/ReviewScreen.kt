package com.abhinavsirohi.kiwi.feature.review

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.design.KiwiCard
import com.abhinavsirohi.kiwi.core.design.KiwiButton
import com.abhinavsirohi.kiwi.data.repository.RoomReviewRepository
import com.abhinavsirohi.kiwi.data.repository.RoomTaskRepository
import com.abhinavsirohi.kiwi.data.repository.RoomWellnessRepository
import com.abhinavsirohi.kiwi.domain.model.PlannerAnalytics
import com.abhinavsirohi.kiwi.domain.model.PlannerDaySummary
import com.abhinavsirohi.kiwi.domain.model.PlannerWeekSummary
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import com.abhinavsirohi.kiwi.domain.model.TaskPostponement
import com.abhinavsirohi.kiwi.domain.model.WeeklyReflection
import com.abhinavsirohi.kiwi.domain.model.WellnessAnalytics
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveTasks
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveCycleRecords
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveWellnessDailyRecords
import com.abhinavsirohi.kiwi.domain.usecase.review.ObserveTaskPostponements
import com.abhinavsirohi.kiwi.domain.usecase.review.ObserveWeeklyReflections
import com.abhinavsirohi.kiwi.domain.usecase.review.SaveWeeklyReflection
import java.time.LocalDate
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray
import java.time.format.DateTimeFormatter

@Composable
fun ReviewRoute(modifier: Modifier = Modifier) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as KiwiApplication
    val taskRepository = RoomTaskRepository(application.database, application.supabaseClient, application.deviceId, application.taskReminderScheduler)
    val wellnessRepository = RoomWellnessRepository(application.database, application.supabaseClient, application.deviceId)
    val reviewRepository = RoomReviewRepository(application.database, application.supabaseClient, application.deviceId)
    val reviewViewModel: ReviewViewModel = viewModel(
        factory = ReviewViewModel.Factory(
            ObserveTasks(taskRepository),
            ObserveCycleRecords(wellnessRepository),
            ObserveWellnessDailyRecords(wellnessRepository),
            ObserveTaskPostponements(reviewRepository),
            ObserveWeeklyReflections(reviewRepository),
            SaveWeeklyReflection(reviewRepository),
        ),
    )
    val state by reviewViewModel.state.collectAsState()
    ReviewScreen(
        state = state,
        onWriteReflection = reviewViewModel::startReflection,
        onReflectionChanged = reviewViewModel::updateReflectionDraft,
        onSaveReflection = reviewViewModel::saveReflection,
        onDismissReflection = reviewViewModel::dismissReflection,
        modifier = modifier,
    )
}

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onWriteReflection: () -> Unit,
    onReflectionChanged: (String) -> Unit,
    onSaveReflection: () -> Unit,
    onDismissReflection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = KiwiSpacing.lg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = KiwiSpacing.xl, bottom = KiwiSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md),
        ) {
            item {
                Text("Review", style = MaterialTheme.typography.displayLarge, color = KiwiCharcoal)
                Text("A quiet look at what you recorded.", style = MaterialTheme.typography.bodyLarge, color = KiwiWarmGray)
            }
            if (state.isLoading) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            } else {
                state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                item { PlannerSummaryCard(state.planner, state.postponements.size) }
                item { RecentPatternCard(state.planner.recentDays) }
                item { WeeklyHistoryCard(state.planner.weeklySummaries) }
                item { ReflectionHistoryCard(state.reflections, onWriteReflection) }
                item { PostponementHistoryCard(state.postponements) }
                item { CategoryCard(state.planner) }
                item { WellnessSummaryCard(state.wellness) }
            }
        }
    }
    state.reflectionDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = onDismissReflection,
            title = { Text("Weekly reflection") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onReflectionChanged,
                    label = { Text("What would you like to remember?") },
                    minLines = 4,
                    enabled = !state.isSavingReflection,
                )
            },
            confirmButton = {
                TextButton(onClick = onSaveReflection, enabled = !state.isSavingReflection) { Text("Save reflection") }
            },
            dismissButton = {
                TextButton(onClick = onDismissReflection, enabled = !state.isSavingReflection) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PlannerSummaryCard(analytics: PlannerAnalytics, postponedCount: Int) {
    KiwiCard(Modifier.fillMaxWidth().animateContentSize()) {
        Text("Planner pulse", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        Text("${analytics.completionPercentage}% completed", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text("${analytics.completedTasks} completed of ${analytics.plannedTasks} planned through today", color = KiwiWarmGray)
        Row(Modifier.fillMaxWidth().padding(top = KiwiSpacing.md), horizontalArrangement = Arrangement.SpaceBetween) {
            Fact("Current streak", "${analytics.currentStreakDays} days")
            Fact("Longest streak", "${analytics.longestStreakDays} days")
            Fact("Missed", analytics.missedTasks.toString())
            Fact("Postponed", postponedCount.toString())
        }
    }
}

@Composable
private fun RecentPatternCard(days: List<PlannerDaySummary>) {
    KiwiCard(Modifier.fillMaxWidth().animateContentSize()) {
        Text("Recent days", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        Text("Completed tasks across the last two weeks", style = MaterialTheme.typography.bodySmall, color = KiwiWarmGray)
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KiwiSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEach { day ->
                    val intensity = when {
                        day.planned == 0 -> 0f
                        day.completed == 0 -> 0.16f
                        day.completed == day.planned -> 1f
                        else -> 0.55f
                    }
                    val label = day.date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(
                                    color = if (intensity == 0f) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = intensity),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .semantics {
                                    contentDescription = "$label, ${day.completed} of ${day.planned} completed"
                                },
                        )
                        Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyHistoryCard(weeks: List<PlannerWeekSummary>) {
    KiwiCard(Modifier.fillMaxWidth().animateContentSize()) {
        Text("Weekly record", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = KiwiSpacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${week.weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${week.weekEnd.format(DateTimeFormatter.ofPattern("MMM d"))}", color = KiwiCharcoal)
                Text("${week.completed}/${week.planned} completed", color = KiwiWarmGray)
            }
        }
    }
}

@Composable
private fun ReflectionHistoryCard(reflections: List<WeeklyReflection>, onWriteReflection: () -> Unit) {
    KiwiCard(Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Weekly reflections", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
                Text("Your own words, saved privately.", style = MaterialTheme.typography.bodySmall, color = KiwiWarmGray)
            }
            KiwiButton(onClick = onWriteReflection) { Text(if (reflections.isEmpty()) "Write" else "This week") }
        }
        if (reflections.isEmpty()) {
            Text("No reflections yet.", modifier = Modifier.padding(top = KiwiSpacing.md), color = KiwiWarmGray)
        } else {
            reflections.take(6).forEach { reflection ->
                val week = formatDate(reflection.weekStart)
                Column(Modifier.fillMaxWidth().padding(top = KiwiSpacing.md)) {
                    Text("Week of $week", style = MaterialTheme.typography.labelLarge, color = KiwiCharcoal)
                    Text(reflection.content, style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
                }
            }
        }
    }
}

@Composable
private fun PostponementHistoryCard(postponements: List<TaskPostponement>) {
    KiwiCard(Modifier.fillMaxWidth().animateContentSize()) {
        Text("Postponed history", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        if (postponements.isEmpty()) {
            Text("No tasks have been moved to a later date.", color = KiwiWarmGray)
        } else {
            postponements.take(10).forEach { event ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = KiwiSpacing.md)
                        .semantics {
                            contentDescription = "${event.taskTitle}, postponed from ${formatDate(event.previousDate)} to ${formatDate(event.newDate)}"
                        },
                ) {
                    Text(event.taskTitle, style = MaterialTheme.typography.titleMedium, color = KiwiCharcoal)
                    Text(
                        "${formatDate(event.previousDate)} to ${formatDate(event.newDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KiwiWarmGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(analytics: PlannerAnalytics) {
    KiwiCard(Modifier.fillMaxWidth().animateContentSize()) {
        Text("By category", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        TaskCategory.entries.forEach { category ->
            Row(Modifier.fillMaxWidth().padding(top = KiwiSpacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category.name.lowercase().replaceFirstChar(Char::uppercase), color = KiwiCharcoal)
                Text((analytics.categoryCounts[category] ?: 0).toString(), color = KiwiWarmGray)
            }
        }
    }
}

@Composable
private fun WellnessSummaryCard(analytics: WellnessAnalytics) {
    KiwiCard(Modifier.fillMaxWidth().animateContentSize().semantics { contentDescription = "Recorded wellness review" }) {
        Text("Recorded wellness", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        Text("${analytics.recordedCycleCount} period records · ${analytics.painHistory.size} pain entries · ${analytics.energyHistory.size} energy entries", color = KiwiWarmGray)
        analytics.averageCycleLengthDays?.let { Text("Average recorded cycle: ${"%.1f".format(it)} days", color = KiwiCharcoal) }
        analytics.moodOccurrences.entries.take(3).forEach { (mood, count) ->
            Text("$mood · $count recorded", style = MaterialTheme.typography.bodySmall, color = KiwiWarmGray)
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = KiwiCharcoal)
        Text(label, style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
    }
}

private fun formatDate(value: String): String =
    runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM d, yyyy")) }.getOrDefault(value)
