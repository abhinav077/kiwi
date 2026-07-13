package com.abhinavsirohi.kiwi.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.design.KiwiButton
import com.abhinavsirohi.kiwi.core.design.KiwiCard
import com.abhinavsirohi.kiwi.ui.theme.KiwiForest
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = KiwiSpacing.xl, vertical = KiwiSpacing.jumbo),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KiwiCard(
                modifier = Modifier.padding(bottom = KiwiSpacing.xxl)
            ) {
                Text(
                    text = "✦",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayLarge,
                    color = KiwiForest
                )
            }
            Text(
                text = "A little space for your day.",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(KiwiSpacing.md))
            Text(
                text = "Plan gently, keep track of what matters, and make room for yourself.",
                style = MaterialTheme.typography.bodyLarge,
                color = KiwiWarmGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(KiwiSpacing.xxxl))
            KiwiButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Continue")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun WelcomeScreenPreview() {
    KiwiTheme {
        WelcomeScreen(onContinue = {})
    }
}
