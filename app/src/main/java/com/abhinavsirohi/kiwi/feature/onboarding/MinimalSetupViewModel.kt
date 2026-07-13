package com.abhinavsirohi.kiwi.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Profile
import com.abhinavsirohi.kiwi.domain.usecase.profile.PreferredNameRequiredException
import com.abhinavsirohi.kiwi.domain.usecase.profile.SavePreferredName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MinimalSetupUiState {
    data class Editing(
        val preferredName: String = "",
        val isSaving: Boolean = false,
        val message: String? = null,
    ) : MinimalSetupUiState

    data class Saved(val profile: Profile) : MinimalSetupUiState
}

class MinimalSetupViewModel(
    private val savePreferredName: SavePreferredName,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow<MinimalSetupUiState>(MinimalSetupUiState.Editing())
    val state: StateFlow<MinimalSetupUiState> = mutableState.asStateFlow()

    fun updatePreferredName(value: String) {
        val current = mutableState.value as? MinimalSetupUiState.Editing ?: return
        mutableState.value = current.copy(preferredName = value, message = null)
    }

    fun save() {
        val current = mutableState.value as? MinimalSetupUiState.Editing ?: return
        if (current.isSaving) return
        mutableState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch(dispatcher) {
            mutableState.value = when (val result = savePreferredName(current.preferredName)) {
                is AppResult.Success -> MinimalSetupUiState.Saved(result.value)
                is AppResult.Failure -> current.copy(
                    isSaving = false,
                    message = if (result.cause is PreferredNameRequiredException) {
                        "Tell Kiwi what you’d like to be called."
                    } else {
                        "Kiwi couldn’t save your name locally. Please try again."
                    },
                )
            }
        }
    }

    class Factory(
        private val savePreferredName: SavePreferredName,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MinimalSetupViewModel::class.java))
            return MinimalSetupViewModel(savePreferredName) as T
        }
    }
}
