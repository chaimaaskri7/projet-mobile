@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.projet_mobile

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.projet_mobile.data.CategoryEntity
import com.example.projet_mobile.data.ProfileEntity
import com.example.projet_mobile.ui.theme.*
import kotlinx.coroutines.delay

// =============================================================
// ÉCRAN D'ACCUEIL
// =============================================================

@Composable
fun FoodgoHomeScreen(
    vm: FoodViewModel,
    contentPadding: PaddingValues,
    onOpenMeal: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val meals by vm.meals.collectAsState()
    val categories by vm.categories.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val query by vm.query.collectAsState()
    val sortMode by vm.sortMode.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val sync by vm.sync.collectAsState()
    val profile by vm.profile.collectAsState()

    val list = meals
    val showSkeleton = list == null || (sync.loading && list.isEmpty())

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        fullWidth("header") {
            StaggeredEntrance(index = 0) {
                HomeHeader(profile = profile, onAvatarClick = onOpenProfile)
            }
        }

        fullWidth("search") {
            StaggeredEntrance(index = 1) {
                SearchRow(
                    value = query,
                    onValueChange = vm::onQueryChange,
                    sortMode = sortMode,
                    onSortChange = vm::setSort
                )
            }
        }

        if (sync.offline) {
            fullWidth("offline") { OfflinePill(onRetry = { vm.sync(force = true) }) }
        }

        if (query.isBlank()) {
            fullWidth("promo") {
                StaggeredEntrance(index = 2) {
                    PromoCarousel(
                        categories = categories,
                        onPromoClick = { category -> vm.selectCategory(category) }
                    )
                }
            }
        }

        fullWidth("categories") {
            StaggeredEntrance(index = 3) {
                CategoriesRow(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = vm::selectCategory
                )
            }
        }

        fullWidth("title") {
            val title = when {
                query.isNotBlank() -> "Résultats pour « ${query.trim()} »"
                selectedCategory == "All" -> "Populaires"
                else -> selectedCategory
            }
            SectionHeader(title = title) {
                if (!showSkeleton) {
                    Text(
                        text = "${list?.size ?: 0} plats",
                        fontSize = 13.sp,
                        color = FoodGray
                    )
                }
            }
        }

        when {
            showSkeleton -> {
                items(6) { SkeletonCard() }
            }

            sync.error != null && list.isNullOrEmpty() -> {
                fullWidth("error") {
                    EmptyState(
                        icon = Icons.Default.CloudOff,
                        title = "Oups, pas de connexion",
                        subtitle = sync.error.orEmpty(),
                        actionLabel = "Réessayer",
                        onAction = { vm.sync(force = true) }
                    )
                }
            }

            list.isNullOrEmpty() -> {
                fullWidth("empty") {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "Aucun plat trouvé",
                        subtitle = "Essaie un autre mot-clé ou une autre catégorie.",
                        actionLabel = "Tout afficher",
                        onAction = {
                            vm.onQueryChange("")
                            vm.selectCategory("All")
                        }
                    )
                }
            }

            else -> {
                itemsIndexed(list.orEmpty(), key = { _, meal -> meal.id }) { index, meal ->
                    StaggeredEntrance(index = index) {
                        MealCard(
                            meal = meal,
                            isFavorite = meal.id in favoriteIds,
                            onClick = { onOpenMeal(meal.id) },
                            onFavoriteClick = { vm.toggleFavorite(meal) },
                            onAddClick = { vm.addToCart(meal) }
                        )
                    }
                }
            }
        }
    }
}

/** Élément qui prend toute la largeur de la grille. */
private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

// =============================================================
// EN-TÊTE
// =============================================================

@Composable
private fun HomeHeader(profile: ProfileEntity?, onAvatarClick: () -> Unit) {
    val firstName = profile?.name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Foodgo",
                fontSize = 40.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = FoodRed
            )
            Text(
                text = if (firstName != null) "Bonjour $firstName, qu'est-ce qui te fait envie ?"
                else "Qu'est-ce qui te fait envie aujourd'hui ?",
                fontSize = 14.sp,
                color = FoodGray,
                maxLines = 2
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(52.dp)
                .border(2.dp, FoodGradient, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(FoodRedSoft)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            if (firstName != null) {
                Text(
                    text = firstName.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FoodRed
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = "Profil", tint = FoodRed)
            }
        }
    }
}

// =============================================================
// RECHERCHE + TRI
// =============================================================

