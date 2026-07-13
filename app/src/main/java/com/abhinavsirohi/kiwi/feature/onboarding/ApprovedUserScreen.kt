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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.data.repository.SupabaseApprovedUserRepository
import com.abhinavsirohi.kiwi.ui.theme.KiwiDimensions
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray

@Composable
fun ApprovedUserRoute(
    onApproved: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.applicationContext as KiwiApplication
    val approvedUserViewModel: ApprovedUserViewModel = viewModel(
        factory = ApprovedUserViewModel.Factory(
            repository = SupabaseApprovedUserRepository(application.supabaseClient),
        ),
    )
    val state by approvedUserViewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            is ApprovedUserUiState.Approved -> onApproved()
            ApprovedUserUiState.SignedOut -> onSignedOut()
            else -> Unit
        }
    }

    ApprovedUserScreen(
        state = state,
        onRetry = approvedUserViewModel::retry,
        onSignOut = approvedUserViewModel::signOut,
        modifier = modifier,
    )
}

@Composable
fun ApprovedUserScreen(
    state: ApprovedUserUiState,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = KiwiSpacing.xl, vertical = KiwiSpacing.jumbo),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state) {
                ApprovedUserUiState.Checking,
                ApprovedUserUiState.SigningOut,
                is ApprovedUserUiState.Approved,
                ApprovedUserUiState.SignedOut,
                -> CheckingAccess(state)
                ApprovedUserUiState.Denied -> AccessDenied(
                    onRetry = onRetry,
                    onSignOut = onSignOut,
                )
                is ApprovedUserUiState.Error -> AccessError(
                    message = state.message,
                    onRetry = onRetry,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

@Composable
private fun CheckingAccess(state: ApprovedUserUiState) {
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(KiwiSpacing.lg))
    Text(
        text = if (state is ApprovedUserUiState.SigningOut) "Signing out…" else "Checking your Kiwi access…",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AccessDenied(
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Text(
        text = "Access denied",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(KiwiSpacing.md))
    Text(
        text = "Kiwi is a private space for one approved account.",
        style = MaterialTheme.typography.bodyLarge,
        color = KiwiWarmGray,
        textAlign = TextAlign.Center,
    )
    AccessActions(onRetry = onRetry, onSignOut = onSignOut)
}

@Composable
private fun AccessError(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Text(
        text = "We couldn’t check access",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(KiwiSpacing.md))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = KiwiWarmGray,
        textAlign = TextAlign.Center,
    )
    AccessActions(onRetry = onRetry, onSignOut = onSignOut)
}

@Composable
private fun AccessActions(
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Spacer(modifier = Modifier.height(KiwiSpacing.xxxl))
    Button(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .height(KiwiDimensions.minimumTouchTarget),
        shape = RoundedCornerShape(KiwiDimensions.chipRadius),
    ) {
        Text(text = "Retry")
    }
    Spacer(modifier = Modifier.height(KiwiSpacing.sm))
    OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier
            .fillMaxWidth()
            .height(KiwiDimensions.minimumTouchTarget),
        shape = RoundedCornerShape(KiwiDimensions.chipRadius),
    ) {
        Text(text = "Sign out")
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun AccessDeniedPreview() {
    KiwiTheme {
        ApprovedUserScreen(
            state = ApprovedUserUiState.Denied,
            onRetry = {},
            onSignOut = {},
        )
    }
}
