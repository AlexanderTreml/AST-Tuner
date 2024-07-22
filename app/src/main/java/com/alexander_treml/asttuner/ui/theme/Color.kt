package com.alexander_treml.asttuner.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

val GRAY0 = Color(0xFF000000)
val GRAY1 = Color(0xFF242424)
val GRAY2 = Color(0xFFA8A8A8)

val SILVER0 = Color(0xFFA8A8A8)
val SILVER1 = Color(0xFFFFFEFE)

val Highlight = Color(0x61FFFFFF)

// Brushes
val silverBrush = Brush.linearGradient(
    listOf(SILVER0, SILVER1),
    end = Offset(80f, 70f),
    tileMode = TileMode.Mirror
)

val shinyBlackBrush = Brush.linearGradient(
    0.0f to GRAY2,
    0.2f to GRAY0,
    0.8f to GRAY0,
    1.0f to GRAY2,
    tileMode = TileMode.Mirror
)

val borderBrush = Brush.linearGradient(
    listOf(GRAY1, GRAY2, GRAY1),
    tileMode = TileMode.Mirror
)

val backgroundBrush = Brush.linearGradient(
    listOf(GRAY0, GRAY1),
    end = Offset(10f, 10f),
    tileMode = TileMode.Repeated
)

val outlineBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.65f))
)

val outlineBrushPressed = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.55f))
)