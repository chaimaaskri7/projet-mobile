package com.example.projet_mobile

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projet_mobile.ui.theme.FoodBackground
import com.example.projet_mobile.ui.theme.FoodDark
import com.example.projet_mobile.ui.theme.ProjetmobileTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Durée du splash (mets 3_000L si le 10 s n'est pas imposé)
private const val SPLASH_DURATION_MS = 10_000L

// Index des onglets de la barre du bas
private const val TAB_HOME = 0
private const val TAB_FAVORITES = 1
private const val TAB_CART = 2
private const val TAB_ORDERS = 3
private const val TAB_PROFILE = 4

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
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
    // Le ViewModel est créé tout de suite : le menu se télécharge pendant le splash
    val vm: FoodViewModel = viewModel()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (showSplash) {
            delay(SPLASH_DURATION_MS)
            showSplash = false
        }
    }

    // Fondu enchaîné entre le splash et l'application
    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 700),
        label = "splashToHome"
    ) { splash ->
        if (splash) FoodgoSplashScreen() else FoodgoMain(vm)
    }
}

// =============================================================
// APPLICATION PRINCIPALE : onglets + écran détail
// =============================================================

@Composable
fun FoodgoMain(vm: FoodViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastDetailId by remember { mutableStateOf<String?>(null) }

    val cartCount by vm.cartCount.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            snackbarHost.currentSnackbarData?.dismiss()
            launch { snackbarHost.showSnackbar(message) }
        }
    }

    // Bouton retour : ferme le détail, sinon revient à l'accueil
    BackHandler(enabled = detailId != null) { detailId = null }
    BackHandler(enabled = detailId == null && tab != TAB_HOME) { tab = TAB_HOME }

    val openMeal: (String) -> Unit = { id ->
        lastDetailId = id
        detailId = id
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = FoodBackground,
            bottomBar = {
                FoodgoBottomBar(
                    selected = tab,
                    cartCount = cartCount,
                    onSelect = { tab = it }
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHost) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = FoodDark,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        ) { innerPadding ->

            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 20 }) togetherWith
                            fadeOut(tween(150))
                },
                label = "tabs"
            ) { currentTab ->
                when (currentTab) {
                    TAB_HOME -> FoodgoHomeScreen(
                        vm = vm,
                        contentPadding = innerPadding,
                        onOpenMeal = openMeal,
                        onOpenProfile = { tab = TAB_PROFILE }
                    )

                    TAB_FAVORITES -> FavoritesScreen(
                        vm = vm,
                        contentPadding = innerPadding,
                        onOpenMeal = openMeal,
                        onBrowse = { tab = TAB_HOME }
                    )

                    TAB_CART -> CartScreen(
                        vm = vm,
                        contentPadding = innerPadding,
                        onOpenMeal = openMeal,
                        onBrowse = { tab = TAB_HOME },
                        onSeeOrders = { tab = TAB_ORDERS },
                        onEditAddress = { tab = TAB_PROFILE }
                    )

                    TAB_ORDERS -> OrdersScreen(
                        vm = vm,
                        contentPadding = innerPadding,
                        onBrowse = { tab = TAB_HOME },
                        onGoToCart = { tab = TAB_CART }
                    )

                    else -> ProfileScreen(
                        vm = vm,
                        contentPadding = innerPadding,
                        onSeeOrders = { tab = TAB_ORDERS },
                        onSeeFavorites = { tab = TAB_FAVORITES }
                    )
                }
            }
        }

        // Écran détail qui glisse par-dessus tout
        AnimatedVisibility(
            visible = detailId != null,
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(250))
        ) {
            val id = detailId ?: lastDetailId
            if (id != null) {
                MealDetailScreen(vm = vm, mealId = id, onBack = { detailId = null })
            }
        }
    }
}

// =============================================================
// SPLASH SCREEN
// =============================================================

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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FoodgoSplashPreview() {
    ProjetmobileTheme {
        FoodgoSplashScreen()
    }
}
