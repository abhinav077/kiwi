package com.abhinavsirohi.kiwi.feature.wellness

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.design.KiwiButton
import com.abhinavsirohi.kiwi.core.design.KiwiCard
import com.abhinavsirohi.kiwi.core.design.KiwiChip
import com.abhinavsirohi.kiwi.data.repository.RoomWellnessRepository
import com.abhinavsirohi.kiwi.data.repository.RoomHealthAlertRepository
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.model.WellnessFlow
import com.abhinavsirohi.kiwi.domain.model.WellnessAnalytics
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.HealthAlertState
import com.abhinavsirohi.kiwi.domain.model.cautionBody
import com.abhinavsirohi.kiwi.domain.model.cautionTitle
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveHealthAlertEpisodes
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ReconcileHealthAlerts
import com.abhinavsirohi.kiwi.domain.usecase.wellness.AcknowledgeHealthAlert
import com.abhinavsirohi.kiwi.domain.usecase.wellness.DismissHealthAlert
import com.abhinavsirohi.kiwi.domain.usecase.wellness.CreateCycleRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.CreateWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveCycleRecords
import com.abhinavsirohi.kiwi.domain.usecase.wellness.ObserveWellnessDailyRecords
import com.abhinavsirohi.kiwi.domain.usecase.wellness.SaveCycleRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.SaveWellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.TombstoneCycleRecord
import com.abhinavsirohi.kiwi.domain.usecase.wellness.TombstoneWellnessDailyRecord
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiLavender
import com.abhinavsirohi.kiwi.ui.theme.KiwiPeriwinkle
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun WellnessRoute(modifier: Modifier = Modifier) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as KiwiApplication
    val repository = RoomWellnessRepository(application.database, application.supabaseClient, application.deviceId)
    val alertRepository = RoomHealthAlertRepository(
        application.database,
        application.supabaseClient,
        application.deviceId,
        application.healthAlertNotificationScheduler,
    )
    val wellnessViewModel: WellnessViewModel = viewModel(
        factory = WellnessViewModel.Factory(
            ObserveCycleRecords(repository),
            ObserveWellnessDailyRecords(repository),
            CreateCycleRecord(repository),
            SaveCycleRecord(repository),
            TombstoneCycleRecord(repository),
            CreateWellnessDailyRecord(repository),
            SaveWellnessDailyRecord(repository),
            TombstoneWellnessDailyRecord(repository),
            observeHealthAlerts = ObserveHealthAlertEpisodes(alertRepository),
            reconcileHealthAlerts = ReconcileHealthAlerts(alertRepository),
            acknowledgeHealthAlert = AcknowledgeHealthAlert(alertRepository),
            dismissHealthAlert = DismissHealthAlert(alertRepository),
        ),
    )
    val state by wellnessViewModel.state.collectAsState()
    WellnessScreen(
        state = state,
        onPreviousMonth = wellnessViewModel::previousMonth,
        onNextMonth = wellnessViewModel::nextMonth,
        onDateSelected = wellnessViewModel::selectDate,
        onQuickLog = wellnessViewModel::startQuickLog,
        onAddCycle = wellnessViewModel::startCycleRecord,
        onEditDaily = wellnessViewModel::editDaily,
        onEditCycle = wellnessViewModel::editCycle,
        onDeleteDaily = wellnessViewModel::requestDeleteDaily,
        onDeleteCycle = wellnessViewModel::requestDeleteCycle,
        onDailyDraftChanged = wellnessViewModel::updateDailyDraft,
        onCycleDraftChanged = wellnessViewModel::updateCycleDraft,
        onSaveDaily = wellnessViewModel::saveDailyRecord,
        onSaveCycle = wellnessViewModel::saveCycleRecord,
        onDismissDaily = wellnessViewModel::dismissDailyEditor,
        onDismissCycle = wellnessViewModel::dismissCycleEditor,
        onConfirmDelete = wellnessViewModel::confirmDeletion,
        onDismissDelete = wellnessViewModel::dismissDeletion,
        onAcknowledgeAlert = wellnessViewModel::acknowledgeAlert,
        onDismissAlert = wellnessViewModel::dismissAlert,
        modifier = modifier,
    )
}

