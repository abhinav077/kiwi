package com.abhinavsirohi.kiwi.feature.settings

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.data.local.KiwiSettings
import com.abhinavsirohi.kiwi.data.local.KiwiSettingsStore
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import kotlinx.coroutines.launch

@Composable
fun AppLockGate(
    settings: KiwiSettings,
    settingsStore: KiwiSettingsStore,
    activity: FragmentActivity,
    content: @Composable () -> Unit,
) {
    var unlocked by remember(settings.appLockEnabled) { mutableStateOf(!settings.appLockEnabled) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, settings.appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && settings.appLockEnabled) unlocked = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!settings.appLockEnabled || unlocked) {
        content()
    } else {
        AppLockScreen(
            biometricEnabled = settings.biometricEnabled,
            activity = activity,
            settingsStore = settingsStore,
            onUnlocked = { unlocked = true },
        )
    }
}

@Composable
private fun AppLockScreen(
    biometricEnabled: Boolean,
    activity: FragmentActivity,
    settingsStore: KiwiSettingsStore,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val biometricPrompt = remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    message = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    message = "Biometric verification did not match."
                }
            },
        )
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Kiwi")
            .setSubtitle("Open your private space")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    KiwiBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = KiwiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Kiwi is locked", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(KiwiSpacing.sm))
            Text("Enter your PIN to return to your private space.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(KiwiSpacing.xl))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Spacer(Modifier.height(KiwiSpacing.md))
            Button(
                onClick = {
                    scope.launch {
                        if (settingsStore.verifyPin(pin.toCharArray())) onUnlocked()
                        else message = "That PIN did not match."
                        pin = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.length in 4..8,
            ) { Text("Unlock") }
            if (biometricEnabled) {
                Spacer(Modifier.height(KiwiSpacing.sm))
                Button(onClick = { biometricPrompt.authenticate(promptInfo) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use biometrics")
                }
            }
            message?.let {
                Spacer(Modifier.height(KiwiSpacing.md))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
