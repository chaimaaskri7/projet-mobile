package com.example.projet_mobile.data

import androidx.room.withTransaction
import com.example.projet_mobile.MealApi
import com.example.projet_mobile.SupabaseMealDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.math.round

/**
 * - Les PLATS viennent de Supabase (API REST) et sont gardés en mémoire.
 * - Favoris, panier, commandes et profil restent stockés dans Room.
 */
class FoodRepository(
    private val db: FoodDatabase,
    private val api: MealApi = MealApi.service
) {
    private val dao = db.dao()

    /** Catalogue chargé depuis Supabase (vide tant que rien n'est chargé). */
    private val catalog = MutableStateFlow<List<MealEntity>>(emptyList())

    // ---------- Lecture ----------

    val categories: Flow<List<CategoryEntity>> = catalog.map { it.toCategories() }

    val favoriteMeals: Flow<List<MealEntity>> =
        combine(dao.observeFavorites(), catalog) { favorites, meals ->
            val byId = meals.associateBy { it.id }
            favorites.mapNotNull { byId[it.mealId] }
        }

    val favoriteIds: Flow<List<String>> = dao.observeFavoriteIds()

    val cart: Flow<List<CartLine>> =
        combine(dao.observeCartItems(), catalog) { items, meals -> items.toCartLines(meals) }

    val orders: Flow<List<OrderEntity>> = dao.observeOrders()
    val profile: Flow<ProfileEntity?> = dao.observeProfile()
    val mealCount: Flow<Int> = catalog.map { it.size }

    fun meals(category: String, query: String): Flow<List<MealEntity>> =
        catalog.map { meals ->
            val q = query.trim()
            meals.filter { meal ->
                (category == "All" || meal.category == category) &&
                    (q.isEmpty() ||
                        meal.name.contains(q, ignoreCase = true) ||
                        meal.area.contains(q, ignoreCase = true) ||
                        meal.category.contains(q, ignoreCase = true))
            }.sortedWith(compareByDescending<MealEntity> { it.rating }.thenBy { it.name })
        }

    fun meal(id: String): Flow<MealEntity?> = catalog.map { meals -> meals.firstOrNull { it.id == id } }

    // ---------- Chargement depuis Supabase ----------

    /** true si le catalogue est déjà chargé en mémoire. */
    suspend fun hasCache(): Boolean = catalog.value.isNotEmpty()

    /** Télécharge tous les plats de la table Supabase "meals". */
    suspend fun refreshCatalog(): Int {
        val meals = api.meals().mapNotNull { it.toMealEntity() }.distinctBy { it.id }
        if (meals.isEmpty()) throw IOException("Aucune donnée reçue de Supabase")
        catalog.value = meals
        return meals.size
    }

    // ---------- Favoris ----------

    suspend fun toggleFavorite(id: String): Boolean {
        return if (dao.isFavorite(id)) {
            dao.removeFavorite(id)
            false
        } else {
            dao.addFavorite(FavoriteEntity(id, System.currentTimeMillis()))
            true
        }
    }

    // ---------- Panier ----------

    suspend fun addToCart(id: String, quantity: Int) {
        val current = dao.cartQuantity(id)
        if (current == null) {
            dao.insertCartItem(
                CartItemEntity(id, quantity.coerceIn(1, 99), System.currentTimeMillis())
            )
        } else {
            dao.setCartQuantity(id, (current + quantity).coerceAtMost(99))
        }
    }

    suspend fun setQuantity(id: String, quantity: Int) {
        if (quantity <= 0) dao.removeFromCart(id)
        else dao.setCartQuantity(id, quantity.coerceAtMost(99))
    }

    suspend fun removeFromCart(id: String) = dao.removeFromCart(id)

    // ---------- Commandes ----------

    /** Transforme le panier en commande (transaction : tout ou rien). */
    suspend fun placeOrder(address: String): Long? = db.withTransaction {
        val lines = dao.getCartItems().toCartLines(catalog.value)
        if (lines.isEmpty()) {
            null
        } else {
            val subtotal = lines.sumOf { it.price * it.quantity }.roundMoney()
            val fee = Pricing.deliveryFee(subtotal)
            val id = dao.insertOrder(
                OrderEntity(
                    createdAt = System.currentTimeMillis(),
                    items = lines.map {
                        OrderLine(it.mealId, it.name, it.thumb, it.price, it.quantity)
                    },
                    subtotal = subtotal,
                    deliveryFee = fee,
                    total = (subtotal + fee).roundMoney(),
                    address = address
                )
            )
            dao.clearCart()
            id
        }
    }

    suspend fun reorder(order: OrderEntity) {
        db.withTransaction {
            order.items.forEach { addToCart(it.mealId, it.quantity) }
        }
    }

    suspend fun clearOrders() = dao.clearOrders()

    // ---------- Profil ----------

    suspend fun saveProfile(profile: ProfileEntity) = dao.upsertProfile(profile.copy(id = 0))
}

// =============================================================
// MAPPING SUPABASE → MODÈLE DE L'APPLI
// =============================================================

private fun SupabaseMealDto.toMealEntity(): MealEntity? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null

    return MealEntity(
        id = id,
        name = name.trim(),
        category = category?.trim().orEmpty().ifEmpty { "Autre" },
        area = area?.trim().orEmpty().ifEmpty { "International" },
        thumb = thumb.orEmpty(),
        instructions = instructions.orEmpty(),
        tags = tags.orEmpty(),
        youtube = youtube.orEmpty(),
        ingredients = ingredients.orEmpty().filter { it.name.isNotBlank() },
        price = (price ?: 9.99).roundMoney(),
        rating = rating ?: 4.5,
        prepMinutes = prepMinutes ?: 30,
        calories = calories ?: 500,
        updatedAt = System.currentTimeMillis()
    )
}

/** Panier (Room) + plats (Supabase) → lignes affichables. */
private fun List<CartItemEntity>.toCartLines(meals: List<MealEntity>): List<CartLine> {
    val byId = meals.associateBy { it.id }
    return mapNotNull { item ->
        val meal = byId[item.mealId] ?: return@mapNotNull null
        CartLine(
            mealId = item.mealId,
            name = meal.name,
            thumb = meal.thumb,
            category = meal.category,
            price = meal.price,
            quantity = item.quantity
        )
    }
}

/** Ordre habituel des catégories, puis les autres par ordre alphabétique. */
private val CATEGORY_ORDER = listOf(
    "Beef", "Chicken", "Dessert", "Lamb", "Miscellaneous", "Pasta", "Pork",
    "Seafood", "Side", "Starter", "Vegan", "Vegetarian", "Breakfast", "Goat"
)

private fun List<MealEntity>.toCategories(): List<CategoryEntity> =
    map { it.category }.distinct()
        .sortedWith(compareBy<String> {
            CATEGORY_ORDER.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i }
        }.thenBy { it })
        .mapIndexed { index, name ->
            CategoryEntity(
                name = name,
                thumb = "https://www.themealdb.com/images/category/$name.png",
                description = "",
                position = index
            )
        }

private fun Double.roundMoney(): Double = round(this * 100) / 100
