package com.abhinavsirohi.kiwi.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.abhinavsirohi.kiwi.ui.theme.KiwiBlush
import com.abhinavsirohi.kiwi.ui.theme.KiwiCream
import com.abhinavsirohi.kiwi.ui.theme.KiwiLavender
import com.abhinavsirohi.kiwi.ui.theme.KiwiPaper
import com.abhinavsirohi.kiwi.ui.theme.KiwiPowderBlue
import com.abhinavsirohi.kiwi.ui.theme.KiwiSage

@Composable
fun KiwiBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(KiwiPaper)
            drawOrganicBlob(
                color = KiwiBlush.copy(alpha = 0.32f),
                path = topLeftBlob(size.width, size.height)
            )
            drawOrganicBlob(
                color = KiwiSage.copy(alpha = 0.28f),
                path = topRightBlob(size.width, size.height)
            )
            drawOrganicBlob(
                color = KiwiLavender.copy(alpha = 0.28f),
                path = bottomRightBlob(size.width, size.height)
            )
            drawOrganicBlob(
                color = KiwiPowderBlue.copy(alpha = 0.26f),
                path = bottomLeftBlob(size.width, size.height)
            )
            drawOval(
                color = KiwiCream.copy(alpha = 0.72f),
                topLeft = Offset(size.width * 0.16f, size.height * 0.20f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.68f, size.height * 0.58f)
            )
        }
        content()
    }
}

private fun DrawScope.drawOrganicBlob(color: Color, path: Path) {
    drawPath(path = path, color = color, style = Fill)
}

private fun topLeftBlob(width: Float, height: Float) = Path().apply {
    moveTo(0f, 0f)
    lineTo(width * 0.40f, 0f)
    cubicTo(width * 0.31f, height * 0.10f, width * 0.30f, height * 0.23f, width * 0.13f, height * 0.29f)
    cubicTo(width * 0.03f, height * 0.25f, width * 0.02f, height * 0.11f, 0f, height * 0.08f)
    close()
}

private fun topRightBlob(width: Float, height: Float) = Path().apply {
    moveTo(width, 0f)
    lineTo(width * 0.62f, 0f)
    cubicTo(width * 0.72f, height * 0.12f, width * 0.74f, height * 0.22f, width * 0.88f, height * 0.28f)
    cubicTo(width * 0.97f, height * 0.25f, width * 0.98f, height * 0.12f, width, height * 0.08f)
    close()
}

private fun bottomRightBlob(width: Float, height: Float) = Path().apply {
    moveTo(width, height)
    lineTo(width * 0.62f, height)
    cubicTo(width * 0.73f, height * 0.88f, width * 0.72f, height * 0.75f, width * 0.89f, height * 0.68f)
    cubicTo(width * 0.98f, height * 0.73f, width * 0.98f, height * 0.88f, width, height * 0.92f)
    close()
}

private fun bottomLeftBlob(width: Float, height: Float) = Path().apply {
    moveTo(0f, height)
    lineTo(width * 0.39f, height)
    cubicTo(width * 0.30f, height * 0.88f, width * 0.28f, height * 0.77f, width * 0.12f, height * 0.70f)
    cubicTo(width * 0.03f, height * 0.74f, width * 0.02f, height * 0.89f, 0f, height * 0.93f)
    close()
}
