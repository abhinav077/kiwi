package com.abhinavsirohi.kiwi.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.notifications.TaskReminderReconciliationWorkRequest
import com.abhinavsirohi.kiwi.core.notifications.SelfCareReminderReconciliationWorkRequest
import com.abhinavsirohi.kiwi.core.sync.restore.FirstLoginRestoreCoordinator
import com.abhinavsirohi.kiwi.data.local.RestorePreferencesStore
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseSessionProvider
import com.abhinavsirohi.kiwi.data.remote.SupabaseRestoreRemoteDataSource
import com.abhinavsirohi.kiwi.data.repository.DefaultSessionRestorationRepository
import com.abhinavsirohi.kiwi.data.repository.RoomRestoreDatabaseWriter
import com.abhinavsirohi.kiwi.data.repository.RoomLocalProfileLookup
import com.abhinavsirohi.kiwi.data.repository.SupabaseApprovedUserRepository
import com.abhinavsirohi.kiwi.domain.usecase.session.RestoreSession
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray
import io.github.jan.supabase.auth.auth

@Composable
fun SessionRestorationRoute(
    onSignedOut: (returningUser: Boolean) -> Unit,
    onAccessDenied: () -> Unit,
    onNeedsProfileSetup: () -> Unit,
    onOpenToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.applicationContext as KiwiApplication
    val clientResult = application.supabaseClient
    val sessionProvider = when (clientResult) {
        is RemoteResult.Success -> SupabaseSessionProvider(clientResult.value)
        is RemoteResult.Failure -> object : SessionProvider {
            override fun currentSession() = clientResult
        }
    }
    val cloudRestore = FirstLoginRestoreCoordinator(
        sessionProvider = sessionProvider,
        remoteDataSource = SupabaseRestoreRemoteDataSource(clientResult, sessionProvider),
        databaseWriter = RoomRestoreDatabaseWriter(application.database),
        reminderRecreator = {
            TaskReminderReconciliationWorkRequest.enqueue(application)
            SelfCareReminderReconciliationWorkRequest.enqueue(application)
        },
        restoreStateStore = RestorePreferencesStore(application),
    )
    val repository = DefaultSessionRestorationRepository(
        sessionProvider = sessionProvider,
        approvedUserRepository = SupabaseApprovedUserRepository(clientResult),
        localProfiles = RoomLocalProfileLookup(application.database.profileDao()),
        awaitSessionInitialization = {
            if (clientResult is RemoteResult.Success) {
                clientResult.value.auth.awaitInitialization()
            }
        },
        restoreCloudData = cloudRestore::restoreIfRequired,
    )
    val restorationViewModel: SessionRestorationViewModel = viewModel(
        factory = SessionRestorationViewModel.Factory(
            restoreSession = RestoreSession(repository),
            repository = repository,
        ),
    )
    val state by restorationViewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (val currentState = state) {
            is SessionRestorationUiState.SignedOut -> onSignedOut(currentState.returningUser)
            SessionRestorationUiState.AccessDenied -> onAccessDenied()
            SessionRestorationUiState.NeedsProfileSetup -> onNeedsProfileSetup()
            is SessionRestorationUiState.OpenToday -> {
                TaskReminderReconciliationWorkRequest.enqueue(application)
                onOpenToday()
            }
            else -> Unit
        }
    }

    SessionRestorationScreen(
        state = state,
        onRetry = restorationViewModel::retry,
        onSignOut = restorationViewModel::signOut,
        modifier = modifier,
    )
}

@Composable
fun SessionRestorationScreen(
    state: SessionRestorationUiState,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KiwiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state) {
                SessionRestorationUiState.Checking,
                SessionRestorationUiState.SigningOut,
                -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(KiwiSpacing.lg))
                    Text(
                        text = if (state is SessionRestorationUiState.SigningOut) {
                            "Signing out…"
                        } else {
                            "Opening your Kiwi space…"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                is SessionRestorationUiState.RetryableFailure -> {
                    Text(
                        text = "Kiwi needs a moment",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(KiwiSpacing.md))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = KiwiWarmGray,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(KiwiSpacing.xl))
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(KiwiSpacing.md))
                    OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign out")
                    }
                }
                else -> CircularProgressIndicator()
            }
        }
    }
}
