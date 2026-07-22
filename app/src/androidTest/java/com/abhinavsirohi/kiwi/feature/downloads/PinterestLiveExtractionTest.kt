package com.abhinavsirohi.kiwi.feature.downloads

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinterestLiveExtractionTest {
    @Test
    fun providedPublicStoryPin_exposesDownloadableMp4() = runBlocking {
        assumeTrue(
            "Live Pinterest checks run only when explicitly requested.",
            InstrumentationRegistry.getArguments().getString(RUN_LIVE_ARGUMENT) == "true",
        )

        val result = PublicPinterestExtractor().extract(PROVIDED_PUBLIC_PIN)

        assertTrue(
            "Expected public Story Pin extraction to succeed, but received $result",
            result is PinterestExtractionResult.Success &&
                result.media.mediaUrl.startsWith("https://") &&
                result.media.mediaUrl.contains(".pinimg.com/") &&
                result.media.mediaUrl.substringBefore('?').endsWith(".mp4"),
        )
    }

    private companion object {
        const val RUN_LIVE_ARGUMENT = "runPinterestLive"
        const val PROVIDED_PUBLIC_PIN = "https://pin.it/63U40vfq6"
    }
}
