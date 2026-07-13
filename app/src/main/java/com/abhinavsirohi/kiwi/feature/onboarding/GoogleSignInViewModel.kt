package com.abhinavsirohi.kiwi.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.GoogleAuthGateway
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GoogleSignInUiState {
    data object Idle : GoogleSignInUiState
    data object Loading : GoogleSignInUiState
    data class Cancelled(val message: String) : GoogleSignInUiState
    data class Error(val message: String) : GoogleSignInUiState
    data class Authenticated(val session: AuthenticatedSession) : GoogleSignInUiState
}

class GoogleSignInViewModel(
    private val authGateway: GoogleAuthGateway,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow<GoogleSignInUiState>(GoogleSignInUiState.Idle)
    val state: StateFlow<GoogleSignInUiState> = mutableState.asStateFlow()

    fun credentialRequestStarted() {
        if (mutableState.value !is GoogleSignInUiState.Loading) {
            mutableState.value = GoogleSignInUiState.Loading
        }
    }

    fun credentialRequestCancelled() {
        mutableState.value = GoogleSignInUiState.Cancelled(
            message = "Sign-in was cancelled. You can try again whenever you’re ready.",
        )
    }

    fun credentialRequestFailed(message: String) {
        mutableState.value = GoogleSignInUiState.Error(message)
    }

    fun signIn(idToken: String, nonce: String) {
        if (idToken.isBlank() || nonce.isBlank()) {
            mutableState.value = GoogleSignInUiState.Error(
                message = "Google sign-in could not be completed. Please try again.",
            )
            return
        }
        mutableState.value = GoogleSignInUiState.Loading
        viewModelScope.launch(dispatcher) {
            mutableState.value = when (val result = authGateway.signIn(idToken, nonce)) {
                is RemoteResult.Success -> GoogleSignInUiState.Authenticated(result.value)
                is RemoteResult.Failure -> GoogleSignInUiState.Error(result.error.signInMessage())
            }
        }
    }

    class Factory(
        private val authGateway: GoogleAuthGateway,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GoogleSignInViewModel::class.java))
            return GoogleSignInViewModel(authGateway) as T
        }
    }
}

private fun RemoteError.signInMessage(): String = when (this) {
    RemoteError.ConfigurationMissing,
    RemoteError.ConfigurationInvalid,
    -> "Google sign-in is not configured yet."
    RemoteError.NetworkUnavailable -> "You’re offline. Connect to the internet and try again."
    RemoteError.ServiceUnavailable -> "Sign-in is temporarily unavailable. Please try again shortly."
    RemoteError.AuthenticationRequired -> "Google could not confirm this sign-in. Please try again."
    RemoteError.AccessDenied -> "This account cannot continue with Kiwi."
    RemoteError.Unknown -> "Google sign-in could not be completed. Please try again."
}
