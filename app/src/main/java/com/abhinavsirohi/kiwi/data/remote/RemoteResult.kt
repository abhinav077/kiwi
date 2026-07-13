package com.abhinavsirohi.kiwi.data.remote

sealed interface RemoteResult<out T> {
    data class Success<T>(val value: T) : RemoteResult<T>

    data class Failure(val error: RemoteError) : RemoteResult<Nothing>
}

sealed interface RemoteError {
    data object ConfigurationMissing : RemoteError
    data object ConfigurationInvalid : RemoteError
    data object AuthenticationRequired : RemoteError
    data object AccessDenied : RemoteError
    data object NetworkUnavailable : RemoteError
    data object ServiceUnavailable : RemoteError
    data object Unknown : RemoteError
}

fun RemoteError.userMessage(): String = when (this) {
    RemoteError.ConfigurationMissing,
    RemoteError.ConfigurationInvalid,
    -> "Kiwi’s secure backup is not configured yet."
    RemoteError.AuthenticationRequired -> "Please sign in again to continue securely."
    RemoteError.AccessDenied -> "This account does not have access to Kiwi data."
    RemoteError.NetworkUnavailable -> "Kiwi will try again when you’re back online."
    RemoteError.ServiceUnavailable -> "Secure backup is temporarily unavailable. Kiwi will try again later."
    RemoteError.Unknown -> "Something went wrong with secure backup. Your local changes are safe."
}
