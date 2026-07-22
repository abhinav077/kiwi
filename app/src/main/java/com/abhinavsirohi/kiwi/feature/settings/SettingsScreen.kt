package com.abhinavsirohi.kiwi.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.data.local.KiwiFontPreference
import com.abhinavsirohi.kiwi.data.local.KiwiThemePreference
import com.abhinavsirohi.kiwi.data.local.LocalAccountDataManager
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.data.repository.SupabaseApprovedUserRepository
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiCoralRose
import com.abhinavsirohi.kiwi.ui.theme.KiwiForest
import com.abhinavsirohi.kiwi.ui.theme.KiwiLavender
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing

@Composable
fun SettingsRoute(
    onOpenWellness: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenSelfCare: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as KiwiApplication
    val clientResult = application.supabaseClient
    val sessionProvider = remember(clientResult) {
        when (clientResult) {
            is RemoteResult.Success -> SupabaseSessionProvider(clientResult.value)
            is RemoteResult.Failure -> object : SessionProvider {
                override fun currentSession() = RemoteResult.Failure(RemoteError.AuthenticationRequired)
            }
        }
    }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            settingsStore = application.settingsStore,
            pendingChangeDao = application.database.pendingChangeDao(),
            accountDataManager = LocalAccountDataManager(context, application.database, application.settingsStore),
            sessionProvider = sessionProvider,
            approvedUserRepository = SupabaseApprovedUserRepository(clientResult),
        ),
    )
    val state by settingsViewModel.state.collectAsState()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) settingsViewModel.export(it)
    }

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }

    SettingsScreen(
        state = state,
        onTheme = settingsViewModel::setTheme,
        onFont = settingsViewModel::setFont,
        onNotificationPrivacy = settingsViewModel::setNotificationPrivacy,
        onRecentScreenPrivacy = settingsViewModel::setRecentScreenPrivacy,
        onConfigurePin = settingsViewModel::configurePin,
        onDisableLock = settingsViewModel::disableAppLock,
        onBiometric = settingsViewModel::setBiometricEnabled,
        onExport = { exportLauncher.launch("kiwi-private-export.json") },
        onSignOut = settingsViewModel::signOut,
        onDeleteLocalData = settingsViewModel::deleteLocalDataAndSignOut,
        onOpenWellness = onOpenWellness,
        onOpenReview = onOpenReview,
        onOpenSelfCare = onOpenSelfCare,
        onOpenDownloads = onOpenDownloads,
        onClearMessage = settingsViewModel::clearMessage,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTheme: (KiwiThemePreference) -> Unit,
    onFont: (KiwiFontPreference) -> Unit,
    onNotificationPrivacy: (Boolean) -> Unit,
    onRecentScreenPrivacy: (Boolean) -> Unit,
    onConfigurePin: (String, String) -> Unit,
    onDisableLock: () -> Unit,
    onBiometric: (Boolean) -> Unit,
    onExport: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteLocalData: () -> Unit,
    onOpenWellness: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenSelfCare: () -> Unit,
    onOpenDownloads: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    KiwiBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(SETTINGS_LIST_TEST_TAG),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = KiwiSpacing.lg,
                end = KiwiSpacing.lg,
                top = KiwiSpacing.xl,
                bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.xl),
        ) {
            item {
                Text("Make Kiwi feel like yours", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(KiwiSpacing.xs))
                Text(
                    "Choose a calm visual rhythm, then decide what Kiwi may show outside the app.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SettingsSection("Theme") {
                    ThemePreview(state.settings.theme)
                    ChipRow(
                        values = KiwiThemePreference.entries,
                        selected = state.settings.theme,
                        label = { it.displayName },
                        onSelected = onTheme,
                    )
                    Text("Font style", style = MaterialTheme.typography.titleLarge)
                    ChipRow(
                        values = KiwiFontPreference.entries,
                        selected = state.settings.font,
                        label = { it.displayName },
                        onSelected = onFont,
                    )
                }
            }
            item {
                SettingsSection("Privacy") {
                    SettingsSwitchRow(
                        title = "Hide notification content",
                        supporting = "Use generic lock-screen notification text and secret visibility.",
                        checked = state.settings.hideNotificationContent,
                        onChecked = onNotificationPrivacy,
                    )
                    SettingsSwitchRow(
                        title = "Protect recent-screen preview",
                        supporting = "Block screenshots and the app preview in Recents.",
                        checked = state.settings.protectRecentScreen,
                        onChecked = onRecentScreenPrivacy,
                    )
                }
            }
            item {
                SettingsSection("App lock") {
                    Text(
                        if (state.settings.appLockEnabled) "PIN protection is active." else "Require a PIN when Kiwi opens or returns from the background.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { if (state.settings.appLockEnabled) onDisableLock() else showPinDialog = true }) {
                        Text(if (state.settings.appLockEnabled) "Turn off app lock" else "Set up PIN")
                    }
                    AnimatedVisibility(state.settings.appLockEnabled) {
                        SettingsSwitchRow(
                            title = "Unlock with biometrics",
                            supporting = if (biometricAvailable) "Use a strong enrolled biometric as an alternative to your PIN." else "No strong enrolled biometric is available.",
                            checked = state.settings.biometricEnabled && biometricAvailable,
                            enabled = biometricAvailable,
                            onChecked = onBiometric,
                        )
                    }
                }
            }
            item {
                SettingsSection("Sync status") {
                    val awaiting = state.pendingSyncCount + state.processingSyncCount + state.failedSyncCount
                    Text(
                        if (awaiting == 0) "No local changes are waiting in Kiwi’s sync queue."
                        else "$awaiting local changes are awaiting sync.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Pending ${state.pendingSyncCount} · Processing ${state.processingSyncCount} · Failed ${state.failedSyncCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SettingsSection("Your spaces") {
                    SettingsLink("Wellness", onOpenWellness)
                    SettingsLink("Review and reflections", onOpenReview)
                    SettingsLink("Self-care routines", onOpenSelfCare)
                    SettingsLink("Pinterest downloads", onOpenDownloads)
                }
            }
            item {
                SettingsSection("Private data and account") {
                    OutlinedButton(onClick = onExport, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Export private data as JSON")
                    }
                    OutlinedButton(onClick = onSignOut, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign out")
                    }
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, KiwiCoralRose),
                    ) {
                        Text("Delete local data and sign out", color = KiwiCoralRose)
                    }
                    Text(
                        "Local deletion does not delete your Supabase account or synchronized cloud copy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = onClearMessage,
            confirmButton = { TextButton(onClick = onClearMessage) { Text("OK") } },
            text = { Text(message) },
        )
    }
    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin, confirmation ->
                onConfigurePin(pin, confirmation)
                showPinDialog = false
            },
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete all local Kiwi data?") },
            text = { Text("This permanently removes local records and diary photos from this device, then signs out. Synchronized cloud data is not deleted.") },
            confirmButton = {
                Button(onClick = { showDeleteDialog = false; onDeleteLocalData() }) { Text("Delete local data") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier.padding(KiwiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md),
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            content()
        }
    }
}

