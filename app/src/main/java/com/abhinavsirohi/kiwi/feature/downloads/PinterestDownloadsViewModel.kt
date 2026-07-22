package com.abhinavsirohi.kiwi.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PinterestDownloadsUiState(
    val url: String = "",
    val loading: Boolean = false,
    val media: PinterestMedia? = null,
    val message: String? = null,
    val downloadQueued: Boolean = false,
)

class PinterestDownloadsViewModel(
    private val extractor: PinterestExtractor,
    private val downloadGateway: PinterestDownloadGateway,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PinterestDownloadsUiState())
    val state: StateFlow<PinterestDownloadsUiState> = mutableState.asStateFlow()

    fun updateUrl(value: String) {
        mutableState.value = mutableState.value.copy(
            url = value,
            media = null,
            message = null,
            downloadQueued = false,
        )
    }

    fun acceptSharedText(value: String) {
        val url = PinterestUrlValidator.firstPinterestUrl(value)
        if (url == null) {
            mutableState.value = mutableState.value.copy(
                message = "The shared text doesn’t contain a public Pinterest link.",
            )
            return
        }
        updateUrl(url)
        preview()
    }

    fun preview() {
        if (mutableState.value.loading) return
        val normalized = PinterestUrlValidator.normalize(mutableState.value.url)
        if (normalized == null) {
            mutableState.value = mutableState.value.copy(
                message = "Paste a public Pinterest Pin link to continue.",
                media = null,
            )
            return
        }
        mutableState.value = mutableState.value.copy(
            url = normalized,
            loading = true,
            media = null,
            message = null,
            downloadQueued = false,
        )
        viewModelScope.launch(dispatcher) {
            when (val result = extractor.extract(normalized)) {
                is PinterestExtractionResult.Success -> mutableState.value = mutableState.value.copy(
                    loading = false,
                    media = result.media,
                )
                is PinterestExtractionResult.Failure -> mutableState.value = mutableState.value.copy(
                    loading = false,
                    message = result.message,
                )
            }
        }
    }

    fun download() {
        val media = mutableState.value.media ?: return
        runCatching { downloadGateway.enqueue(media) }
            .onSuccess {
                mutableState.value = mutableState.value.copy(
                    downloadQueued = true,
                    message = "Download started. Android will notify you when the video is saved in Movies/Kiwi.",
                )
            }
            .onFailure {
                mutableState.value = mutableState.value.copy(
                    message = "Kiwi couldn’t start the download. Check device storage and try again.",
                )
            }
    }

    fun storagePermissionDenied() {
        mutableState.value = mutableState.value.copy(
            message = "Storage permission is needed to save into Movies/Kiwi on this Android version.",
        )
    }

    class Factory(
        private val extractor: PinterestExtractor,
        private val downloadGateway: PinterestDownloadGateway,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PinterestDownloadsViewModel::class.java))
            return PinterestDownloadsViewModel(extractor, downloadGateway) as T
        }
    }
}
