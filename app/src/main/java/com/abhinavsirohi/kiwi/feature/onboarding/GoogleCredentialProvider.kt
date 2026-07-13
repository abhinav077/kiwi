package com.abhinavsirohi.kiwi.feature.onboarding

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom

sealed interface GoogleCredentialResult {
    data class Success(
        val idToken: String,
        val nonce: String,
    ) : GoogleCredentialResult

    data object Cancelled : GoogleCredentialResult
    data object ConfigurationMissing : GoogleCredentialResult
    data object Unavailable : GoogleCredentialResult
}

class GoogleCredentialProvider(
    private val credentialManagerFactory: (Context) -> CredentialManager = CredentialManager::create,
    private val nonceFactory: () -> String = ::createNonce,
) {
    suspend fun request(
        context: Context,
        serverClientId: String,
    ): GoogleCredentialResult {
        if (serverClientId.isBlank()) return GoogleCredentialResult.ConfigurationMissing

        val nonce = nonceFactory()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setNonce(nonce.sha256())
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credential = credentialManagerFactory(context)
                .getCredential(context = context, request = request)
                .credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleCredentialResult.Success(
                    idToken = googleCredential.idToken,
                    nonce = nonce,
                )
            } else {
                GoogleCredentialResult.Unavailable
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelled
        } catch (_: GetCredentialException) {
            GoogleCredentialResult.Unavailable
        } catch (_: IllegalArgumentException) {
            GoogleCredentialResult.Unavailable
        }
    }
}

private fun createNonce(): String {
    val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

private fun String.sha256(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8))
    .joinToString(separator = "") { byte -> "%02x".format(byte) }
