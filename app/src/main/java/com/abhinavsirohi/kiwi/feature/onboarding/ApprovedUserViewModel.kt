package com.abhinavsirohi.kiwi.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.repository.ApprovedUserAccess
import com.abhinavsirohi.kiwi.data.repository.ApprovedUserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ApprovedUserUiState {
    data object Checking : ApprovedUserUiState
    data class Approved(val session: AuthenticatedSession) : ApprovedUserUiState
    data object Denied : ApprovedUserUiState
    data class Error(val message: String) : ApprovedUserUiState
    data object SigningOut : ApprovedUserUiState
    data object SignedOut : ApprovedUserUiState
}

class ApprovedUserViewModel(
    private val repository: ApprovedUserRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ApprovedUserUiState>(ApprovedUserUiState.Checking)
    val state: StateFlow<ApprovedUserUiState> = mutableState.asStateFlow()

    init {
        checkAccess()
    }

    fun retry() {
        checkAccess()
    }

    fun signOut() {
        if (mutableState.value is ApprovedUserUiState.SigningOut) return
        mutableState.value = ApprovedUserUiState.SigningOut
        viewModelScope.launch(dispatcher) {
            mutableState.value = when (val result = repository.signOut()) {
                is RemoteResult.Success -> ApprovedUserUiState.SignedOut
                is RemoteResult.Failure -> ApprovedUserUiState.Error(result.error.accessMessage())
            }
        }
    }

    private fun checkAccess() {
        mutableState.value = ApprovedUserUiState.Checking
        viewModelScope.launch(dispatcher) {
            mutableState.value = when (val result = repository.checkAccess()) {
                is RemoteResult.Success -> when (val access = result.value) {
                    is ApprovedUserAccess.Approved -> ApprovedUserUiState.Approved(access.session)
                    ApprovedUserAccess.Denied -> ApprovedUserUiState.Denied
                }
                is RemoteResult.Failure -> ApprovedUserUiState.Error(result.error.accessMessage())
            }
        }
    }

    class Factory(
        private val repository: ApprovedUserRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ApprovedUserViewModel::class.java))
            return ApprovedUserViewModel(repository) as T
        }
    }
}

private fun RemoteError.accessMessage(): String = when (this) {
    RemoteError.ConfigurationMissing,
    RemoteError.ConfigurationInvalid,
    -> "Kiwi’s secure access check is not configured yet."
    RemoteError.AuthenticationRequired -> "Your session has ended. Sign in again to continue."
    RemoteError.AccessDenied -> "This account does not have access to Kiwi."
    RemoteError.NetworkUnavailable -> "Connect to the internet to check access, then try again."
    RemoteError.ServiceUnavailable -> "Kiwi cannot check access right now. Please try again shortly."
    RemoteError.Unknown -> "Kiwi could not check this account. Please try again."
}
