package com.example.projet_mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.projet_mobile.data.CartLine
import com.example.projet_mobile.data.OrderEntity
import com.example.projet_mobile.data.Pricing
import com.example.projet_mobile.data.ProfileEntity
import com.example.projet_mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================
// FAVORIS
// =============================================================

@Composable
fun FavoritesScreen(
    vm: FoodViewModel,
    contentPadding: PaddingValues,
    onOpenMeal: (String) -> Unit,
    onBrowse: () -> Unit
) {
    val favorites by vm.favorites.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenTitle(
                title = "Mes favoris",
                subtitle = if (favorites.isEmpty()) "Garde tes plats préférés sous la main"
                else "${favorites.size} plat(s) enregistré(s) sur ton téléphone"
            )
        }

        if (favorites.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = Icons.Default.FavoriteBorder,
                    title = "Aucun favori pour l'instant",
                    subtitle = "Touche le cœur d'un plat pour le retrouver ici, même hors ligne.",
                    actionLabel = "Explorer le menu",
                    onAction = onBrowse
                )
            }
        } else {
            itemsIndexed(favorites, key = { _, meal -> meal.id }) { index, meal ->
                StaggeredEntrance(index = index) {
                    MealCard(
                        meal = meal,
                        isFavorite = true,
                        onClick = { onOpenMeal(meal.id) },
                        onFavoriteClick = { vm.toggleFavorite(meal) },
                        onAddClick = { vm.addToCart(meal) }
                    )
                }
            }
        }
    }
}

// =============================================================
// PANIER
// =============================================================

@Composable
fun CartScreen(
    vm: FoodViewModel,
    contentPadding: PaddingValues,
    onOpenMeal: (String) -> Unit,
    onBrowse: () -> Unit,
    onSeeOrders: () -> Unit,
    onEditAddress: () -> Unit
) {
    val cart by vm.cart.collectAsState()
    val profile by vm.profile.collectAsState()
    var placedOrderId by remember { mutableStateOf<Long?>(null) }

    val subtotal = cart.sumOf { it.price * it.quantity }
    val fee = Pricing.deliveryFee(subtotal)
    val total = subtotal + fee
    val itemCount = cart.sumOf { it.quantity }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        ScreenTitle(
            title = "Mon panier",
            subtitle = if (cart.isEmpty()) "Ton panier est vide" else "$itemCount article(s)",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        )

        if (cart.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ShoppingCart,
                title = "Ton panier est vide",
                subtitle = "Ajoute des plats avec le bouton + pour passer commande.",
                actionLabel = "Voir le menu",
                onAction = onBrowse,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cart, key = { it.mealId }) { line ->
                    CartRow(
                        line = line,
                        onClick = { onOpenMeal(line.mealId) },
                        onQuantityChange = { vm.setQuantity(line.mealId, it) },
                        onRemove = { vm.removeFromCart(line.mealId) }
                    )
                }

                item {
                    DeliveryCard(address = profile?.address.orEmpty(), onEdit = onEditAddress)
                }

                item {
                    SummaryCard(subtotal = subtotal, fee = fee, total = total)
                }
            }

            GradientButton(
                text = "Commander · ${formatPrice(total)}",
                icon = Icons.Default.DeliveryDining,
                onClick = { vm.placeOrder { id -> placedOrderId = id } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    placedOrderId?.let { id ->
        AlertDialog(
            onDismissRequest = { placedOrderId = null },
            containerColor = Color.White,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(FoodGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FoodGreen, modifier = Modifier.size(48.dp))
                }
            },
            title = {
                Text("Commande #$id confirmée !", fontWeight = FontWeight.ExtraBold, color = FoodText)
            },
            text = {
                Text(
                    "Ta commande est enregistrée dans l'historique (Room). Elle sera prête dans 30 minutes environ.",
                    color = FoodGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    placedOrderId = null
                    onSeeOrders()
                }) {
                    Text("Voir mes commandes", color = FoodRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { placedOrderId = null }) {
                    Text("Fermer", color = FoodGray)
                }
            }
        )
    }
}

