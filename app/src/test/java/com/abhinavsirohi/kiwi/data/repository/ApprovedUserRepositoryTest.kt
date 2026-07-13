package com.abhinavsirohi.kiwi.data.repository

import com.abhinavsirohi.kiwi.data.remote.AuthenticatedSession
import org.junit.Assert.assertEquals
import org.junit.Test

class ApprovedUserRepositoryTest {
    private val session = AuthenticatedSession(
        userId = "user-1",
        email = "Approved@Example.com",
    )

    @Test
    fun activeMatchingRow_isApprovedCaseInsensitively() {
        val result = evaluateApprovedUser(
            session = session,
            record = AllowedUserRecord(email = "approved@example.com", isActive = true),
        )

        assertEquals(ApprovedUserAccess.Approved(session), result)
    }

    @Test
    fun emptyRlsResult_isDenied() {
        assertEquals(ApprovedUserAccess.Denied, evaluateApprovedUser(session, record = null))
    }

    @Test
    fun inactiveOrMismatchedRows_areDeniedLocally() {
        assertEquals(
            ApprovedUserAccess.Denied,
            evaluateApprovedUser(
                session,
                AllowedUserRecord(email = "approved@example.com", isActive = false),
            ),
        )
        assertEquals(
            ApprovedUserAccess.Denied,
            evaluateApprovedUser(
                session,
                AllowedUserRecord(email = "different@example.com", isActive = true),
            ),
        )
    }
}
