package com.abhinavsirohi.kiwi.feature.onboarding

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.Profile
import com.abhinavsirohi.kiwi.domain.repository.ProfileRepository
import com.abhinavsirohi.kiwi.domain.usecase.profile.SavePreferredName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MinimalSetupViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun successfulSave_reachesSavedState() = runTest(dispatcher) {
        val profile = Profile("user-1", "user-1", "Abhi", 1_000L, 1_000L)
        val viewModel = MinimalSetupViewModel(
            SavePreferredName(FakeSetupProfileRepository(AppResult.Success(profile))),
            dispatcher,
        )

        viewModel.updatePreferredName(" Abhi ")
        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MinimalSetupUiState.Saved(profile), viewModel.state.value)
    }

    @Test
    fun blankName_showsCalmValidationMessage() = runTest(dispatcher) {
        val viewModel = MinimalSetupViewModel(
            SavePreferredName(FakeSetupProfileRepository(AppResult.Failure(IllegalStateException()))),
            dispatcher,
        )

        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            MinimalSetupUiState.Editing(
                message = "Tell Kiwi what you’d like to be called.",
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun localFailure_preservesDraftForRetry() = runTest(dispatcher) {
        val viewModel = MinimalSetupViewModel(
            SavePreferredName(
                FakeSetupProfileRepository(AppResult.Failure(IllegalStateException("database"))),
            ),
            dispatcher,
        )

        viewModel.updatePreferredName("Abhi")
        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            MinimalSetupUiState.Editing(
                preferredName = "Abhi",
                message = "Kiwi couldn’t save your name locally. Please try again.",
            ),
            viewModel.state.value,
        )
    }
}

private class FakeSetupProfileRepository(
    private val result: AppResult<Profile>,
) : ProfileRepository {
    override suspend fun savePreferredName(preferredName: String): AppResult<Profile> = result
}