@Composable
private fun CartRow(
    line: CartLine,
    onClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(radius = 22.dp, elevation = 6.dp, color = Color(0x12000000))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MealImage(
            url = line.thumb,
            contentDescription = line.name,
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(FoodLightGray)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = line.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FoodText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Retirer",
                    tint = FoodGray,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRemove)
                        .padding(4.dp)
                )
            }
            Text(line.category, fontSize = 12.sp, color = FoodGray)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPrice(line.price * line.quantity),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FoodRed
                )
                QuantityStepper(
                    quantity = line.quantity,
                    onMinus = { onQuantityChange(line.quantity - 1) },
                    onPlus = { onQuantityChange(line.quantity + 1) },
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun DeliveryCard(address: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onEdit)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(FoodRedSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = FoodRed)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Livrer à", fontSize = 12.sp, color = FoodGray)
            Text(
                text = address.ifBlank { "Ajoute ton adresse dans le profil" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (address.isBlank()) FoodRed else FoodText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = FoodGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SummaryCard(subtotal: Double, fee: Double, total: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryLine("Sous-total", formatPrice(subtotal))
        SummaryLine("Livraison", if (fee == 0.0) "Offerte" else formatPrice(fee), highlight = fee == 0.0)
        if (fee > 0.0) {
            val missing = Pricing.FREE_DELIVERY_FROM - subtotal
            Text(
                text = "Plus que ${formatPrice(missing)} pour la livraison offerte",
                fontSize = 12.sp,
                color = FoodRed
            )
            LinearProgressIndicator(
                progress = (subtotal / Pricing.FREE_DELIVERY_FROM).toFloat().coerceIn(0f, 1f),
                color = FoodRed,
                trackColor = FoodRedSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
        Divider(color = FoodLightGray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FoodText)
            Text(formatPrice(total), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = FoodRed)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = FoodGray)
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) FoodGreen else FoodText
        )
    }
}

// =============================================================
// COMMANDES
// =============================================================

@Composable
fun OrdersScreen(
    vm: FoodViewModel,
    contentPadding: PaddingValues,
    onBrowse: () -> Unit,
    onGoToCart: () -> Unit
) {
    val orders by vm.orders.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenTitle(
                title = "Mes commandes",
                subtitle = if (orders.isEmpty()) "Ton historique apparaîtra ici"
                else "${orders.size} commande(s) · ${formatPrice(orders.sumOf { it.total })} au total"
            )
        }

        if (orders.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.ReceiptLong,
                    title = "Aucune commande",
                    subtitle = "Passe ta première commande depuis le panier.",
                    actionLabel = "Commander",
                    onAction = onBrowse
                )
            }
        } else {
            items(orders, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    onReorder = {
                        vm.reorder(order)
                        onGoToCart()
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderEntity, onReorder: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy 'à' HH:mm", Locale.FRANCE) }
    val delivered = System.currentTimeMillis() - order.createdAt > 30 * 60 * 1000L
    val itemCount = order.items.sumOf { it.quantity }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(radius = 22.dp, elevation = 6.dp, color = Color(0x12000000))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Commande #${order.id}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FoodText)
                Text(dateFormat.format(Date(order.createdAt)), fontSize = 12.sp, color = FoodGray)
            }
            StatusChip(delivered)
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Vignettes qui se chevauchent
            Box(modifier = Modifier.weight(1f)) {
                order.items.take(4).forEachIndexed { index, line ->
                    MealImage(
                        url = line.thumb,
                        contentDescription = line.name,
                        modifier = Modifier
                            .padding(start = (index * 30).dp)
                            .size(44.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                            .background(FoodLightGray)
                    )
                }
                if (order.items.size > 4) {
                    Box(
                        modifier = Modifier
                            .padding(start = 120.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FoodRedSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+${order.items.size - 4}", color = FoodRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPrice(order.total), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = FoodRed)
                Text("$itemCount article(s)", fontSize = 12.sp, color = FoodGray)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Divider(color = FoodLightGray)
                Spacer(Modifier.height(10.dp))
                order.items.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${line.quantity} × ${line.name}",
                            fontSize = 13.sp,
                            color = FoodText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(formatPrice(line.unitPrice * line.quantity), fontSize = 13.sp, color = FoodGray)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Livraison", fontSize = 13.sp, color = FoodGray)
                    Text(
                        if (order.deliveryFee == 0.0) "Offerte" else formatPrice(order.deliveryFee),
                        fontSize = 13.sp,
                        color = FoodGray
                    )
                }
                if (order.address.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("📍 ${order.address}", fontSize = 12.sp, color = FoodGray)
                }
                Spacer(Modifier.height(14.dp))
                GradientButton(
                    text = "Commander à nouveau",
                    icon = Icons.Default.Replay,
                    onClick = onReorder,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = FoodGray
            )
        }
    }
}

@Composable
private fun StatusChip(delivered: Boolean) {
    val color = if (delivered) FoodGreen else Color(0xFFFF8A00)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (delivered) "Livrée" else "En préparation",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// =============================================================
// PROFIL
// =============================================================

@Composable
fun ProfileScreen(
    vm: FoodViewModel,
    contentPadding: PaddingValues,
    onSeeOrders: () -> Unit,
    onSeeFavorites: () -> Unit
) {
    val profile by vm.profile.collectAsState()
    val orders by vm.orders.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val mealCount by vm.mealCount.collectAsState()
    val sync by vm.sync.collectAsState()

    var name by remember(profile) { mutableStateOf(profile?.name.orEmpty()) }
    var email by remember(profile) { mutableStateOf(profile?.email.orEmpty()) }
    var phone by remember(profile) { mutableStateOf(profile?.phone.orEmpty()) }
    var address by remember(profile) { mutableStateOf(profile?.address.orEmpty()) }
    var confirmClear by remember { mutableStateOf(false) }

    val changed = name != profile?.name.orEmpty() ||
            email != profile?.email.orEmpty() ||
            phone != profile?.phone.orEmpty() ||
            address != profile?.address.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------- Carte d'en-tête ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(FoodGradient)
                .padding(20.dp)
        ) {
            Spacer(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-50).dp)
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = profile?.name?.trim()?.firstOrNull()?.uppercaseChar()
                        if (initial != null) {
                            Text(initial.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = FoodRed)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = FoodRed, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = profile?.name?.ifBlank { null } ?: "Invité",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = profile?.email?.ifBlank { null } ?: "Complète ton profil ci-dessous",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(orders.size.toString(), "Commandes", onSeeOrders)
                    StatItem(favorites.size.toString(), "Favoris", onSeeFavorites)
                    StatItem(formatPrice(orders.sumOf { it.total }), "Dépensé", null)
                }
            }
        }

        // ---------- Formulaire ----------
        SectionCard(title = "Mes informations") {
            ProfileField(name, { name = it }, "Nom complet", Icons.Default.Person)
            ProfileField(email, { email = it }, "E-mail", Icons.Default.Email, KeyboardType.Email)
            ProfileField(phone, { phone = it }, "Téléphone", Icons.Default.Phone, KeyboardType.Phone)
            ProfileField(address, { address = it }, "Adresse de livraison", Icons.Default.LocationOn)
            GradientButton(
                text = "Enregistrer",
                icon = Icons.Default.Check,
                enabled = changed,
                onClick = {
                    vm.saveProfile(
                        ProfileEntity(
                            name = name.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            address = address.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---------- Données locales ----------
        SectionCard(title = "Données sur le téléphone") {
            SettingRow(
                icon = Icons.Default.Storage,
                title = "Menu enregistré",
                subtitle = "$mealCount plats disponibles hors ligne"
            )
            SettingRow(
                icon = Icons.Default.Refresh,
                title = if (sync.loading) "Mise à jour en cours…" else "Actualiser le menu",
                subtitle = "Télécharge les nouveautés depuis TheMealDB",
                onClick = if (sync.loading) null else ({ vm.sync(force = true) }),
                trailing = {
                    if (sync.loading) {
                        CircularProgressIndicator(color = FoodRed, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                }
            )
            SettingRow(
                icon = Icons.Default.DeleteSweep,
                title = "Effacer l'historique",
                subtitle = "Supprime toutes les commandes",
                tint = FoodRed,
                onClick = if (orders.isEmpty()) null else ({ confirmClear = true })
            )
        }

        Text(
            text = "Foodgo · données stockées localement avec Room, sans serveur.",
            fontSize = 12.sp,
            color = FoodGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = { Text("Effacer l'historique ?", fontWeight = FontWeight.Bold) },
            text = { Text("Toutes tes commandes seront supprimées du téléphone.", color = FoodGray) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearOrders()
                    confirmClear = false
                }) { Text("Effacer", color = FoodRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Annuler", color = FoodGray) }
            }
        )
    }
}

@Composable
private fun StatItem(value: String, label: String, onClick: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(radius = 24.dp, elevation = 6.dp, color = Color(0x10000000))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FoodText)
        content()
    }
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = FoodRed) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FoodRed,
            unfocusedBorderColor = FoodLightGray,
            focusedLabelColor = FoodRed,
            cursorColor = FoodRed
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = FoodText,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FoodRedSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = FoodRed, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tint)
            Text(subtitle, fontSize = 12.sp, color = FoodGray)
        }
        if (trailing != null) trailing()
    }
}
