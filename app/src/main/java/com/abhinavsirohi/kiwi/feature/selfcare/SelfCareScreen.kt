package com.abhinavsirohi.kiwi.feature.selfcare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.design.KiwiButton
import com.abhinavsirohi.kiwi.core.design.KiwiCard
import com.abhinavsirohi.kiwi.domain.model.SelfCareCategory
import com.abhinavsirohi.kiwi.domain.model.SelfCareDay
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.data.repository.RoomSelfCareRepository
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.CreateSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.ObserveSelfCareRoutines
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.SaveSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.TombstoneSelfCareRoutine
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray

@Composable
fun SelfCareRoute() {
    val application = LocalContext.current.applicationContext as KiwiApplication
    val repository = RoomSelfCareRepository(application.database, application.supabaseClient, application.deviceId, application.selfCareReminderScheduler)
    val selfCareViewModel: SelfCareViewModel = viewModel(factory = SelfCareViewModel.Factory(
        ObserveSelfCareRoutines(repository), CreateSelfCareRoutine(repository), SaveSelfCareRoutine(repository), TombstoneSelfCareRoutine(repository),
    ))
    val state by selfCareViewModel.state.collectAsState()
    SelfCareScreen(state, selfCareViewModel::startCreating, selfCareViewModel::startEditing, selfCareViewModel::toggleActive, selfCareViewModel::toggleCompleted, selfCareViewModel::requestDelete, selfCareViewModel::updateDraft, selfCareViewModel::saveEditor, selfCareViewModel::dismissEditor, selfCareViewModel::confirmDelete, selfCareViewModel::dismissDelete)
}

@Composable
fun SelfCareScreen(
    state: SelfCareUiState,
    onAdd: () -> Unit,
    onEdit: (SelfCareRoutine) -> Unit,
    onToggleActive: (SelfCareRoutine) -> Unit,
    onToggleCompleted: (SelfCareRoutine) -> Unit,
    onDelete: (SelfCareRoutine) -> Unit,
    onDraftChanged: ((SelfCareDraft) -> SelfCareDraft) -> Unit,
    onSave: () -> Unit,
    onDismissEditor: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
) {
    KiwiBackground(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = KiwiSpacing.lg), contentPadding = PaddingValues(top = KiwiSpacing.xl, bottom = KiwiSpacing.xxxl), verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Self-care", style = MaterialTheme.typography.displayLarge, color = KiwiCharcoal)
                        Text("Small routines that make space for you.", color = KiwiWarmGray)
                    }
                    KiwiButton(onClick = onAdd) { Text("New routine") }
                }
            }
            state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.isLoading) item { Text("Loading your routines…", color = KiwiWarmGray) }
            else if (state.routines.isEmpty()) item {
                KiwiCard(Modifier.fillMaxWidth()) {
                    Text("A little care, regularly", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
                    Text("Create a gentle routine for your mind, body, home, or joy.", color = KiwiWarmGray)
                    Spacer(Modifier.height(KiwiSpacing.sm))
                    KiwiButton(onClick = onAdd) { Text("Create a routine") }
                }
            } else items(state.routines, key = SelfCareRoutine::localId) { routine ->
                SelfCareCard(routine, onEdit, onToggleActive, onToggleCompleted, onDelete)
            }
        }
    }
    state.editor?.let { SelfCareEditor(it, state.isSaving, onDraftChanged, onSave, onDismissEditor) }
    state.pendingDelete?.let { routine ->
        AlertDialog(onDismissRequest = onDismissDelete, title = { Text("Delete this routine?") }, text = { Text("Its completion history and reminder will be removed from this device and queued for backup synchronization.") }, confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } }, dismissButton = { TextButton(onClick = onDismissDelete) { Text("Keep routine") } })
    }
}

@Composable
private fun SelfCareCard(routine: SelfCareRoutine, onEdit: (SelfCareRoutine) -> Unit, onToggleActive: (SelfCareRoutine) -> Unit, onToggleCompleted: (SelfCareRoutine) -> Unit, onDelete: (SelfCareRoutine) -> Unit) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (routine.completionDates.contains(java.time.LocalDate.now().toString())) "✓" else "○", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(KiwiSpacing.sm))
            Column(Modifier.weight(1f)) {
                Text(routine.name, style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal, textDecoration = if (!routine.isActive) TextDecoration.LineThrough else null)
                Text(listOf(routine.category.name, routine.scheduleLabel(), routine.repeatLabel()).filter(String::isNotEmpty).joinToString(" • "), color = KiwiWarmGray)
            }
        }
        routine.description?.let { Text(it, color = KiwiWarmGray) }
        if (routine.checklist.isNotEmpty()) Text("Checklist: ${routine.checklist.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall, color = KiwiWarmGray)
        Text("Completed ${routine.completionDates.size} time${if (routine.completionDates.size == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onToggleCompleted(routine) }) { Text(if (routine.completionDates.contains(java.time.LocalDate.now().toString())) "Undo today" else "Complete") }
            TextButton(onClick = { onToggleActive(routine) }) { Text(if (routine.isActive) "Pause" else "Resume") }
            TextButton(onClick = { onEdit(routine) }) { Text("Edit") }
            TextButton(onClick = { onDelete(routine) }) { Text("Delete") }
        }
    }
}

@Composable
private fun SelfCareEditor(draft: SelfCareDraft, saving: Boolean, onChanged: ((SelfCareDraft) -> SelfCareDraft) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (draft.name.isBlank()) "New routine" else "Edit routine") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
            OutlinedTextField(draft.name, { value -> onChanged { it.copy(name = value) } }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(draft.description, { value -> onChanged { it.copy(description = value) } }, label = { Text("Description") }, minLines = 2)
            Text("Category", style = MaterialTheme.typography.labelLarge, color = KiwiWarmGray)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.xs)) { SelfCareCategory.entries.forEach { category -> FilterChip(selected = draft.category == category, onClick = { onChanged { it.copy(category = category) } }, label = { Text(category.name) }) } }
            OutlinedTextField(draft.timeText, { value -> onChanged { it.copy(timeText = value) } }, label = { Text("Reminder time (HH:MM, optional)") }, singleLine = true)
            Text("Repeat on", style = MaterialTheme.typography.labelLarge, color = KiwiWarmGray)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.xs)) { SelfCareDay.entries.forEach { day -> FilterChip(selected = day in draft.repeatDays, onClick = { onChanged { it.copy(repeatDays = if (day in it.repeatDays) it.repeatDays - day else it.repeatDays + day) } }, label = { Text(day.name.take(1)) }) } }
            OutlinedTextField(draft.checklistText, { value -> onChanged { it.copy(checklistText = value) } }, label = { Text("Checklist (one item per line, optional)") }, minLines = 2)
        }
    }, confirmButton = { TextButton(onClick = onSave, enabled = !saving) { Text(if (saving) "Saving…" else "Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun SelfCareRoutine.scheduleLabel() = scheduledTimeMinutes?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()
private fun SelfCareRoutine.repeatLabel() = if (repeatDays.isEmpty()) "Any day" else repeatDays.joinToString(", ") { it.name.take(3) }
