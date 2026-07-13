package com.abhinavsirohi.kiwi.feature.onboarding

import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.GoogleAuthGateway
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
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
class GoogleSignInViewModelTest {
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
    fun successfulTokenExchange_exposesAuthenticatedSession() = runTest(dispatcher) {
        val session = AuthenticatedSession("user-1", "approved@example.com")
        val viewModel = GoogleSignInViewModel(
            authGateway = FakeGoogleAuthGateway(RemoteResult.Success(session)),
            dispatcher = dispatcher,
        )

        viewModel.signIn(idToken = "google-id-token", nonce = "nonce")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(GoogleSignInUiState.Authenticated(session), viewModel.state.value)
    }

    @Test
    fun cancellation_isRecoverableAndExplicit() {
        val viewModel = GoogleSignInViewModel(
            authGateway = FakeGoogleAuthGateway(RemoteResult.Failure(RemoteError.Unknown)),
            dispatcher = dispatcher,
        )

        viewModel.credentialRequestStarted()
        viewModel.credentialRequestCancelled()

        assertEquals(
            GoogleSignInUiState.Cancelled(
                "Sign-in was cancelled. You can try again whenever you’re ready.",
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun networkFailure_showsCalmActionableMessage() = runTest(dispatcher) {
        val viewModel = GoogleSignInViewModel(
            authGateway = FakeGoogleAuthGateway(
                RemoteResult.Failure(RemoteError.NetworkUnavailable),
            ),
            dispatcher = dispatcher,
        )

        viewModel.signIn(idToken = "google-id-token", nonce = "nonce")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            GoogleSignInUiState.Error("You’re offline. Connect to the internet and try again."),
            viewModel.state.value,
        )
    }
}

private class FakeGoogleAuthGateway(
    private val result: RemoteResult<AuthenticatedSession>,
) : GoogleAuthGateway {
    override suspend fun signIn(
        idToken: String,
        nonce: String,
    ): RemoteResult<AuthenticatedSession> = result
}
