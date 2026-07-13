package com.abhinavsirohi.kiwi.feature.onboarding

import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.repository.ApprovedUserAccess
import com.abhinavsirohi.kiwi.data.repository.ApprovedUserRepository
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
class ApprovedUserViewModelTest {
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
    fun approvedAccount_continuesToSetup() = runTest(dispatcher) {
        val session = AuthenticatedSession("user-1", "approved@example.com")
        val viewModel = ApprovedUserViewModel(
            repository = FakeApprovedUserRepository(
                accessResults = ArrayDeque(
                    listOf(RemoteResult.Success(ApprovedUserAccess.Approved(session))),
                ),
            ),
            dispatcher = dispatcher,
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ApprovedUserUiState.Approved(session), viewModel.state.value)
    }

    @Test
    fun rlsFilteredAccount_showsAccessDenied() = runTest(dispatcher) {
        val viewModel = ApprovedUserViewModel(
            repository = FakeApprovedUserRepository(
                accessResults = ArrayDeque(listOf(RemoteResult.Success(ApprovedUserAccess.Denied))),
            ),
            dispatcher = dispatcher,
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ApprovedUserUiState.Denied, viewModel.state.value)
    }

    @Test
    fun missingSession_requiresSignInAgain() = runTest(dispatcher) {
        val viewModel = ApprovedUserViewModel(
            repository = FakeApprovedUserRepository(
                accessResults = ArrayDeque(
                    listOf(RemoteResult.Failure(RemoteError.AuthenticationRequired)),
                ),
            ),
            dispatcher = dispatcher,
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            ApprovedUserUiState.Error("Your session has ended. Sign in again to continue."),
            viewModel.state.value,
        )
    }

    @Test
    fun networkFailure_canRetrySuccessfully() = runTest(dispatcher) {
        val session = AuthenticatedSession("user-1", "approved@example.com")
        val repository = FakeApprovedUserRepository(
            accessResults = ArrayDeque(
                listOf(
                    RemoteResult.Failure(RemoteError.NetworkUnavailable),
                    RemoteResult.Success(ApprovedUserAccess.Approved(session)),
                ),
            ),
        )
        val viewModel = ApprovedUserViewModel(repository, dispatcher)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            ApprovedUserUiState.Error("Connect to the internet to check access, then try again."),
            viewModel.state.value,
        )

        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ApprovedUserUiState.Approved(session), viewModel.state.value)
    }

    @Test
    fun signOut_clearsSessionAndReturnsToSignIn() = runTest(dispatcher) {
        val repository = FakeApprovedUserRepository(
            accessResults = ArrayDeque(listOf(RemoteResult.Success(ApprovedUserAccess.Denied))),
            signOutResult = RemoteResult.Success(Unit),
        )
        val viewModel = ApprovedUserViewModel(repository, dispatcher)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.signOut()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ApprovedUserUiState.SignedOut, viewModel.state.value)
        assertEquals(1, repository.signOutCalls)
    }
}

private class FakeApprovedUserRepository(
    private val accessResults: ArrayDeque<RemoteResult<ApprovedUserAccess>>,
    private val signOutResult: RemoteResult<Unit> = RemoteResult.Success(Unit),
) : ApprovedUserRepository {
    var signOutCalls: Int = 0
        private set

    override suspend fun checkAccess(): RemoteResult<ApprovedUserAccess> = accessResults.removeFirst()

    override suspend fun signOut(): RemoteResult<Unit> {
        signOutCalls += 1
        return signOutResult
    }
}
