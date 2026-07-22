package com.abhinavsirohi.kiwi.feature.downloads

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinterestDownloadsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sharedPinterestLink_loadsPreview_andQueuesDownload() = runTest {
        val media = PinterestMedia(
            sourceUrl = "https://www.pinterest.com/pin/123/",
            mediaUrl = "https://v1.pinimg.com/video.mp4",
            previewUrl = "https://i.pinimg.com/preview.jpg",
            title = "Garden",
            attribution = "Studio",
            mimeType = "video/mp4",
        )
        var queued: PinterestMedia? = null
        val viewModel = PinterestDownloadsViewModel(
            extractor = PinterestExtractor { PinterestExtractionResult.Success(media) },
            downloadGateway = PinterestDownloadGateway { queued = it; 42L },
            dispatcher = dispatcher,
        )

        viewModel.acceptSharedText("Look at this https://www.pinterest.com/pin/123/")
        advanceUntilIdle()

        assertEquals(media, viewModel.state.value.media)
        assertFalse(viewModel.state.value.loading)
        viewModel.download()
        assertEquals(media, queued)
        assertTrue(viewModel.state.value.downloadQueued)
    }

    @Test
    fun invalidSharedText_reportsCalmFailure_withoutExtraction() {
        var extracted = false
        val viewModel = PinterestDownloadsViewModel(
            extractor = PinterestExtractor { extracted = true; PinterestExtractionResult.Failure("failure") },
            downloadGateway = PinterestDownloadGateway { 1L },
            dispatcher = dispatcher,
        )

        viewModel.acceptSharedText("No link here")

        assertFalse(extracted)
        assertEquals("The shared text doesn’t contain a public Pinterest link.", viewModel.state.value.message)
    }
}
