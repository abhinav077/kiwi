package com.abhinavsirohi.kiwi.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.data.local.KiwiFontPreference
import com.abhinavsirohi.kiwi.data.local.KiwiSettings
import com.abhinavsirohi.kiwi.data.local.KiwiSettingsStore
import com.abhinavsirohi.kiwi.data.local.KiwiThemePreference
import com.abhinavsirohi.kiwi.data.local.LocalAccountDataManager
import com.abhinavsirohi.kiwi.data.local.dao.PendingChangeDao
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.data.repository.ApprovedUserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: KiwiSettings = KiwiSettings(),
    val pendingSyncCount: Int = 0,
    val processingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
    val busy: Boolean = false,
    val signedOut: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsStore: KiwiSettingsStore,
    private val pendingChangeDao: PendingChangeDao,
    private val accountDataManager: LocalAccountDataManager,
    private val sessionProvider: SessionProvider,
    private val approvedUserRepository: ApprovedUserRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            combine(
                settingsStore.settings,
                pendingChangeDao.observePendingCount(),
                pendingChangeDao.observeProcessingCount(),
                pendingChangeDao.observeFailedCount(),
            ) { settings, pending, processing, failed ->
                mutableState.value.copy(
                    settings = settings,
                    pendingSyncCount = pending,
                    processingSyncCount = processing,
                    failedSyncCount = failed,
                )
            }.collect(mutableState::emit)
        }
    }

    fun setTheme(value: KiwiThemePreference) = launchUpdate { settingsStore.setTheme(value) }

    fun setFont(value: KiwiFontPreference) = launchUpdate { settingsStore.setFont(value) }

    fun setNotificationPrivacy(value: Boolean) = launchUpdate { settingsStore.setHideNotificationContent(value) }

    fun setRecentScreenPrivacy(value: Boolean) = launchUpdate { settingsStore.setProtectRecentScreen(value) }

    fun setBiometricEnabled(value: Boolean) = launchUpdate { settingsStore.setBiometricEnabled(value) }

    fun configurePin(pin: String, confirmation: String) {
        if (pin != confirmation) {
            showMessage("The PINs do not match.")
            return
        }
        launchUpdate(successMessage = "App lock is ready.") {
            settingsStore.configurePin(pin.toCharArray())
        }
    }

    fun disableAppLock() = launchUpdate(successMessage = "App lock is off.") {
        settingsStore.disableAppLock()
    }

    fun export(destination: Uri) {
        launchBusy {
            val session = when (val result = sessionProvider.currentSession()) {
                is RemoteResult.Success -> result.value
                is RemoteResult.Failure -> {
                    showMessage("Sign in before exporting your private data.")
                    return@launchBusy
                }
            }
            accountDataManager.exportJson(destination, session.userId)
            showMessage("Private data exported. Diary photo files are not included.")
        }
    }

    fun signOut() {
        launchBusy {
            when (approvedUserRepository.signOut()) {
                is RemoteResult.Success -> mutableState.value = mutableState.value.copy(signedOut = true)
                is RemoteResult.Failure -> showMessage("Kiwi couldn’t sign out. Please try again.")
            }
        }
    }

    fun deleteLocalDataAndSignOut() {
        launchBusy {
            when (approvedUserRepository.signOut()) {
                is RemoteResult.Failure -> showMessage("Kiwi couldn’t sign out, so no local data was deleted.")
                is RemoteResult.Success -> {
                    accountDataManager.deleteAllLocalData()
                    mutableState.value = mutableState.value.copy(signedOut = true)
                }
            }
        }
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    private fun launchUpdate(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch(dispatcher) {
            runCatching { block() }
                .onSuccess { if (successMessage != null) showMessage(successMessage) }
                .onFailure { showMessage(it.message ?: "Kiwi couldn’t save that setting.") }
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, message = null)
        viewModelScope.launch(dispatcher) {
            runCatching { block() }
                .onFailure { showMessage(it.message ?: "Kiwi couldn’t complete that action.") }
            mutableState.value = mutableState.value.copy(busy = false)
        }
    }

    private fun showMessage(message: String) {
        mutableState.value = mutableState.value.copy(message = message)
    }

    class Factory(
        private val settingsStore: KiwiSettingsStore,
        private val pendingChangeDao: PendingChangeDao,
        private val accountDataManager: LocalAccountDataManager,
        private val sessionProvider: SessionProvider,
        private val approvedUserRepository: ApprovedUserRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(
                settingsStore,
                pendingChangeDao,
                accountDataManager,
                sessionProvider,
                approvedUserRepository,
            ) as T
        }
    }
}
