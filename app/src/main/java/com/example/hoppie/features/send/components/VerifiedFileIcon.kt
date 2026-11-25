package com.example.hoppie.features.send.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun VerifiedFileIcon() {
    Canvas(Modifier.size(24.dp).semantics { contentDescription = "Verified" }) {
        drawCircle(Color(0xFF50844E))
        val check = Path().apply {
            moveTo(size.width * 0.27f, size.height * 0.51f)
            lineTo(size.width * 0.43f, size.height * 0.66f)
            lineTo(size.width * 0.73f, size.height * 0.36f)
        }
        drawPath(
            check,
            Color.White,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
