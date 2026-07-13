package com.abhinavsirohi.kiwi.domain.usecase.profile

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Profile
import com.abhinavsirohi.kiwi.domain.repository.ProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavePreferredNameTest {
    private val repository = FakeProfileRepository()
    private val useCase = SavePreferredName(repository)

    @Test
    fun trimsAndSavesPreferredName() = runBlocking {
        val result = useCase("  Abhi  ")

        assertEquals("Abhi", repository.savedName)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun blankNameFailsBeforeRepository() = runBlocking {
        val result = useCase("   ")

        assertTrue((result as AppResult.Failure).cause is PreferredNameRequiredException)
        assertEquals(null, repository.savedName)
    }
}

private class FakeProfileRepository : ProfileRepository {
    var savedName: String? = null

    override suspend fun savePreferredName(preferredName: String): AppResult<Profile> {
        savedName = preferredName
        return AppResult.Success(
            Profile("user-1", "user-1", preferredName, 1_000L, 1_000L),
        )
    }
}