@Composable
private fun SearchRow(
    value: String,
    onValueChange: (String) -> Unit,
    sortMode: SortMode,
    onSortChange: (SortMode) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var sortMenuOpen by remember { mutableStateOf(false) }

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
                .softShadow(radius = 18.dp, elevation = 8.dp, color = Color(0x16000000)),
            placeholder = {
                Text("Pizza, poulet, Italian…", fontSize = 14.sp, color = FoodGray)
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
                        Icon(Icons.Default.Close, contentDescription = "Effacer", tint = FoodGray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FoodRed.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = FoodRed
            )
        )

        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = FoodRed.copy(alpha = 0.4f),
                        spotColor = FoodRed.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(FoodGradient)
                    .clickable { sortMenuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Trier", tint = Color.White)
            }

            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { sortMenuOpen = false },
                modifier = Modifier.background(Color.White)
            ) {
                Text(
                    text = "Trier par",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FoodGray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                SortMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mode.label,
                                fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (mode == sortMode) FoodRed else FoodText
                            )
                        },
                        trailingIcon = {
                            if (mode == sortMode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FoodRed)
                            }
                        },
                        onClick = {
                            onSortChange(mode)
                            sortMenuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflinePill(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FoodDark)
            .clickable(onClick = onRetry)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Hors ligne · menu enregistré sur le téléphone",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text("Réessayer", color = FoodRedLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// =============================================================
// CARROUSEL PROMO
// =============================================================

private data class Promo(
    val title: String,
    val subtitle: String,
    val cta: String,
    val category: String,
    val brush: Brush,
    @DrawableRes val fallbackImage: Int?
)

@Composable
private fun PromoCarousel(
    categories: List<CategoryEntity>,
    onPromoClick: (String) -> Unit
) {
    val promos = remember {
        listOf(
            Promo("Livraison offerte", "dès 30 € de commande", "Commander", "Beef", FoodGradient, R.drawable.img),
            Promo("Envie de sucré ?", "Nos desserts les mieux notés", "Découvrir", "Dessert", FoodSunsetGradient, null),
            Promo("Fraîcheur marine", "Poissons & fruits de mer", "Explorer", "Seafood", FoodDarkGradient, null)
        )
    }
    val pagerState = rememberPagerState(pageCount = { promos.size })

    // Défilement automatique toutes les 4 secondes
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % promos.size)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val promo = promos[page]
            val image: Any? = promo.fallbackImage
                ?: categories.firstOrNull { it.name == promo.category }?.thumb
            PromoCard(promo = promo, image = image, onClick = { onPromoClick(promo.category) })
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(promos.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(if (selected) 22.dp else 7.dp, label = "dot")
                val color by animateColorAsState(if (selected) FoodRed else Color(0xFFD9D9DF), label = "dotColor")
                Box(
                    Modifier
                        .height(7.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun PromoCard(promo: Promo, image: Any?, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "promo")
    val bob by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val tilt by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilt"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(promo.brush)
            .clickable(onClick = onClick)
    ) {
        // Bulles décoratives
        Spacer(
            Modifier
                .size(140.dp)
                .offset(x = (-40).dp, y = (-50).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )
        Spacer(
            Modifier
                .align(Alignment.BottomEnd)
                .size(110.dp)
                .offset(x = 30.dp, y = 40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .fillMaxWidth(0.56f)
        ) {
            Text(
                text = promo.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = promo.subtitle,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = 2
            )
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
                Text(
                    text = promo.cta,
                    color = FoodRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        val imageModifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(x = 18.dp)
            .size(150.dp)
            .graphicsLayer {
                translationY = bob.dp.toPx()
                rotationZ = tilt
            }

        if (image is Int) {
            Image(
                painter = painterResource(id = image),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier
            )
        } else if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier.padding(12.dp)
            )
        }
    }
}

// =============================================================
// CATÉGORIES (images venant de l'API)
// =============================================================

@Composable
private fun CategoriesRow(
    categories: List<CategoryEntity>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        SectionHeader(title = "Catégories")
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            item(key = "All") {
                CategoryBubble(
                    label = "Tout",
                    image = null,
                    selected = selected == "All",
                    onClick = { onSelect("All") }
                )
            }
            items(categories, key = { it.name }) { category ->
                CategoryBubble(
                    label = category.name,
                    image = category.thumb,
                    selected = selected == category.name,
                    onClick = { onSelect(category.name) }
                )
            }
        }
    }
}

@Composable
private fun CategoryBubble(
    label: String,
    image: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) FoodRedSoft else Color.White,
        animationSpec = tween(250),
        label = "catBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) FoodRed else Color.Transparent,
        animationSpec = tween(250),
        label = "catBorder"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .softShadow(radius = 32.dp, elevation = 6.dp, color = Color(0x14000000))
                .clip(CircleShape)
                .background(background)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (image.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = FoodRed,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                AsyncImage(
                    model = image,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) FoodRed else FoodText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