@Composable
fun WellnessScreen(
    state: WellnessUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onQuickLog: () -> Unit,
    onAddCycle: () -> Unit,
    onEditDaily: (WellnessDailyRecord) -> Unit,
    onEditCycle: (CycleRecord) -> Unit,
    onDeleteDaily: (WellnessDailyRecord) -> Unit,
    onDeleteCycle: (CycleRecord) -> Unit,
    onDailyDraftChanged: (WellnessDailyDraft) -> Unit,
    onCycleDraftChanged: (CycleRecordDraft) -> Unit,
    onSaveDaily: () -> Unit,
    onSaveCycle: () -> Unit,
    onDismissDaily: () -> Unit,
    onDismissCycle: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onAcknowledgeAlert: (String) -> Unit,
    onDismissAlert: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = KiwiSpacing.lg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = KiwiSpacing.xl, bottom = KiwiSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md),
        ) {
            item {
                Text("Wellness", style = MaterialTheme.typography.displayLarge, color = KiwiCharcoal)
                Text(
                    "Your recorded moments, held gently.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KiwiWarmGray,
                )
            }
            item { WellnessOverview(state.dailyRecords, state.cycleRecords, onQuickLog, onAddCycle) }
            if (state.healthAlerts.isNotEmpty()) {
                item { Text("Recorded-pattern notes", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal) }
                items(state.healthAlerts, key = { "alert-${it.localId}" }) { alert ->
                    HealthAlertCard(alert, { onAcknowledgeAlert(alert.localId) }, { onDismissAlert(alert.localId) })
                }
            }
            item { WellnessAnalyticsCard(state.analytics) }
            item {
                WellnessCalendar(
                    month = state.displayedMonth,
                    selectedDate = state.selectedDate,
                    recordedDates = state.dailyRecords.mapTo(mutableSetOf()) { it.recordDate },
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onDateSelected = onDateSelected,
                )
            }
            state.message?.let { message -> item { Text(message, style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray) } }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else {
                item { Text("History", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal) }
                if (state.dailyRecords.isEmpty() && state.cycleRecords.isEmpty()) {
                    item { Text("No wellness records yet. Start with a small check-in whenever you’re ready.", color = KiwiWarmGray) }
                }
                items(state.dailyRecords, key = { "daily-${it.localId}" }) { record ->
                    DailyRecordCard(record, onEdit = { onEditDaily(record) }, onDelete = { onDeleteDaily(record) })
                }
                items(state.cycleRecords, key = { "cycle-${it.localId}" }) { record ->
                    CycleRecordCard(record, onEdit = { onEditCycle(record) }, onDelete = { onDeleteCycle(record) })
                }
            }
        }
    }
    state.dailyDraft?.let { draft ->
        DailyLogDialog(draft, state.isSaving, onDailyDraftChanged, onSaveDaily, onDismissDaily)
    }
    state.cycleDraft?.let { draft ->
        CycleRecordDialog(draft, state.isSaving, onCycleDraftChanged, onSaveCycle, onDismissCycle)
    }
    state.pendingDeletion?.let { deletion ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Remove this record?") },
            text = { Text("It will disappear here now and be queued for removal when sync is available.") },
            confirmButton = { TextButton(onClick = onConfirmDelete, enabled = !state.isSaving) { Text("Remove") } },
            dismissButton = { TextButton(onClick = onDismissDelete, enabled = !state.isSaving) { Text("Keep") } },
        )
    }
}

