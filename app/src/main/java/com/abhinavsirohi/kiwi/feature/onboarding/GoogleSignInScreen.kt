package com.abhinavsirohi.kiwi.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.BuildConfig
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.data.remote.SupabaseGoogleAuthGateway
import com.abhinavsirohi.kiwi.ui.theme.KiwiDimensions
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInRoute(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as KiwiApplication
    val signInViewModel: GoogleSignInViewModel = viewModel(
        factory = GoogleSignInViewModel.Factory(
            authGateway = SupabaseGoogleAuthGateway(application.supabaseClient),
        ),
    )
    val state by signInViewModel.state.collectAsState()
    val credentialProvider = remember { GoogleCredentialProvider() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state) {
        if (state is GoogleSignInUiState.Authenticated) onAuthenticated()
    }

    GoogleSignInScreen(
        state = state,
        onSignIn = {
            if (state !is GoogleSignInUiState.Loading) {
                signInViewModel.credentialRequestStarted()
                coroutineScope.launch {
                    when (
                        val result = credentialProvider.request(
                            context = context,
                            serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                        )
                    ) {
                        is GoogleCredentialResult.Success -> signInViewModel.signIn(
                            idToken = result.idToken,
                            nonce = result.nonce,
                        )
                        GoogleCredentialResult.Cancelled -> signInViewModel.credentialRequestCancelled()
                        GoogleCredentialResult.ConfigurationMissing -> {
                            signInViewModel.credentialRequestFailed("Google sign-in is not configured yet.")
                        }
                        GoogleCredentialResult.Unavailable -> {
                            signInViewModel.credentialRequestFailed(
                                "Google sign-in is unavailable on this device. Please try again.",
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun GoogleSignInScreen(
    state: GoogleSignInUiState,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loading = state is GoogleSignInUiState.Loading
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = KiwiSpacing.xl, vertical = KiwiSpacing.jumbo),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Welcome to your space",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KiwiSpacing.md))
            Text(
                text = "Continue with Google to keep Kiwi private and connected to your approved account.",
                style = MaterialTheme.typography.bodyLarge,
                color = KiwiWarmGray,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KiwiSpacing.xxxl))
            Button(
                onClick = onSignIn,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KiwiDimensions.minimumTouchTarget),
                shape = RoundedCornerShape(KiwiDimensions.chipRadius),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = "Continue with Google")
                }
            }
            SignInStatus(state = state)
        }
    }
}

@Composable
private fun SignInStatus(state: GoogleSignInUiState) {
    val message = when (state) {
        GoogleSignInUiState.Idle,
        GoogleSignInUiState.Loading,
        is GoogleSignInUiState.Authenticated,
        -> null
        is GoogleSignInUiState.Cancelled -> state.message
        is GoogleSignInUiState.Error -> state.message
    }
    if (message != null) {
        Spacer(modifier = Modifier.height(KiwiSpacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun GoogleSignInScreenPreview() {
    KiwiTheme {
        GoogleSignInScreen(
            state = GoogleSignInUiState.Idle,
            onSignIn = {},
        )
    }
}