@Composable
private fun ThemePreview(theme: KiwiThemePreference) {
    val accent = when (theme) {
        KiwiThemePreference.DARK -> KiwiCharcoal
        KiwiThemePreference.SUNRISE -> KiwiCoralRose
        KiwiThemePreference.GROVE -> KiwiForest
        KiwiThemePreference.SYSTEM,
        KiwiThemePreference.LIGHT,
        -> KiwiLavender
    }
    Card(colors = CardDefaults.cardColors(containerColor = accent), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(KiwiSpacing.lg)) {
            Text("A quieter place for your day", color = if (theme == KiwiThemePreference.DARK) MaterialTheme.colorScheme.surface else KiwiCharcoal, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(KiwiSpacing.md))
            Text("Theme preview", color = if (theme == KiwiThemePreference.DARK) MaterialTheme.colorScheme.surface else KiwiCharcoal)
        }
    }
}

@Composable
private fun <T> ChipRow(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.xs),
    ) {
        values.forEach { value ->
            FilterChip(selected = selected == value, onClick = { onSelected(value) }, label = { Text(label(value)) })
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, supporting: String, checked: Boolean, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun SettingsLink(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a Kiwi PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
                Text("Use 4 to 8 digits. Kiwi stores only a salted one-way PIN hash.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirmation = it },
                    label = { Text("Confirm PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pin, confirmation) }, enabled = pin.length in 4..8 && confirmation.length in 4..8) { Text("Enable lock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val KiwiThemePreference.displayName: String
    get() = name.lowercase().replaceFirstChar(Char::uppercase)

private val KiwiFontPreference.displayName: String
    get() = name.lowercase().replaceFirstChar(Char::uppercase)

internal const val SETTINGS_LIST_TEST_TAG = "settings-list"
