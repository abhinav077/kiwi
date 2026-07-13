package com.abhinavsirohi.kiwi.data.repository

import com.abhinavsirohi.kiwi.data.local.dao.ProfileDao
import com.abhinavsirohi.kiwi.data.remote.RemoteError
import com.abhinavsirohi.kiwi.data.remote.RemoteResult
import com.abhinavsirohi.kiwi.data.remote.SessionProvider
import com.abhinavsirohi.kiwi.domain.model.SessionRestoration
import com.abhinavsirohi.kiwi.domain.model.SessionRestorationFailure
import com.abhinavsirohi.kiwi.domain.repository.SessionRestorationRepository

class DefaultSessionRestorationRepository(
    private val sessionProvider: SessionProvider,
    private val approvedUserRepository: ApprovedUserRepository,
    private val localProfiles: LocalProfileLookup,
    private val awaitSessionInitialization: suspend () -> Unit = {},
) : SessionRestorationRepository {
    override suspend fun restore(): SessionRestoration {
        awaitSessionInitialization()
        val session = when (val result = sessionProvider.currentSession()) {
            is RemoteResult.Success -> result.value
            is RemoteResult.Failure -> return if (result.error == RemoteError.AuthenticationRequired) {
                SessionRestoration.SignedOut(returningUser = localProfiles.hasAnyProfile())
            } else {
                result.error.toSessionFailure()
            }
        }

        return when (val result = approvedUserRepository.checkAccess()) {
            is RemoteResult.Success -> when (result.value) {
                is ApprovedUserAccess.Approved -> {
                    if (!localProfiles.hasProfileFor(session.userId)) {
                        SessionRestoration.NeedsProfileSetup
                    } else {
                        SessionRestoration.OpenToday(offline = false)
                    }
                }
                ApprovedUserAccess.Denied -> SessionRestoration.AccessDenied
            }
            is RemoteResult.Failure -> when (result.error) {
                RemoteError.NetworkUnavailable -> {
                    if (localProfiles.hasProfileFor(session.userId)) {
                        SessionRestoration.OpenToday(offline = true)
                    } else {
                        SessionRestoration.RetryableFailure(
                            SessionRestorationFailure.OfflineApprovalRequired,
                        )
                    }
                }
                else -> result.error.toSessionFailure()
            }
        }
    }

    override suspend fun signOut(): Boolean =
        approvedUserRepository.signOut() is RemoteResult.Success

    private suspend fun RemoteError.toSessionFailure(): SessionRestoration = when (this) {
        RemoteError.AuthenticationRequired -> SessionRestoration.SignedOut(
            returningUser = localProfiles.hasAnyProfile(),
        )
        RemoteError.AccessDenied -> SessionRestoration.AccessDenied
        RemoteError.ConfigurationMissing,
        RemoteError.ConfigurationInvalid,
        -> SessionRestoration.RetryableFailure(
            SessionRestorationFailure.ConfigurationUnavailable,
        )
        RemoteError.NetworkUnavailable -> SessionRestoration.RetryableFailure(
            SessionRestorationFailure.OfflineApprovalRequired,
        )
        RemoteError.ServiceUnavailable,
        RemoteError.Unknown,
        -> SessionRestoration.RetryableFailure(SessionRestorationFailure.ServiceUnavailable)
    }
}

interface LocalProfileLookup {
    suspend fun hasProfileFor(userId: String): Boolean
    suspend fun hasAnyProfile(): Boolean
}

class RoomLocalProfileLookup(
    private val profileDao: ProfileDao,
) : LocalProfileLookup {
    override suspend fun hasProfileFor(userId: String): Boolean =
        profileDao.findByUserId(userId) != null

    override suspend fun hasAnyProfile(): Boolean = profileDao.hasAnyProfile()
}
