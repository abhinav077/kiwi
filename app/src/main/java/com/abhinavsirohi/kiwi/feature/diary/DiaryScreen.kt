package com.abhinavsirohi.kiwi.feature.diary

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.design.KiwiButton
import com.abhinavsirohi.kiwi.core.design.KiwiCard
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.DiaryPhoto
import com.abhinavsirohi.kiwi.domain.usecase.diary.CreateDiaryEntry
import com.abhinavsirohi.kiwi.domain.usecase.diary.ObserveDiaryEntries
import com.abhinavsirohi.kiwi.domain.usecase.diary.SaveDiaryEntry
import com.abhinavsirohi.kiwi.domain.usecase.diary.TombstoneDiaryEntry
import com.abhinavsirohi.kiwi.data.repository.RoomDiaryRepository
import com.abhinavsirohi.kiwi.data.repository.RoomDiaryPhotoRepository
import com.abhinavsirohi.kiwi.domain.usecase.diary.AddDiaryPhoto
import com.abhinavsirohi.kiwi.domain.usecase.diary.DeleteDiaryPhoto
import com.abhinavsirohi.kiwi.domain.usecase.diary.ObserveDiaryPhotos
import com.abhinavsirohi.kiwi.core.design.KiwiChip
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DiaryRoute(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as KiwiApplication
    val repository = RoomDiaryRepository(
        application.database,
        application.supabaseClient,
        application.deviceId,
        application.diaryPhotoSyncScheduler,
    )
    val photoRepository = RoomDiaryPhotoRepository(
        application.database,
        application.supabaseClient,
        application.deviceId,
        application.diaryPhotoLocalStore,
        application.diaryPhotoSyncScheduler,
    )
    val diaryViewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory(
        ObserveDiaryEntries(repository), CreateDiaryEntry(repository), SaveDiaryEntry(repository), TombstoneDiaryEntry(repository),
        ObserveDiaryPhotos(photoRepository), AddDiaryPhoto(photoRepository), DeleteDiaryPhoto(photoRepository),
    ))
    val state by diaryViewModel.state.collectAsState()
    DiaryScreen(
        state, diaryViewModel::setQuery, diaryViewModel::startCreating, diaryViewModel::startEditing,
        diaryViewModel::requestDelete, diaryViewModel::addPhoto, diaryViewModel::requestDeletePhoto,
        diaryViewModel::updateDraft, diaryViewModel::saveEditor, diaryViewModel::dismissEditor,
        diaryViewModel::confirmDelete, diaryViewModel::dismissDelete, diaryViewModel::confirmDeletePhoto,
        diaryViewModel::dismissDeletePhoto, diaryViewModel::setFilter, diaryViewModel::toggleCalendar,
        diaryViewModel::selectDate, diaryViewModel::clearDate, diaryViewModel::showPrivacy,
        diaryViewModel::dismissPrivacy, modifier,
    )
}

