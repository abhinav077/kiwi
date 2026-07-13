package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.domain.model.SessionRestoration

interface SessionRestorationRepository {
    suspend fun restore(): SessionRestoration
    suspend fun signOut(): Boolean
}
