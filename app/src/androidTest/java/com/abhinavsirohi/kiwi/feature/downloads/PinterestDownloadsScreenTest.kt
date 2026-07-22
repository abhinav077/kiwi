package com.abhinavsirohi.kiwi.feature.downloads

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PinterestDownloadsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun previewSurface_showsAttributionAndHighContrastSaveAction() {
        var saveClicked = false
        composeRule.setContent {
            KiwiTheme {
                PinterestDownloadsScreen(
                    state = PinterestDownloadsUiState(
                        url = "https://www.pinterest.com/pin/123/",
                        media = PinterestMedia(
                            sourceUrl = "https://www.pinterest.com/pin/123/",
                            mediaUrl = "https://v1.pinimg.com/video.mp4",
                            previewUrl = null,
                            title = "A quiet garden",
                            attribution = "Garden Studio",
                            mimeType = "video/mp4",
                        ),
                    ),
                    onUrlChanged = {},
                    onPaste = {},
                    onPreview = {},
                    onDownload = { saveClicked = true },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Keep an inspiring find close").assertExists()
        composeRule.onNodeWithText("Source: Garden Studio").assertExists()
        composeRule.onNodeWithText("Save to device").performClick()
        assertTrue(saveClicked)
    }
}
