package com.example.projet_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.projet_mobile.ui.theme.ProjetmobileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProjetmobileTheme {
                FoodgoSplashScreen()
            }
        }
    }
}

@Composable
fun FoodgoSplashScreen() {

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

        // Dimensions de l'écran
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // =========================================================
        // LOGO FOODGO
        // =========================================================

        Text(
            text = "Foodgo",
            color = Color.White,
            fontSize = 110.sp,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,

            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    y = -screenHeight * 0.10f
                )
        )

        // =========================================================
        // GRAND BURGER À GAUCHE
        // =========================================================

        Image(
            painter = painterResource(
                id = R.drawable.img_1
            ),
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
        )

        // =========================================================
        // PETIT BURGER À DROITE
        // =========================================================

        Image(
            painter = painterResource(
                id = R.drawable.img
            ),
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
        )
    }
}


// =============================================================
// PREVIEW ANDROID STUDIO
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