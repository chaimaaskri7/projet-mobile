package com.example.projet_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projet_mobile.ui.theme.ProjetmobileTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Durée du splash (mets 3_000L si le 10 s n'est pas imposé)
private const val SPLASH_DURATION_MS = 10_000L

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProjetmobileTheme {
                FoodgoApp()
            }
        }
    }
}

@Composable
fun FoodgoApp() {

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        showSplash = false
    }

    // Fondu enchaîné entre le splash et l'accueil
    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 700),
        label = "splashToHome"
    ) { splash ->
        if (splash) FoodgoSplashScreen() else FoodgoHomeScreen()
    }
}

@Composable
fun FoodgoSplashScreen() {

    // ---------- Animations d'entrée (une seule fois) ----------
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val leftIn = remember { Animatable(0f) }
    val rightIn = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(700)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
            )
        }
        launch {
            delay(300)
            leftIn.animateTo(
                1f,
                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow)
            )
        }
        launch {
            delay(500)
            rightIn.animateTo(
                1f,
                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow)
            )
        }
        launch {
            delay(900)
            taglineAlpha.animateTo(1f, tween(600))
        }
    }

    // ---------- Animations en boucle (flottement, bulles) ----------
    val infinite = rememberInfiniteTransition(label = "splashLoop")

    val float by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "float"
    )

    val pulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF8290),
                        Color(0xFFFF5267),
                        Color(0xFFF20D2F)
                    )
                )
            )
    ) {

        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val widthPx = constraints.maxWidth.toFloat()

        // =====================================================
        // BULLES DÉCORATIVES (pulsation douce)
        // =====================================================

        Spacer(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = screenWidth * 0.15f, y = screenHeight * 0.04f)
                .size(screenWidth * 0.6f)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )

        Spacer(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = -screenWidth * 0.2f, y = -screenHeight * 0.18f)
                .size(screenWidth * 0.4f)
                .graphicsLayer {
                    scaleX = 2.05f - pulse
                    scaleY = 2.05f - pulse
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        // =====================================================
        // LOGO + SLOGAN
        // =====================================================

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -screenHeight * 0.10f)
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                }
        ) {
            Text(
                text = "Foodgo",
                color = Color.White,
                fontSize = 110.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x40000000),
                        offset = Offset(0f, 8f),
                        blurRadius = 24f
                    )
                )
            )

            Text(
                text = "Order your favourite food!",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value }
            )
        }

        // =====================================================
        // GRAND BURGER À GAUCHE (glisse + flotte)
        // =====================================================

        Image(
            painter = painterResource(id = R.drawable.img_1),
            contentDescription = "Grand hamburger",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(
                    x = -screenWidth * 0.199f,
                    y = screenHeight * 0.12f
                )
                .width(screenWidth * 0.68f)
                .height(screenHeight * 0.38f)
                .graphicsLayer {
                    val p = leftIn.value
                    translationX = -(1f - p) * widthPx * 0.8f
                    translationY = float * 14.dp.toPx() * p
                    rotationZ = (1f - p) * -25f + (float - 0.5f) * 4f
                }
        )

        // =====================================================
        // BURGER À DROITE (glisse + flotte en opposition)
        // =====================================================

        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = "Petit hamburger",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = screenWidth * 0.12f,
                    y = screenHeight * 0.055f
                )
                .width(screenWidth * 0.60f)
                .height(screenHeight * 0.30f)
                .graphicsLayer {
                    val p = rightIn.value
                    translationX = (1f - p) * widthPx * 0.8f
                    translationY = (1f - float) * 12.dp.toPx() * p
                    rotationZ = (1f - p) * 25f + (0.5f - float) * 4f
                }
        )
    }
}

// =============================================================
// PREVIEW DU SPLASH SCREEN
// =============================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun FoodgoSplashPreview() {
    ProjetmobileTheme {
        FoodgoSplashScreen()
    }
}