@Composable
fun DiaryScreen(
    state: DiaryUiState,
    onQueryChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit,
    onPhotoPicked: (String, String) -> Unit,
    onDeletePhoto: (DiaryPhoto) -> Unit,
    onDraftChanged: ((DiaryDraft) -> DiaryDraft) -> Unit,
    onSave: () -> Unit,
    onDismissEditor: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDeletePhoto: () -> Unit,
    onDismissDeletePhoto: () -> Unit,
    onFilterChanged: (DiaryFilter) -> Unit,
    onToggleCalendar: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onClearDate: () -> Unit,
    onShowPrivacy: () -> Unit,
    onDismissPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var photoEntryId by remember { mutableStateOf<String?>(null) }
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val entryId = photoEntryId
        photoEntryId = null
        if (uri != null && entryId != null) onPhotoPicked(entryId, uri.toString())
    }
    val filtered = state.entries.filter { entry ->
        val query = state.query.trim()
        val matchesQuery = query.isEmpty() || listOf(entry.title, entry.content, entry.bestThing.orEmpty(), entry.mood.orEmpty()).any { it.contains(query, ignoreCase = true) }
        val matchesFilter = state.filter == DiaryFilter.ALL || entry.isFavourite
        val matchesDate = state.selectedDate == null || entry.entryDate == state.selectedDate
        matchesQuery && matchesFilter && matchesDate
    }
    KiwiBackground(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = KiwiSpacing.lg), contentPadding = PaddingValues(top = KiwiSpacing.xl, bottom = KiwiSpacing.xxxl), verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Diary", style = androidx.compose.material3.MaterialTheme.typography.displayLarge, color = KiwiCharcoal)
                        Text("A private place for what you want to remember.", color = KiwiWarmGray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = onShowPrivacy) { Text("Privacy & lock") }
                        KiwiButton(onClick = onAdd) { Text("New entry") }
                    }
                }
            }
            item { OutlinedTextField(value = state.query, onValueChange = onQueryChanged, modifier = Modifier.fillMaxWidth(), label = { Text("Search diary") }, singleLine = true) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    KiwiChip("All entries", { onFilterChanged(DiaryFilter.ALL) })
                    KiwiChip("Favourites", { onFilterChanged(DiaryFilter.FAVOURITES) })
                    KiwiChip(if (state.showCalendar) "Hide calendar" else "Calendar", onToggleCalendar)
                }
            }
            if (state.filter == DiaryFilter.FAVOURITES || state.selectedDate != null) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Text("Showing", style = MaterialTheme.typography.labelLarge, color = KiwiWarmGray)
                        if (state.filter == DiaryFilter.FAVOURITES) Text("favourites", color = KiwiCharcoal)
                        state.selectedDate?.let { Text(it, color = KiwiCharcoal) }
                        TextButton(onClick = { onFilterChanged(DiaryFilter.ALL); onClearDate() }) { Text("Clear filters") }
                    }
                }
            }
            if (state.showCalendar) {
                item {
                    DiaryCalendar(
                        month = visibleMonth,
                        selectedDate = state.selectedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                        onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                        onDateSelected = onDateSelected,
                    )
                }
            }
            state.message?.let { message -> item { Text(message, color = androidx.compose.material3.MaterialTheme.colorScheme.error) } }
            if (state.isLoading) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            else if (filtered.isEmpty()) item {
                KiwiCard(Modifier.fillMaxWidth()) {
                    val hasFilters = state.query.isNotBlank() || state.filter == DiaryFilter.FAVOURITES || state.selectedDate != null
                    Text(if (hasFilters) "Nothing here yet" else "Your diary is ready", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
                    Text(if (hasFilters) "Try clearing a filter or write a new entry for this day." else "Write down a moment whenever you’re ready.", color = KiwiWarmGray)
                    if (hasFilters) TextButton(onClick = { onQueryChanged(""); onFilterChanged(DiaryFilter.ALL); onClearDate() }) { Text("Clear filters") }
                }
            }
            else items(filtered, key = DiaryEntry::localId) { entry ->
                DiaryCard(
                    entry = entry,
                    photos = state.photos[entry.localId].orEmpty(),
                    isAddingPhoto = state.isAddingPhoto,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onAddPhoto = {
                        photoEntryId = entry.localId
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onDeletePhoto = onDeletePhoto,
                )
            }
        }
    }
    state.editor?.let { draft -> DiaryEditorDialog(draft, state.isSaving, onDraftChanged, onSave, onDismissEditor) }
    state.pendingDelete?.let { entry -> AlertDialog(onDismissRequest = onDismissDelete, title = { Text("Delete this entry?") }, text = { Text("This diary entry will be removed from this device and queued for backup synchronization.") }, confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } }, dismissButton = { TextButton(onClick = onDismissDelete) { Text("Keep entry") } }) }
    state.pendingDeletePhoto?.let {
        AlertDialog(
            onDismissRequest = onDismissDeletePhoto,
            title = { Text("Remove this photo?") },
            text = { Text("The local preview will disappear now. Private Storage cleanup will finish safely when online.") },
            confirmButton = { TextButton(onClick = onConfirmDeletePhoto) { Text("Remove photo") } },
            dismissButton = { TextButton(onClick = onDismissDeletePhoto) { Text("Keep photo") } },
        )
    }
    if (state.showPrivacy) {
        AlertDialog(
            onDismissRequest = onDismissPrivacy,
            title = { Text("Your private diary") },
            text = { Text("Diary writing and photo previews are saved on this device first. Optional backup uses your authenticated private account and private Storage. App lock and privacy controls can be opened from this entry point when enabled.") },
            confirmButton = { TextButton(onClick = onDismissPrivacy) { Text("Done") } },
        )
    }
}

