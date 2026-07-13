package com.abhinavsirohi.kiwi.domain.repository

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Profile

interface ProfileRepository {
    suspend fun savePreferredName(preferredName: String): AppResult<Profile>
}
