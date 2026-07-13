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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.data.repository.RoomProfileRepository
import com.abhinavsirohi.kiwi.domain.usecase.profile.SavePreferredName
import com.abhinavsirohi.kiwi.ui.theme.KiwiDimensions
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray

@Composable
fun MinimalSetupRoute(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.applicationContext as KiwiApplication
    val minimalSetupViewModel: MinimalSetupViewModel = viewModel(
        factory = MinimalSetupViewModel.Factory(
            savePreferredName = SavePreferredName(
                RoomProfileRepository(
                    database = application.database,
                    clientResult = application.supabaseClient,
                    deviceId = application.deviceId,
                ),
            ),
        ),
    )
    val state by minimalSetupViewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is MinimalSetupUiState.Saved) onComplete()
    }

    MinimalSetupScreen(
        state = state,
        onNameChanged = minimalSetupViewModel::updatePreferredName,
        onSave = minimalSetupViewModel::save,
        modifier = modifier,
    )
}

@Composable
fun MinimalSetupScreen(
    state: MinimalSetupUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = state as? MinimalSetupUiState.Editing
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
                text = "What should Kiwi call you?",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KiwiSpacing.md))
            Text(
                text = "Just your preferred name — you can change it later.",
                style = MaterialTheme.typography.bodyLarge,
                color = KiwiWarmGray,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KiwiSpacing.xxl))
            OutlinedTextField(
                value = editing?.preferredName.orEmpty(),
                onValueChange = onNameChanged,
                enabled = editing?.isSaving != true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Preferred name") },
                supportingText = editing?.message?.let { message -> ({ Text(message) }) },
                isError = editing?.message != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
            )
            Spacer(modifier = Modifier.height(KiwiSpacing.lg))
            Button(
                onClick = onSave,
                enabled = editing != null && !editing.isSaving && editing.preferredName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KiwiDimensions.minimumTouchTarget),
                shape = RoundedCornerShape(KiwiDimensions.chipRadius),
            ) {
                if (editing?.isSaving == true || state is MinimalSetupUiState.Saved) {
                    CircularProgressIndicator(strokeWidth = KiwiSpacing.xxs)
                } else {
                    Text("Continue")
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun MinimalSetupPreview() {
    KiwiTheme {
        MinimalSetupScreen(
            state = MinimalSetupUiState.Editing(preferredName = "Abhi"),
            onNameChanged = {},
            onSave = {},
        )
    }
}
