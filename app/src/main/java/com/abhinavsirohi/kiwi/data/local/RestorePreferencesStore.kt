package com.abhinavsirohi.kiwi.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abhinavsirohi.kiwi.core.sync.restore.RestoreStateStore
import kotlinx.coroutines.flow.first

private val Context.restoreDataStore by preferencesDataStore(name = "restore_state")

class RestorePreferencesStore(
    private val context: Context,
) : RestoreStateStore {
    override suspend fun isComplete(userId: String): Boolean =
        context.restoreDataStore.data.first()[RESTORED_USER_IDS]?.contains(userId) == true

    override suspend fun markComplete(userId: String) {
        context.restoreDataStore.edit { preferences ->
            val restoredUsers = preferences[RESTORED_USER_IDS].orEmpty()
            preferences[RESTORED_USER_IDS] = restoredUsers + userId
        }
    }

    private companion object {
        val RESTORED_USER_IDS = stringSetPreferencesKey("restored_user_ids")
    }
}