@Composable
private fun HealthAlertCard(alert: HealthAlertEpisode, onAcknowledge: () -> Unit, onDismiss: () -> Unit) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Text(alert.patternType.cautionTitle(), style = MaterialTheme.typography.titleMedium, color = KiwiCharcoal)
        Text(alert.patternType.cautionBody(), style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
        Text(
            "Recorded ${alert.evidenceCount} time(s), ${alert.firstEvidenceDate} to ${alert.lastEvidenceDate}.",
            style = MaterialTheme.typography.labelSmall,
            color = KiwiWarmGray,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
            if (alert.state == HealthAlertState.Active) TextButton(onClick = onAcknowledge) { Text("I’ve seen this") }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun WellnessAnalyticsCard(analytics: WellnessAnalytics) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Text("Historical summary", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        Text("Based only on recorded entries.", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
        Spacer(Modifier.height(KiwiSpacing.sm))
        Text("Recorded periods: ${analytics.recordedCycleCount}", color = KiwiCharcoal)
        analytics.averageCycleLengthDays?.let {
            Text("Average recorded cycle: ${it.oneDecimal()} days", color = KiwiWarmGray)
        } ?: Text("Add at least two different period starts for cycle-length history.", color = KiwiWarmGray)
        if (analytics.shortestCycleDays != null && analytics.longestCycleDays != null) {
            Text(
                "Shortest ${analytics.shortestCycleDays} days · Longest ${analytics.longestCycleDays} days · Variation ${analytics.cycleVariationDays} days",
                color = KiwiWarmGray,
            )
        }
        analytics.averageBleedingDurationDays?.let {
            Text("Average recorded bleeding duration: ${it.oneDecimal()} days", color = KiwiWarmGray)
        }
        if (analytics.flowDistribution.isNotEmpty()) {
            Text(
                "Flow entries: " + analytics.flowDistribution.entries.joinToString { (flow, count) ->
                    "${flow.name.lowercase()} $count"
                },
                color = KiwiWarmGray,
            )
        }
        HistoryLine("Pain history", analytics.painHistory.takeLast(5).joinToString { "${it.date}: ${it.value}" })
        HistoryLine("Energy history", analytics.energyHistory.takeLast(5).joinToString { "${it.date}: ${it.value}" })
        HistoryLine(
            "Symptoms",
            analytics.symptomOccurrences.entries.sortedByDescending { it.value }.take(5)
                .joinToString { "${it.key} ${it.value}" },
        )
        HistoryLine(
            "Moods",
            analytics.moodOccurrences.entries.sortedByDescending { it.value }.take(5)
                .joinToString { "${it.key} ${it.value}" },
        )
    }
}

@Composable
private fun HistoryLine(label: String, value: String) {
    if (value.isNotBlank()) {
        Text("$label: $value", style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
    }
}

private fun Double.oneDecimal(): String = "%.1f".format(this)

@Composable
private fun WellnessOverview(
    dailyRecords: List<WellnessDailyRecord>,
    cycleRecords: List<CycleRecord>,
    onQuickLog: () -> Unit,
    onAddCycle: () -> Unit,
) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Text("Recorded facts", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        Spacer(Modifier.height(KiwiSpacing.xs))
        Text(
            "${dailyRecords.size} daily logs · ${cycleRecords.size} period records",
            style = MaterialTheme.typography.bodyMedium,
            color = KiwiWarmGray,
        )
        dailyRecords.maxByOrNull { it.recordDate }?.let {
            Text("Latest log: ${it.recordDate}", style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
        }
        Spacer(Modifier.height(KiwiSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
            KiwiButton(onClick = onQuickLog, modifier = Modifier.weight(1f)) { Text("Quick log") }
            KiwiButton(onClick = onAddCycle, modifier = Modifier.weight(1f)) { Text("Period record") }
        }
    }
}

@Composable
private fun WellnessCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    recordedDates: Set<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPreviousMonth, modifier = Modifier.semantics { contentDescription = "Previous wellness month" }) { Text("‹") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
            TextButton(onClick = onNextMonth, modifier = Modifier.semantics { contentDescription = "Next wellness month" }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
            }
        }
        val blanks = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
        val days = List<LocalDate?>(blanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).size(44.dp), contentAlignment = Alignment.Center) {
                        date?.let {
                            val selected = it == selectedDate
                            val recorded = it.toString() in recordedDates
                            Text(
                                it.dayOfMonth.toString(),
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(if (selected) KiwiLavender else if (recorded) KiwiPeriwinkle else Color.Transparent, MaterialTheme.shapes.small)
                                    .clickable { onDateSelected(it) }
                                    .semantics { contentDescription = "Wellness date $it" }
                                    .padding(top = 9.dp),
                                textAlign = TextAlign.Center,
                                color = KiwiCharcoal,
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).size(44.dp)) }
            }
        }
        Text("A lavender date has a saved daily log.", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
    }
}

