package com.abhinavsirohi.kiwi.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun KiwiDesignPreviewCatalog() {
    KiwiTheme {
        KiwiBackground {
            Column(
                modifier = Modifier.padding(KiwiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md)
            ) {
                KiwiCard(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Text(
                        text = "Kiwi design foundation",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                }
                KiwiButton(onClick = {}) {
                    androidx.compose.material3.Text(text = "Primary action")
                }
                KiwiChip(label = "Category", onClick = {})
            }
        }
    }
}
