package com.example.projet_mobile

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.projet_mobile.data.Ingredient
import com.example.projet_mobile.data.MealEntity
import com.example.projet_mobile.ui.theme.*
import kotlinx.coroutines.delay

// =============================================================
// DÉTAIL D'UN PLAT
// =============================================================

@Composable
fun MealDetailScreen(
    vm: FoodViewModel,
    mealId: String,
    onBack: () -> Unit
) {
    val meal by remember(mealId) { vm.meal(mealId) }.collectAsState(initial = null)
    val favoriteIds by vm.favoriteIds.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FoodBackground)
    ) {
        val current = meal
        if (current == null) {
            CircularProgressIndicator(color = FoodRed, modifier = Modifier.align(Alignment.Center))
        } else {
            MealDetailContent(
                meal = current,
                isFavorite = current.id in favoriteIds,
                onBack = onBack,
                onToggleFavorite = { vm.toggleFavorite(current) },
                onAddToCart = { quantity -> vm.addToCart(current, quantity, notify = false) }
            )
        }
    }
}

@Composable
private fun MealDetailContent(
    meal: MealEntity,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToCart: (Int) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var quantity by rememberSaveable(meal.id) { mutableIntStateOf(1) }
    var justAdded by remember { mutableStateOf(false) }

    LaunchedEffect(justAdded) {
        if (justAdded) {
            delay(1600)
            justAdded = false
        }
    }

    val steps = remember(meal.instructions) { splitSteps(meal.instructions) }
    val tags = remember(meal.tags) {
        meal.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp)
        ) {

            // ---------- Grande photo ----------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(FoodLightGray)
                ) {
                    AsyncImage(
                        model = meal.thumb,
                        contentDescription = meal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color(0x55000000),
                                    0.3f to Color.Transparent,
                                    0.75f to Color.Transparent,
                                    1f to Color(0x66000000)
                                )
                            )
                    )
                }
            }

            // ---------- Contenu ----------
            item {
                Column(
                    modifier = Modifier
                        .offset(y = (-30).dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(FoodBackground)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagChip(meal.category, filled = true)
                        TagChip(meal.area, filled = false)
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = meal.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FoodText,
                        lineHeight = 32.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .softShadow(radius = 20.dp, elevation = 6.dp, color = Color(0x12000000))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InfoItem(Icons.Default.Star, FoodYellow, formatRating(meal.rating), "Note")
                        InfoItem(Icons.Default.Timer, FoodRed, "${meal.prepMinutes} min", "Préparation")
                        InfoItem(Icons.Default.LocalFireDepartment, Color(0xFFFF7A00), "${meal.calories}", "kcal")
                    }

                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tags.take(4).forEach { tag ->
                                Text(
                                    text = "#$tag",
                                    fontSize = 12.sp,
                                    color = FoodGray,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    SectionHeader(title = "Ingrédients") {
                        Text("${meal.ingredients.size} éléments", fontSize = 13.sp, color = FoodGray)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ---------- Ingrédients (images TheMealDB) ----------
            item {
                LazyRow(
                    modifier = Modifier.offset(y = (-30).dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(meal.ingredients) { ingredient -> IngredientCard(ingredient) }
                }
            }

            // ---------- Préparation ----------
            item {
                Column(
                    modifier = Modifier
                        .offset(y = (-30).dp)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(24.dp))
                    SectionHeader(title = "Préparation") {
                        Text("${steps.size} étapes", fontSize = 13.sp, color = FoodGray)
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            itemsIndexed(steps) { index, step ->
                StepRow(
                    number = index + 1,
                    text = step,
                    isLast = index == steps.lastIndex,
                    modifier = Modifier
                        .offset(y = (-30).dp)
                        .padding(horizontal = 20.dp)
                )
            }

            if (meal.youtube.isNotBlank()) {
                item {
                    OutlinedButton(
                        onClick = { runCatching { uriHandler.openUri(meal.youtube) } },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, FoodRed),
                        modifier = Modifier
                            .offset(y = (-14).dp)
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = FoodRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Voir la recette en vidéo", color = FoodRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---------- Boutons en haut ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleIconButton(Icons.Default.ArrowBack, "Retour", onBack)
            FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite, size = 44.dp)
        }

        // ---------- Barre d'achat en bas ----------
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Prix", fontSize = 12.sp, color = FoodGray)
                Text(
                    text = formatPrice(meal.price),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FoodRed
                )
            }
            Spacer(Modifier.width(12.dp))
            QuantityStepper(
                quantity = quantity,
                onMinus = { if (quantity > 1) quantity-- },
                onPlus = { if (quantity < 20) quantity++ },
                compact = true
            )
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                targetState = justAdded,
                label = "addButton",
                modifier = Modifier.weight(1f)
            ) { added ->
                GradientButton(
                    text = if (added) "Ajouté !" else formatPrice(meal.price * quantity),
                    icon = if (added) Icons.Default.Check else Icons.Default.ShoppingCart,
                    brush = if (added) Brush.linearGradient(listOf(FoodGreen, FoodGreen)) else FoodGradient,
                    onClick = {
                        if (!added) {
                            onAddToCart(quantity)
                            justAdded = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================
// COMPOSANTS DU DÉTAIL
// =============================================================

@Composable
private fun TagChip(text: String, filled: Boolean) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (filled) Color.White else FoodRed,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) FoodRed else FoodRedSoft)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun InfoItem(icon: ImageVector, tint: Color, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FoodText)
        Text(label, fontSize = 11.sp, color = FoodGray)
    }
}

@Composable
private fun IngredientCard(ingredient: Ingredient) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(86.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .softShadow(radius = 22.dp, elevation = 5.dp, color = Color(0x12000000))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://www.themealdb.com/images/ingredients/${Uri.encode(ingredient.name)}-Small.png",
                contentDescription = ingredient.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(54.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = ingredient.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = FoodText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (ingredient.measure.isNotBlank()) {
            Text(
                text = ingredient.measure,
                fontSize = 11.sp,
                color = FoodGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String, isLast: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(FoodGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (!isLast) {
                Box(
                    Modifier
                        .padding(vertical = 4.dp)
                        .width(2.dp)
                        .height(28.dp)
                        .background(FoodRed.copy(alpha = 0.2f))
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = FoodText,
            lineHeight = 21.sp,
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, bottom = 16.dp)
        )
    }
}

/** Découpe les instructions en étapes lisibles. */
private fun splitSteps(instructions: String): List<String> {
    val stepLabel = Regex("^(step|étape)\\s*\\d+[:.)]?$", RegexOption.IGNORE_CASE)
    val leadingNumber = Regex("^\\d+[.):-]\\s*")

    var steps = instructions
        .split(Regex("\\r?\\n"))
        .map { it.trim().replace(leadingNumber, "") }
        .filter { it.length > 2 && !stepLabel.matches(it) }

    if (steps.size <= 1) {
        steps = instructions
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 2 }
            .chunked(2)
            .map { it.joinToString(" ") }
    }
    return steps
}
