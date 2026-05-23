package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AashiqLogo(
    modifier: Modifier = Modifier,
    enableGlow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
    
    // Smooth golden breathing glow effect
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Draw the cinematic gold logo
        Canvas(modifier = Modifier.size(200.dp)) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            
            val goldGradients = listOf(
                Color(0xFFF9E7B9), // Light gold
                Color(0xFFD4AF37), // Antique gold
                Color(0xFFAA7C11), // Dark bronze-gold
                Color(0xFFD4AF37)
            )
            
            val goldBrush = Brush.linearGradient(
                colors = goldGradients,
                start = Offset(width * 0.2f, height * 0.2f),
                end = Offset(width * 0.8f, height * 0.8f)
            )

            // 1. Draw elegant outer gold ring with glow
            if (enableGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3BFAF1D4), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = centerX * 1.3f * glowScale
                    ),
                    radius = centerX * 1.0f
                )
            }

            drawCircle(
                brush = goldBrush,
                radius = centerX * 0.88f,
                style = Stroke(width = 4.dp.toPx())
            )
            
            drawCircle(
                color = Color(0x30D4AF37),
                radius = centerX * 0.84f,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Draw Matte Black inner shield
            drawCircle(
                color = Color(0xFF0C0C0E),
                radius = centerX * 0.82f
            )

            // 3. Draw stylized gold 'A'
            val aPath = Path().apply {
                // Outer peak
                moveTo(centerX, centerY * 0.45f)
                // Left foot
                lineTo(centerX * 0.52f, centerY * 1.48f)
                // Left inner corner
                lineTo(centerX * 0.65f, centerY * 1.48f)
                // Left inner peak slope
                lineTo(centerX, centerY * 0.77f)
                // Right inner peak slope
                lineTo(centerX * 1.35f, centerY * 1.48f)
                // Right foot outer corner
                lineTo(centerX * 1.48f, centerY * 1.48f)
                close()
            }

            drawPath(
                path = aPath,
                brush = goldBrush
            )

            // Draw clean premium silver-gold highlight edge
            val highlightPath = Path().apply {
                moveTo(centerX, centerY * 0.45f)
                lineTo(centerX * 1.48f, centerY * 1.48f)
            }
            drawPath(
                path = highlightPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFFE5C158), Color.Transparent),
                    start = Offset(centerX, centerY * 0.45f),
                    end = Offset(centerX * 1.48f, centerY * 1.48f)
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Draw Center Play Button Triangle
            val playPath = Path().apply {
                moveTo(centerX - 8.dp.toPx(), centerY + 4.dp.toPx())
                lineTo(centerX + 12.dp.toPx(), centerY + 14.dp.toPx())
                lineTo(centerX - 8.dp.toPx(), centerY + 24.dp.toPx())
                close()
            }
            drawPath(
                path = playPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF6DF), Color(0xFFD4AF37)),
                    start = Offset(centerX - 8.dp.toPx(), centerY + 4.dp.toPx()),
                    end = Offset(centerX + 12.dp.toPx(), centerY + 24.dp.toPx())
                )
            )

            // 5. Draw Gold Plus Symbol (+) in target position
            val plusSize = 12.dp.toPx()
            val plusX = centerX * 1.36f
            val plusY = centerY * 0.82f
            val thick = 4.dp.toPx()

            // Horizontal bar of plus
            drawRoundRect(
                brush = goldBrush,
                topLeft = Offset(plusX - plusSize / 2f, plusY - thick / 2f),
                size = androidx.compose.ui.geometry.Size(plusSize, thick),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
            // Vertical bar of plus
            drawRoundRect(
                brush = goldBrush,
                topLeft = Offset(plusX - thick / 2f, plusY - plusSize / 2f),
                size = androidx.compose.ui.geometry.Size(thick, plusSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    }
}
