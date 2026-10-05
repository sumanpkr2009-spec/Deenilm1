package com.deenilm.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenSurfaceVariant

@Composable
fun EightPointStarBackground(modifier: Modifier = Modifier, alpha: Float = 0.045f) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacing = 96f
        val half = 26f
        val gold = DeenGold.copy(alpha = alpha)
        val style = Stroke(width = 2f)
        var y = spacing / 2f
        while (y < size.height) {
            var x = spacing / 2f
            while (x < size.width) {
                // Upright square
                val upright = Path().apply {
                    moveTo(x - half, y - half)
                    lineTo(x + half, y - half)
                    lineTo(x + half, y + half)
                    lineTo(x - half, y + half)
                    close()
                }
                // Diamond (square rotated 45 degrees) -> eight-pointed star
                val r = half * 1.35f
                val diamond = Path().apply {
                    moveTo(x, y - r)
                    lineTo(x + r, y)
                    lineTo(x, y + r)
                    lineTo(x - r, y)
                    close()
                }
                drawPath(upright, color = gold, style = style)
                drawPath(diamond, color = gold, style = style)
                x += spacing
            }
            y += spacing
        }
    }
}

@Composable
fun IslamicArchHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cornerY = 84f
            val apexY = 24f
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, cornerY)
                quadraticBezierTo(w * 0.18f, cornerY - 6f, w / 2f, apexY)
                quadraticBezierTo(w * 0.82f, cornerY - 6f, w, cornerY)
                lineTo(w, h)
                close()
            }
            drawPath(path, color = DeenSurfaceVariant)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 88.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = DeenGold,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeenCream,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
