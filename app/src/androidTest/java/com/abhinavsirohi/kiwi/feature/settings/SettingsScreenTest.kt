package com.abhinavsirohi.kiwi.feature.settings

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.abhinavsirohi.kiwi.data.local.KiwiThemePreference
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsSurface_exposesThemePrivacyLockSyncAndAccountActions() {
        var selectedTheme: KiwiThemePreference? = null
        composeRule.setContent {
            KiwiTheme {
                SettingsScreen(
                    state = SettingsUiState(),
                    onTheme = { selectedTheme = it },
                    onFont = {},
                    onNotificationPrivacy = {},
                    onRecentScreenPrivacy = {},
                    onConfigurePin = { _, _ -> },
                    onDisableLock = {},
                    onBiometric = {},
                    onExport = {},
                    onSignOut = {},
                    onDeleteLocalData = {},
                    onOpenWellness = {},
                    onOpenReview = {},
                    onOpenSelfCare = {},
                    onOpenDownloads = {},
                    onClearMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("Make Kiwi feel like yours").assertExists()
        composeRule.onNodeWithText("Grove").performClick()
        assertEquals(KiwiThemePreference.GROVE, selectedTheme)
        composeRule.onNodeWithText("Hide notification content").assertExists()
        composeRule.onNodeWithText("Protect recent-screen preview").assertExists()
        composeRule.onNodeWithText("Set up PIN").assertExists()
        val settingsList = composeRule.onNodeWithTag(SETTINGS_LIST_TEST_TAG)
        settingsList.performScrollToNode(hasText("Sync status"))
        composeRule.onNodeWithText("Sync status").assertExists()
        settingsList.performScrollToNode(hasText("Private data and account"))
        composeRule.onNodeWithText("Private data and account").assertExists()
    }
}
