@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.projet_mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.projet_mobile.data.MealEntity
import com.example.projet_mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// =============================================================
// UTILITAIRES
// =============================================================

fun formatPrice(value: Double): String = String.format(Locale.FRANCE, "%.2f €", value)

fun formatRating(value: Double): String = String.format(Locale.FRANCE, "%.1f", value)

/** Ombre douce utilisée partout (cartes, boutons ronds…). */
fun Modifier.softShadow(
    radius: Dp = 22.dp,
    elevation: Dp = 10.dp,
    color: Color = Color(0x22000000)
): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(radius),
    ambientColor = color,
    spotColor = color
)

// =============================================================
// IMAGE D'UN PLAT (version légère + secours)
// =============================================================

/**
 * TheMealDB propose une miniature légère en ajoutant "/preview" à l'URL.
 * Si elle n'existe pas, on retombe automatiquement sur l'image d'origine.
 */
@Composable
fun MealImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    small: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop
) {
    var useOriginal by remember(url) { mutableStateOf(!small || url.isBlank()) }

    AsyncImage(
        model = if (useOriginal) url else "$url/preview",
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onError = { if (!useOriginal) useOriginal = true }
    )
}

// =============================================================
// ANIMATIONS RÉUTILISABLES
// =============================================================

/** Fondu + glissement + léger zoom à l'apparition (décalé pour les premiers éléments). */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(if (index < 8) index * 60L else 0L)
        progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * 40.dp.toPx()
            val s = 0.95f + 0.05f * p
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}

/** Reflet qui traverse la zone (effet "skeleton loading"). */
@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    return this.background(
        Brush.linearGradient(
            colors = listOf(Color(0xFFEAEAEF), Color(0xFFF8F8FB), Color(0xFFEAEAEF)),
            start = Offset(x, 0f),
            end = Offset(x + 400f, 400f)
        )
    )
}

/** Petit effet "ressort" quand on appuie. */
@Composable
fun rememberPressScale(interaction: MutableInteractionSource, pressed: Float = 0.94f): State<Float> {
    val isPressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pressScale"
    )
}

// =============================================================
// TITRES
// =============================================================

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FoodText
        )
        if (subtitle != null) {
            Text(text = subtitle, fontSize = 14.sp, color = FoodGray)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = FoodText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (trailing != null) trailing()
    }
}

// =============================================================
// CARTE PLAT (grille)
// =============================================================

@Composable
fun MealCard(
    meal: MealEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction, 0.96f)

    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .softShadow(radius = 24.dp, elevation = 8.dp, color = Color(0x18000000)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = FoodSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FoodLightGray)
            ) {
                MealImage(
                    url = meal.thumb,
                    contentDescription = meal.name,
                    modifier = Modifier.fillMaxSize()
                )
                RatingBadge(
                    rating = meal.rating,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
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
                text = meal.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = FoodText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = "${meal.area} · ${meal.prepMinutes} min",
                fontSize = 12.sp,
                color = FoodGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = formatPrice(meal.price),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FoodRed
                )
                AddButton(onClick = onAddClick)
            }
        }
    }
}

@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = FoodYellow,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = formatRating(rating),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = FoodText
        )
    }
}

/** Bouton "+" rond qui rebondit quand on ajoute au panier. */
@Composable
fun AddButton(onClick: () -> Unit, size: Dp = 34.dp) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .shadow(6.dp, CircleShape, ambientColor = FoodRed, spotColor = FoodRed)
            .clip(CircleShape)
            .background(FoodGradient)
            .clickable {
                onClick()
                scope.launch {
                    scale.animateTo(1.25f, tween(90))
                    scale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Ajouter au panier",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Cœur qui "pop" avec un petit rebond et change de couleur en douceur. */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val scale = remember { Animatable(1f) }
    var firstRun by remember { mutableStateOf(true) }

    LaunchedEffect(isFavorite) {
        if (firstRun) {
            firstRun = false
            return@LaunchedEffect
        }
        scale.animateTo(1.4f, tween(100))
        scale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
    }

    val tint by animateColorAsState(
        targetValue = if (isFavorite) FoodRed else FoodGray,
        animationSpec = tween(250),
        label = "heartTint"
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
            tint = tint,
            modifier = Modifier
                .size(size * 0.55f)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}

// =============================================================
// SÉLECTEUR DE QUANTITÉ
// =============================================================

@Composable
fun QuantityStepper(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val buttonSize = if (compact) 28.dp else 40.dp

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(FoodLightGray)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onMinus),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Moins",
                tint = FoodText,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp)
            )
        }

        Text(
            text = quantity.toString(),
            fontSize = if (compact) 14.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = FoodText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = if (compact) 26.dp else 36.dp)
        )

        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(FoodGradient)
                .clickable(onClick = onPlus),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Plus",
                tint = Color.White,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp)
            )
        }
    }
}

// =============================================================
// BOUTONS
// =============================================================

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    brush: Brush = FoodGradient
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction, 0.97f)

    Row(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = FoodRed.copy(alpha = 0.5f),
                spotColor = FoodRed.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FoodText
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

// =============================================================
// ÉTATS : chargement / erreur / vide
// =============================================================

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(18.dp))
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
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer()
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(56.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmer()
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .shimmer()
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(FoodRedSoft),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(FoodGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = FoodText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = FoodGray,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            GradientButton(text = actionLabel, onClick = onAction)
        }
    }
}

// =============================================================
// BARRE DE NAVIGATION FLOTTANTE
// =============================================================

data class NavTab(val label: String, val icon: ImageVector)

val FoodgoTabs = listOf(
    NavTab("Accueil", Icons.Default.Home),
    NavTab("Favoris", Icons.Default.Favorite),
    NavTab("Panier", Icons.Default.ShoppingCart),
    NavTab("Commandes", Icons.Default.ReceiptLong),
    NavTab("Profil", Icons.Default.Person)
)

@Composable
fun FoodgoBottomBar(
    selected: Int,
    cartCount: Int,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = FoodRed.copy(alpha = 0.45f),
                    spotColor = FoodRed.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(FoodGradient)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodgoTabs.forEachIndexed { index, tab ->
                NavItem(
                    tab = tab,
                    selected = selected == index,
                    badge = if (index == 2) cartCount else 0,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

/** L'onglet actif devient une pastille blanche qui s'agrandit pour afficher son label. */
@Composable
private fun NavItem(
    tab: NavTab,
    selected: Boolean,
    badge: Int,
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
            .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (badge > 0) {
                    Badge(
                        containerColor = if (selected) FoodRed else Color.White,
                        contentColor = if (selected) Color.White else FoodRed
                    ) {
                        Text(
                            text = if (badge > 99) "99+" else badge.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            Icon(imageVector = tab.icon, contentDescription = tab.label, tint = foreground)
        }
        AnimatedVisibility(visible = selected) {
            Text(
                text = tab.label,
                color = foreground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
