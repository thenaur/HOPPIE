package com.example.hoppie.features.send.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TrackOutline = Color(0xFF151F52)
private val StripeLight = Color(0xFF83A9FF)
private val StripeDark = Color(0xFF4F75E8)

@Composable
internal fun StripedTransferProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    border: Dp = 2.dp
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = animateFloatAsState(target, tween(durationMillis = 180), label = "transfer progress")
    Canvas(modifier.progressSemantics(target)) {
        val inset = border.toPx().coerceAtMost(size.minDimension / 2f)
        drawRoundRect(TrackOutline, cornerRadius = CornerRadius(size.height / 2f))
        val inner = Size(size.width - 2 * inset, size.height - 2 * inset)
        if (inner.width <= 0f || inner.height <= 0f) return@Canvas
        val origin = Offset(inset, inset)
        drawRoundRect(Color.White, origin, inner, CornerRadius(inner.height / 2f))

        val width = inner.width * animated.value.coerceIn(0f, target)
        if (width <= 0f) return@Canvas
        val radius = CornerRadius(minOf(width, inner.height) / 2f)
        val fill = Path().apply {
            addRoundRect(RoundRect(inset, inset, inset + width, inset + inner.height, radius))
        }
        clipPath(fill) {
            drawRect(StripeLight, origin, Size(width, inner.height))
            val step = 11.dp.toPx()
            var x = inset - inner.height - step
            while (x < inset + width + step) {
                drawLine(
                    StripeDark,
                    start = Offset(x - step, inset + inner.height + step),
                    end = Offset(x + inner.height + step, inset - step),
                    strokeWidth = step * 0.707107f
                )
                x += 2 * step
            }
        }
    }
}