@Composable
private fun DiaryCalendar(
    month: YearMonth,
    selectedDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPreviousMonth) { Text("‹") }
            Text(month.month.name.lowercase().replaceFirstChar(Char::uppercase) + " ${month.year}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = KiwiCharcoal)
            TextButton(onClick = onNextMonth) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.values().forEach { day ->
                Text(day.name.take(1), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
            }
        }
        val firstDayOffset = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
        val days = buildList<LocalDate?> {
            repeat(firstDayOffset) { add(null) }
            (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
            while (size % 7 != 0) add(null)
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).padding(vertical = KiwiSpacing.xs), contentAlignment = Alignment.Center) {
                        date?.let {
                            TextButton(
                                onClick = { onDateSelected(it) },
                                modifier = Modifier.semantics { contentDescription = "Diary date $it" },
                            ) {
                                Text(
                                    it.dayOfMonth.toString(),
                                    color = if (it == selectedDate) MaterialTheme.colorScheme.primary else KiwiCharcoal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun DiaryCard(
    entry: DiaryEntry,
    photos: List<DiaryPhoto>,
    isAddingPhoto: Boolean,
    onEdit: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (DiaryPhoto) -> Unit,
) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(entry.title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = KiwiCharcoal, modifier = Modifier.weight(1f)); if (entry.isFavourite) Text("★") }
        Text(entry.entryDate, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
        Spacer(Modifier.height(KiwiSpacing.xs))
        Text(entry.content, color = KiwiWarmGray, maxLines = 3, overflow = TextOverflow.Ellipsis)
        entry.mood?.let { Text("Mood: $it", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = KiwiWarmGray) }
        if (photos.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
                photos.forEach { photo -> DiaryPhotoPreview(photo, onDeletePhoto) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
            TextButton(onClick = onAddPhoto, enabled = !isAddingPhoto) { Text(if (isAddingPhoto) "Saving photo…" else "Add photo") }
            TextButton(onClick = { onEdit(entry) }) { Text("Edit") }
            TextButton(onClick = { onDelete(entry) }) { Text("Delete") }
        }
    }
}

@Composable
private fun DiaryPhotoPreview(photo: DiaryPhoto, onDeletePhoto: (DiaryPhoto) -> Unit) {
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, key1 = photo.localPath) {
        value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(photo.localPath)?.asImageBitmap() }
    }
    Column(Modifier.width(128.dp)) {
        Box {
            image?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Diary photo preview",
                    modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            } ?: Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        TextButton(onClick = { onDeletePhoto(photo) }) { Text("Remove") }
    }
}

@Composable private fun DiaryEditorDialog(draft: DiaryDraft, saving: Boolean, onChanged: ((DiaryDraft) -> DiaryDraft) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (draft.title.isEmpty()) "New diary entry" else "Edit diary entry") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
            OutlinedTextField(draft.title, { value -> onChanged { it.copy(title = value) } }, label = { Text("Title") }, singleLine = true)
            OutlinedTextField(draft.content, { value -> onChanged { it.copy(content = value) } }, label = { Text("What happened?") }, minLines = 4)
            OutlinedTextField(draft.entryDate, { value -> onChanged { it.copy(entryDate = value) } }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
            OutlinedTextField(draft.bestThing, { value -> onChanged { it.copy(bestThing = value) } }, label = { Text("Best thing today") }, singleLine = true)
            OutlinedTextField(draft.mood, { value -> onChanged { it.copy(mood = value) } }, label = { Text("Mood") }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Favourite", Modifier.weight(1f)); Switch(checked = draft.isFavourite, onCheckedChange = { checked -> onChanged { it.copy(isFavourite = checked) } }) }
        }
    }, confirmButton = { TextButton(onClick = onSave, enabled = !saving) { Text(if (saving) "Saving…" else "Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
