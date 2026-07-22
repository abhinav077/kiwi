package com.abhinavsirohi.kiwi

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.abhinavsirohi.kiwi.core.navigation.KiwiApp
import com.abhinavsirohi.kiwi.data.local.KiwiSettings
import com.abhinavsirohi.kiwi.feature.settings.AppLockGate
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme

class MainActivity : FragmentActivity() {
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedText = sharedTextFrom(intent)
        enableEdgeToEdge()
        setContent {
            val application = application as KiwiApplication
            val settings by application.settingsStore.settings.collectAsState(initial = KiwiSettings())
            DisposableEffect(settings.protectRecentScreen) {
                if (settings.protectRecentScreen) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose { }
            }
            KiwiTheme(themePreference = settings.theme, fontPreference = settings.font) {
                AppLockGate(settings, application.settingsStore, this@MainActivity) {
                    KiwiApp(
                        sharedText = sharedText,
                        onSharedTextConsumed = { sharedText = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = sharedTextFrom(intent)
    }

    private fun sharedTextFrom(intent: Intent?): String? = intent
        ?.takeIf { it.action == Intent.ACTION_SEND && it.type == "text/plain" }
        ?.getStringExtra(Intent.EXTRA_TEXT)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
}
