package com.example.projet_mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val FoodRed = Color(0xFFF3233D)
private val FoodRedLight = Color(0xFFFF6B7A)
private val FoodBackground = Color(0xFFF8F8FA)
private val FoodText = Color(0xFF303038)
private val FoodGray = Color(0xFF85858D)
private val FoodGradient = Brush.linearGradient(listOf(FoodRedLight, FoodRed))

private val categories = listOf(
    "All" to "🍽️",
    "Beef" to "🥩",
    "Chicken" to "🍗",
    "Lamb" to "🍖",
    "Miscellaneous" to "✨"
)

// =============================================================
// ÉCRAN D'ACCUEIL
// =============================================================

@Composable
fun FoodgoHomeScreen() {

    var searchText by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("burger") }
    var retryKey by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedNav by remember { mutableIntStateOf(0) }
    var meals by remember { mutableStateOf<List<Meal>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var favorites by remember { mutableStateOf(setOf<String>()) }

    // Appel API au lancement, à chaque recherche et à chaque "Réessayer"
    LaunchedEffect(submittedQuery, retryKey) {
        loading = true
        error = null

        try {
            val response = withContext(Dispatchers.IO) {
                MealApi.service.searchMeals(submittedQuery)
            }
            meals = response.meals.orEmpty()
        } catch (e: Exception) {
            error = "Impossible de charger les repas."
        } finally {
            loading = false
        }
    }

    val filteredMeals = meals.filter { meal ->
        val matchesCategory =
            selectedCategory == "All" ||
                    meal.strCategory.equals(selectedCategory, ignoreCase = true)
        val matchesFavorites = selectedNav != 1 || meal.idMeal in favorites
        matchesCategory && matchesFavorites
    }

    fun submitSearch() {
        if (searchText.isNotBlank()) {
            submittedQuery = searchText.trim()
            selectedCategory = "All"
            selectedNav = 0
        }
    }

    Scaffold(
        containerColor = FoodBackground,
        bottomBar = {
            FoodgoBottomBar(
                selected = selectedNav,
                onSelect = { selectedNav = it }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // HEADER
            item {
                StaggeredEntrance(index = 0, modifier = Modifier.fillMaxWidth()) {
                    HomeHeader()
                }
            }

            // RECHERCHE
            item {
                StaggeredEntrance(index = 1, modifier = Modifier.fillMaxWidth()) {
                    SearchRow(
                        value = searchText,
                        onValueChange = { searchText = it },
                        onSearch = ::submitSearch
                    )
                }
            }

            // BANNIÈRE (uniquement sur l'accueil)
            if (selectedNav == 0) {
                item {
                    StaggeredEntrance(index = 2, modifier = Modifier.fillMaxWidth()) {
                        PromoBanner()
                    }
                }
            }

            // FILTRES
            item {
                StaggeredEntrance(index = 3, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categories.forEach { (name, emoji) ->
                            CategoryChip(
                                label = "$emoji  $name",
                                selected = name == selectedCategory,
                                onClick = { selectedCategory = name }
                            )
                        }
                    }
                }
            }

            // TITRE DE LA LISTE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedContent(
                        targetState = selectedNav == 1,
                        label = "listTitle"
                    ) { showFavorites ->
                        Text(
                            text = if (showFavorites) "My favourites" else "Popular burgers",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = FoodText
                        )
                    }

                    if (!loading && error == null) {
                        Text(
                            text = "${filteredMeals.size} results",
                            fontSize = 12.sp,
                            color = FoodGray
                        )
                    }
                }
            }

            // CHARGEMENT : cartes "skeleton" avec effet shimmer
            if (loading) {
                items(3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SkeletonCard(Modifier.weight(1f))
                        SkeletonCard(Modifier.weight(1f))
                    }
                }
            }

            // ERREUR
            if (!loading && error != null) {
                item {
                    ErrorState(
                        message = error.orEmpty(),
                        onRetry = { retryKey++ }
                    )
                }
            }

            // CARTES EN GRILLE : 2 PAR LIGNE, apparition échelonnée
            if (!loading && error == null) {
                val rows = filteredMeals.chunked(2)

                if (rows.isEmpty()) {
                    item { EmptyState(isFavorites = selectedNav == 1) }
                }

                itemsIndexed(
                    items = rows,
                    key = { _, row -> row.first().idMeal }
                ) { rowIndex, rowMeals ->
                    StaggeredEntrance(
                        index = rowIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowMeals.forEach { meal ->
                                MealCard(
                                    meal = meal,
                                    isFavorite = meal.idMeal in favorites,
                                    onFavoriteClick = {
                                        favorites =
                                            if (meal.idMeal in favorites) favorites - meal.idMeal
                                            else favorites + meal.idMeal
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (rowMeals.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================
// ANIMATIONS RÉUTILISABLES
// =============================================================

/** Fondu + glissement + léger zoom à l'apparition, décalé selon l'index. */
@Composable
private fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay((index * 70L).coerceAtMost(420L))
        progress.animateTo(
            1f,
            spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * 48.dp.toPx()
            val s = 0.94f + 0.06f * p
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}

/** Reflet qui traverse la zone (effet "skeleton loading"). */
@Composable
private fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    return this.background(
        Brush.linearGradient(
            colors = listOf(Color(0xFFE9E9EE), Color(0xFFF7F7FA), Color(0xFFE9E9EE)),
            start = Offset(x, 0f),
            end = Offset(x + 400f, 400f)
        )
    )
}

// =============================================================
// COMPOSANTS
// =============================================================

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Foodgo",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FoodText
            )
            Text(
                text = "Order your favourite food!",
                fontSize = 14.sp,
                color = FoodGray
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .border(2.dp, FoodGradient, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profil",
                tint = FoodRed,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun SearchRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "searchBtn"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000)
                ),
            placeholder = {
                Text("Search your favourite food", fontSize = 14.sp, color = FoodGray)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = FoodRed)
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = value.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Effacer",
                            tint = FoodGray
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        IconButton(
            onClick = onSearch,
            interactionSource = interaction,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = FoodRed.copy(alpha = 0.3f),
                    spotColor = FoodRed.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(FoodGradient)
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Rechercher",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PromoBanner() {
    val transition = rememberInfiniteTransition(label = "banner")
    val bob by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bob"
    )
    val tilt by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "tilt"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = FoodRed.copy(alpha = 0.35f),
                spotColor = FoodRed.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(FoodGradient)
    ) {
        // Bulles décoratives
        Spacer(
            Modifier
                .size(130.dp)
                .offset(x = (-35).dp, y = (-45).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )
        Spacer(
            Modifier
                .align(Alignment.BottomEnd)
                .size(90.dp)
                .offset(x = 20.dp, y = 30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .fillMaxWidth(0.55f)
        ) {
            Text(
                text = "Free delivery",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "on your first order",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Text(
                    text = "Order now",
                    color = FoodRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 24.dp)
                .size(160.dp)
                .graphicsLayer {
                    translationY = bob.dp.toPx()
                    rotationZ = tilt
                }
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) FoodRed else Color.White,
        animationSpec = tween(250),
        label = "chipBg"
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else FoodGray,
        animationSpec = tween(250),
        label = "chipFg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        shadowElevation = if (selected) 6.dp else 1.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            color = content,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(
    meal: Meal,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "cardPress"
    )

    Card(
        onClick = { /* TODO : ouvrir le détail du repas */ },
        interactionSource = interaction,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            // Image + dégradé + badge catégorie + bouton favori
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = meal.strMealThumb,
                    contentDescription = meal.strMeal,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color(0x80000000)
                            )
                        )
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = meal.strCategory ?: "Meal",
                        color = FoodRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                FavoriteButton(
                    isFavorite = isFavorite,
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = meal.strMeal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = FoodText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = FoodRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = meal.strArea ?: "Cuisine",
                        fontSize = 12.sp,
                        color = FoodGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(FoodGradient)
                        .clickable { /* TODO : ajouter au panier */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Cœur qui "pop" avec un petit rebond et change de couleur en douceur. */
@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    var firstRun by remember { mutableStateOf(true) }

    LaunchedEffect(isFavorite) {
        if (firstRun) {
            firstRun = false
            return@LaunchedEffect
        }
        scale.animateTo(1.4f, tween(100))
        scale.animateTo(
            1f,
            spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium)
        )
    }

    val tint by animateColorAsState(
        targetValue = if (isFavorite) FoodRed else FoodGray,
        animationSpec = tween(250),
        label = "heartTint"
    )

    Box(
        modifier = modifier
            .size(34.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Ajouter aux favoris",
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmer()
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer()
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer()
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = FoodRed,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(message, color = FoodGray)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FoodRed)
        ) {
            Text("Réessayer")
        }
    }
}

@Composable
private fun EmptyState(isFavorites: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isFavorites) Icons.Default.FavoriteBorder else Icons.Default.Search,
            contentDescription = null,
            tint = FoodGray,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isFavorites) "Aucun favori pour l'instant" else "Aucun repas trouvé.",
            color = FoodGray
        )
    }
}

// =============================================================
// BARRE DE NAVIGATION FLOTTANTE
// =============================================================

@Composable
private fun FoodgoBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val items = listOf(
        Icons.Default.Home to "Home",
        Icons.Default.Favorite to "Favoris",
        Icons.Default.Receipt to "Commandes",
        Icons.Default.Person to "Profil"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = FoodRed.copy(alpha = 0.4f),
                    spotColor = FoodRed.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(FoodGradient)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (icon, label) ->
                NavItem(
                    icon = icon,
                    label = label,
                    selected = selected == index,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

/** L'onglet actif devient une pastille blanche qui s'agrandit pour afficher son label. */
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) Color.White else Color.Transparent,
        animationSpec = tween(250),
        label = "navBg"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) FoodRed else Color.White,
        animationSpec = tween(250),
        label = "navFg"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = foreground
        )
        AnimatedVisibility(visible = selected) {
            Text(
                text = label,
                color = foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}