@Composable
private fun DailyRecordCard(record: WellnessDailyRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(record.recordDate, style = MaterialTheme.typography.titleMedium, color = KiwiCharcoal)
                Text(record.dailySummary(), style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Remove") }
        }
        record.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray) }
    }
}

@Composable
private fun CycleRecordCard(record: CycleRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Period record", style = MaterialTheme.typography.titleMedium, color = KiwiCharcoal)
                Text("${record.startDate} – ${record.endDate ?: "ongoing"}", style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Remove") }
        }
    }
}

@Composable
private fun DailyLogDialog(
    draft: WellnessDailyDraft,
    isSaving: Boolean,
    onDraftChanged: (WellnessDailyDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.existing == null) "Quick log" else "Edit daily log") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.xs)) {
                item { WellnessTextField("Date", draft.recordDate) { onDraftChanged(draft.copy(recordDate = it)) } }
                item {
                    Text("Flow", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.xs)) {
                        listOf<WellnessFlow?>(null, WellnessFlow.None, WellnessFlow.Light, WellnessFlow.Medium, WellnessFlow.Heavy).forEach { flow ->
                            KiwiChip(flow?.name ?: "Unset", onClick = { onDraftChanged(draft.copy(flow = flow)) })
                        }
                    }
                }
                item { WellnessTextField("Pain 0–10", draft.painLevel) { onDraftChanged(draft.copy(painLevel = it)) } }
                item { WellnessTextField("Cramps 0–10", draft.crampsLevel) { onDraftChanged(draft.copy(crampsLevel = it)) } }
                item { WellnessTextField("Symptoms, separated by commas", draft.symptoms) { onDraftChanged(draft.copy(symptoms = it)) } }
                item { WellnessTextField("Mood", draft.mood) { onDraftChanged(draft.copy(mood = it)) } }
                item { WellnessTextField("Energy 0–10", draft.energyLevel) { onDraftChanged(draft.copy(energyLevel = it)) } }
                item { WellnessTextField("Sleep minutes", draft.sleepMinutes) { onDraftChanged(draft.copy(sleepMinutes = it)) } }
                item { WellnessTextField("Notes", draft.notes) { onDraftChanged(draft.copy(notes = it)) } }
                item { WellnessTextField("Exercise", draft.exercise) { onDraftChanged(draft.copy(exercise = it)) } }
                item { WellnessTextField("Self-care / medication notes", draft.selfCareMedicationNotes) { onDraftChanged(draft.copy(selfCareMedicationNotes = it)) } }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = !isSaving) { Text(if (isSaving) "Saving…" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } },
    )
}

@Composable
private fun CycleRecordDialog(
    draft: CycleRecordDraft,
    isSaving: Boolean,
    onDraftChanged: (CycleRecordDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.existing == null) "Period record" else "Edit period record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
                WellnessTextField("Start date (YYYY-MM-DD)", draft.startDate) { onDraftChanged(draft.copy(startDate = it)) }
                WellnessTextField("End date (optional)", draft.endDate) { onDraftChanged(draft.copy(endDate = it)) }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = !isSaving) { Text(if (isSaving) "Saving…" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } },
    )
}

@Composable
private fun WellnessTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

private fun WellnessDailyRecord.dailySummary(): String = buildList {
    flow?.let { add("${it.name.lowercase()} flow") }
    painLevel?.let { add("pain $it") }
    mood?.let { add(it) }
    if (isEmpty()) add("Daily check-in")
}.joinToString(" · ")
