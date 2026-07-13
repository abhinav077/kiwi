package com.abhinavsirohi.kiwi.domain.usecase.selfcare

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.RecordMetadata
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.repository.SelfCareRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfCareUseCasesTest {
    @Test
    fun createTrimsNameDescriptionAndChecklist() = runTest {
        val repository = FakeSelfCareRepository()
        val result = CreateSelfCareRoutine(repository)(NewSelfCareRoutine("  Stretch  ", "  A pause  ", checklist = listOf("  Breathe  ", " ")))

        assertTrue(result is AppResult.Success)
        assertEquals("Stretch", repository.created?.name)
        assertEquals("A pause", repository.created?.description)
        assertEquals(listOf("Breathe"), repository.created?.checklist)
    }

    @Test
    fun createRejectsBlankNameAndInvalidTime() = runTest {
        assertTrue(CreateSelfCareRoutine(FakeSelfCareRepository())(NewSelfCareRoutine(" ")) is AppResult.Failure)
        assertTrue(CreateSelfCareRoutine(FakeSelfCareRepository())(NewSelfCareRoutine("Walk", scheduledTimeMinutes = 1440)) is AppResult.Failure)
    }

    private class FakeSelfCareRepository : SelfCareRepository {
        var created: NewSelfCareRoutine? = null
        override fun observeRoutines(): Flow<AppResult<List<SelfCareRoutine>>> = emptyFlow()
        override suspend fun createRoutine(routine: NewSelfCareRoutine): AppResult<SelfCareRoutine> {
            created = routine
            return AppResult.Success(SelfCareRoutine("id", routine.name, routine.description, metadata = RecordMetadata(userId = "user", createdAt = 0, updatedAt = 0, deviceId = "device")))
        }
        override suspend fun saveRoutine(routine: SelfCareRoutine) = AppResult.Success(Unit)
        override suspend fun tombstoneRoutine(localId: String, deletedAt: Long) = AppResult.Success(Unit)
    }
}
