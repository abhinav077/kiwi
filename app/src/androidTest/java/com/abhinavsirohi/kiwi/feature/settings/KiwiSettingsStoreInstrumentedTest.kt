package com.abhinavsirohi.kiwi.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhinavsirohi.kiwi.data.local.KiwiSettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KiwiSettingsStoreInstrumentedTest {
    @Test
    fun configuredPin_verifiesOnlyMatchingDigits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = KiwiSettingsStore(context)
        store.clearAll()
        try {
            store.configurePin("4826".toCharArray())
            assertTrue(store.verifyPin("4826".toCharArray()))
            assertFalse(store.verifyPin("4827".toCharArray()))
        } finally {
            store.clearAll()
        }
    }
}
