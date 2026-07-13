package com.abhinavsirohi.kiwi.domain.model

sealed interface SessionRestoration {
    data class SignedOut(val returningUser: Boolean) : SessionRestoration
    data object AccessDenied : SessionRestoration
    data object NeedsProfileSetup : SessionRestoration
    data class OpenToday(val offline: Boolean) : SessionRestoration
    data class RetryableFailure(val reason: SessionRestorationFailure) : SessionRestoration
}

enum class SessionRestorationFailure {
    OfflineApprovalRequired,
    ConfigurationUnavailable,
    ServiceUnavailable,
}
