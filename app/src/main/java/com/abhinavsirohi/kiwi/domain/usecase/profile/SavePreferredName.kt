package com.abhinavsirohi.kiwi.domain.usecase.profile

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Profile
import com.abhinavsirohi.kiwi.domain.repository.ProfileRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

class PreferredNameRequiredException : IllegalArgumentException("Preferred name is required")

class SavePreferredName(
    private val profileRepository: ProfileRepository,
) : UseCase<String, AppResult<Profile>> {
    override suspend fun invoke(parameters: String): AppResult<Profile> {
        val preferredName = parameters.trim()
        if (preferredName.isEmpty()) return AppResult.Failure(PreferredNameRequiredException())
        return profileRepository.savePreferredName(preferredName)
    }
}
