package com.abhinavsirohi.kiwi.data.remote

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException

object RemoteErrorMapper {
    fun map(throwable: Throwable): RemoteError = when (throwable) {
        is HttpRequestTimeoutException,
        is IOException,
        -> RemoteError.NetworkUnavailable
        is ClientRequestException -> when (throwable.response.status.value) {
            401 -> RemoteError.AuthenticationRequired
            403 -> RemoteError.AccessDenied
            else -> RemoteError.Unknown
        }
        is ServerResponseException -> RemoteError.ServiceUnavailable
        else -> RemoteError.Unknown
    }
}
