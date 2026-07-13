package com.abhinavsirohi.kiwi.data.repository

import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.domain.model.SessionRestoration
import com.abhinavsirohi.kiwi.domain.model.SessionRestorationFailure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultSessionRestorationRepositoryTest {
    private val session = AuthenticatedSession("user-1", "approved@example.com")

    @Test
    fun approvedSessionWithMatchingProfile_opensTodayOnline() = runBlocking {
        assertEquals(
            SessionRestoration.OpenToday(offline = false),
            repository(localUserIds = setOf("user-1")).restore(),
        )
    }

    @Test
    fun approvedSessionWithoutProfile_opensMinimalSetup() = runBlocking {
        assertEquals(SessionRestoration.NeedsProfileSetup, repository().restore())
    }

    @Test
    fun deniedSession_neverOpensLocalData() = runBlocking {
        assertEquals(
            SessionRestoration.AccessDenied,
            repository(
                accessResult = RemoteResult.Success(ApprovedUserAccess.Denied),
                localUserIds = setOf("user-1"),
            ).restore(),
        )
    }

    @Test
    fun offlineSessionWithMatchingApprovedProfile_opensTodayLocally() = runBlocking {
        assertEquals(
            SessionRestoration.OpenToday(offline = true),
            repository(
                accessResult = RemoteResult.Failure(RemoteError.NetworkUnavailable),
                localUserIds = setOf("user-1"),
            ).restore(),
        )
    }

    @Test
    fun offlineSessionWithoutMatchingProfile_requiresOnlineApproval() = runBlocking {
        assertEquals(
            SessionRestoration.RetryableFailure(
                SessionRestorationFailure.OfflineApprovalRequired,
            ),
            repository(
                accessResult = RemoteResult.Failure(RemoteError.NetworkUnavailable),
                localUserIds = setOf("another-user"),
            ).restore(),
        )
    }

    @Test
    fun missingSessionWithNoProfile_isFirstLaunch() = runBlocking {
        assertEquals(
            SessionRestoration.SignedOut(returningUser = false),
            repository(
                sessionResult = RemoteResult.Failure(RemoteError.AuthenticationRequired),
            ).restore(),
        )
    }

    @Test
    fun expiredSessionWithLocalProfile_isReturningSignIn() = runBlocking {
        assertEquals(
            SessionRestoration.SignedOut(returningUser = true),
            repository(
                sessionResult = RemoteResult.Failure(RemoteError.AuthenticationRequired),
                localUserIds = setOf("user-1"),
            ).restore(),
        )
    }

    @Test
    fun sessionStorage_isInitializedBeforeCachedSessionIsRead() = runBlocking {
        val events = mutableListOf<String>()
        val repository = DefaultSessionRestorationRepository(
            sessionProvider = object : SessionProvider {
                override fun currentSession(): RemoteResult<AuthenticatedSession> {
                    events += "session"
                    return RemoteResult.Success(session)
                }
            },
            approvedUserRepository = SessionRestoreApprovedUserRepository(
                RemoteResult.Success(ApprovedUserAccess.Approved(session)),
            ),
            localProfiles = FakeLocalProfileLookup(setOf("user-1")),
            awaitSessionInitialization = { events += "initialized" },
        )

        repository.restore()

        assertEquals(listOf("initialized", "session"), events)
    }

    private fun repository(
        sessionResult: RemoteResult<AuthenticatedSession> = RemoteResult.Success(session),
        accessResult: RemoteResult<ApprovedUserAccess> = RemoteResult.Success(
            ApprovedUserAccess.Approved(session),
        ),
        localUserIds: Set<String> = emptySet(),
    ) = DefaultSessionRestorationRepository(
        sessionProvider = object : SessionProvider {
            override fun currentSession() = sessionResult
        },
        approvedUserRepository = SessionRestoreApprovedUserRepository(accessResult),
        localProfiles = FakeLocalProfileLookup(localUserIds),
    )
}

private class SessionRestoreApprovedUserRepository(
    private val accessResult: RemoteResult<ApprovedUserAccess>,
) : ApprovedUserRepository {
    override suspend fun checkAccess() = accessResult
    override suspend fun signOut(): RemoteResult<Unit> = RemoteResult.Success(Unit)
}

private class FakeLocalProfileLookup(
    private val userIds: Set<String>,
) : LocalProfileLookup {
    override suspend fun hasProfileFor(userId: String): Boolean = userId in userIds
    override suspend fun hasAnyProfile(): Boolean = userIds.isNotEmpty()
}
