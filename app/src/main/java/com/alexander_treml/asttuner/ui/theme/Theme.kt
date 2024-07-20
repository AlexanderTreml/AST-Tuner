package com.alexander_treml.asttuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val colorScheme = darkColorScheme(
    primary = Brass,
    secondary = LightBrass,
    background = DarkGray,
    onBackground = LightGray
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}