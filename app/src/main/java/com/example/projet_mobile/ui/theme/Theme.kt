package com.example.projet_mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FoodgoColorScheme = lightColorScheme(
    primary = FoodRed,
    onPrimary = Color.White,
    primaryContainer = FoodRedSoft,
    onPrimaryContainer = FoodRedDark,
    secondary = FoodRedLight,
    onSecondary = Color.White,
    background = FoodBackground,
    onBackground = FoodText,
    surface = FoodSurface,
    onSurface = FoodText,
    surfaceVariant = FoodLightGray,
    onSurfaceVariant = FoodGray,
    outline = FoodGray
)

/** Thème clair fixe aux couleurs Foodgo (pas de couleurs dynamiques Android 12+). */
@Composable
fun ProjetmobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoodgoColorScheme,
        typography = Typography,
        content = content
    )
}
