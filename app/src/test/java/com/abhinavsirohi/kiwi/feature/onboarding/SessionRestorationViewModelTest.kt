package com.abhinavsirohi.kiwi.feature.onboarding

import com.abhinavsirohi.kiwi.domain.model.SessionRestoration
import com.abhinavsirohi.kiwi.domain.model.SessionRestorationFailure
import com.abhinavsirohi.kiwi.domain.repository.SessionRestorationRepository
import com.abhinavsirohi.kiwi.domain.usecase.session.RestoreSession
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
class SessionRestorationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validSession_routesDirectlyToToday() = runTest(dispatcher) {
        val repository = FakeSessionRestorationRepository(
            results = ArrayDeque(listOf(SessionRestoration.OpenToday(offline = false))),
        )
        val viewModel = SessionRestorationViewModel(
            RestoreSession(repository),
            repository,
            dispatcher,
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SessionRestorationUiState.OpenToday(offline = false), viewModel.state.value)
    }

    @Test
    fun offlineWithoutApprovedProfile_canRetry() = runTest(dispatcher) {
        val repository = FakeSessionRestorationRepository(
            results = ArrayDeque(
                listOf(
                    SessionRestoration.RetryableFailure(
                        SessionRestorationFailure.OfflineApprovalRequired,
                    ),
                    SessionRestoration.OpenToday(offline = false),
                ),
            ),
        )
        val viewModel = SessionRestorationViewModel(
            RestoreSession(repository),
            repository,
            dispatcher,
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            SessionRestorationUiState.RetryableFailure(
                "Connect to the internet once so Kiwi can confirm this account.",
            ),
            viewModel.state.value,
        )

        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SessionRestorationUiState.OpenToday(offline = false), viewModel.state.value)
    }

    @Test
    fun retryScreen_canSignOut() = runTest(dispatcher) {
        val repository = FakeSessionRestorationRepository(
            results = ArrayDeque(
                listOf(
                    SessionRestoration.RetryableFailure(
                        SessionRestorationFailure.ServiceUnavailable,
                    ),
                ),
            ),
        )
        val viewModel = SessionRestorationViewModel(
            RestoreSession(repository),
            repository,
            dispatcher,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.signOut()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            SessionRestorationUiState.SignedOut(returningUser = true),
            viewModel.state.value,
        )
    }
}

private class FakeSessionRestorationRepository(
    private val results: ArrayDeque<SessionRestoration>,
) : SessionRestorationRepository {
    override suspend fun restore(): SessionRestoration = results.removeFirst()
    override suspend fun signOut(): Boolean = true
}
