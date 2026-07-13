package com.abhinavsirohi.kiwi.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.domain.model.SessionRestoration
import com.abhinavsirohi.kiwi.domain.model.SessionRestorationFailure
import com.abhinavsirohi.kiwi.domain.repository.SessionRestorationRepository
import com.abhinavsirohi.kiwi.domain.usecase.session.RestoreSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionRestorationUiState {
    data object Checking : SessionRestorationUiState
    data object SigningOut : SessionRestorationUiState
    data class SignedOut(val returningUser: Boolean) : SessionRestorationUiState
    data object AccessDenied : SessionRestorationUiState
    data object NeedsProfileSetup : SessionRestorationUiState
    data class OpenToday(val offline: Boolean) : SessionRestorationUiState
    data class RetryableFailure(val message: String) : SessionRestorationUiState
}

class SessionRestorationViewModel(
    private val restoreSession: RestoreSession,
    private val repository: SessionRestorationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow<SessionRestorationUiState>(
        SessionRestorationUiState.Checking,
    )
    val state: StateFlow<SessionRestorationUiState> = mutableState.asStateFlow()

    init {
        restore()
    }

    fun retry() {
        restore()
    }

    fun signOut() {
        if (mutableState.value is SessionRestorationUiState.SigningOut) return
        mutableState.value = SessionRestorationUiState.SigningOut
        viewModelScope.launch(dispatcher) {
            mutableState.value = if (repository.signOut()) {
                SessionRestorationUiState.SignedOut(returningUser = true)
            } else {
                SessionRestorationUiState.RetryableFailure(
                    "Kiwi couldn’t sign out right now. Please try again.",
                )
            }
        }
    }

    private fun restore() {
        mutableState.value = SessionRestorationUiState.Checking
        viewModelScope.launch(dispatcher) {
            mutableState.value = restoreSession(Unit).toUiState()
        }
    }

    private fun SessionRestoration.toUiState(): SessionRestorationUiState = when (this) {
        is SessionRestoration.SignedOut -> SessionRestorationUiState.SignedOut(returningUser)
        SessionRestoration.AccessDenied -> SessionRestorationUiState.AccessDenied
        SessionRestoration.NeedsProfileSetup -> SessionRestorationUiState.NeedsProfileSetup
        is SessionRestoration.OpenToday -> SessionRestorationUiState.OpenToday(offline)
        is SessionRestoration.RetryableFailure -> SessionRestorationUiState.RetryableFailure(
            when (reason) {
                SessionRestorationFailure.OfflineApprovalRequired ->
                    "Connect to the internet once so Kiwi can confirm this account."
                SessionRestorationFailure.ConfigurationUnavailable ->
                    "Kiwi’s secure sign-in is not configured yet."
                SessionRestorationFailure.ServiceUnavailable ->
                    "Kiwi cannot restore your session right now. Please try again shortly."
            },
        )
    }

    class Factory(
        private val restoreSession: RestoreSession,
        private val repository: SessionRestorationRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SessionRestorationViewModel::class.java))
            return SessionRestorationViewModel(restoreSession, repository) as T
        }
    }
}
