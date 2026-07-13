package com.abhinavsirohi.kiwi.core.design

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhinavsirohi.kiwi.ui.theme.KiwiDimensions
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing

@Composable
fun KiwiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = KiwiDimensions.minimumTouchTarget),
        shape = RoundedCornerShape(KiwiDimensions.chipRadius),
        contentPadding = PaddingValues(horizontal = KiwiSpacing.lg, vertical = KiwiSpacing.sm),
        content = content
    )
}

@Composable
fun KiwiCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        content = { androidx.compose.foundation.layout.Column(Modifier.padding(KiwiSpacing.md)) { content() } }
    )
}

@Composable
fun KiwiChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier.heightIn(min = KiwiDimensions.minimumTouchTarget),
        shape = RoundedCornerShape(KiwiDimensions.chipRadius)
    )
}
