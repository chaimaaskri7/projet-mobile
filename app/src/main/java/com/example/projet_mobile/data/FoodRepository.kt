package com.example.projet_mobile.data

import androidx.room.withTransaction
import com.example.projet_mobile.MealApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import kotlin.math.abs
import kotlin.math.round

/**
 * Source unique de vérité : l'UI lit TOUJOURS Room.
 * Le réseau sert seulement à remplir / rafraîchir Room.
 */
class FoodRepository(
    private val db: FoodDatabase,
    private val api: MealApi = MealApi.service
) {
    private val dao = db.dao()

    // ---------- Lecture (Flows Room → l'UI se met à jour toute seule) ----------

    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val favoriteMeals: Flow<List<MealEntity>> = dao.observeFavoriteMeals()
    val favoriteIds: Flow<List<String>> = dao.observeFavoriteIds()
    val cart: Flow<List<CartLine>> = dao.observeCart()
    val orders: Flow<List<OrderEntity>> = dao.observeOrders()
    val profile: Flow<ProfileEntity?> = dao.observeProfile()
    val mealCount: Flow<Int> = dao.observeMealCount()

    fun meals(category: String, query: String): Flow<List<MealEntity>> =
        dao.observeMeals(category, query)

    fun meal(id: String): Flow<MealEntity?> = dao.observeMeal(id)

    // ---------- Synchronisation avec l'API ----------

    suspend fun hasCache(): Boolean = dao.mealCount() > 0

    /** Télécharge tout le catalogue (a → z) et l'enregistre dans Room. */
    suspend fun refreshCatalog(): Int = coroutineScope {
        val categoriesDeferred = async {
            runCatching { api.categories().categories.orEmpty() }.getOrDefault(emptyList())
        }

        // 6 requêtes en parallèle maximum pour ne pas surcharger l'API
        val limiter = Semaphore(6)
        val meals = ('a'..'z').map { letter ->
            async {
                limiter.withPermit {
                    runCatching { api.searchByFirstLetter(letter.toString()).meals.orEmpty() }
                        .getOrDefault(emptyList())
                }
            }
        }
            .awaitAll()
            .flatten()
            .mapNotNull { it.toMealEntity() }
            .distinctBy { it.id }

        if (meals.isEmpty()) throw IOException("Aucune donnée reçue de l'API")

        val categoryEntities = categoriesDeferred.await().mapIndexedNotNull { index, dto ->
            val name = dto.strCategory ?: return@mapIndexedNotNull null
            CategoryEntity(
                name = name,
                thumb = dto.strCategoryThumb.orEmpty(),
                description = dto.strCategoryDescription.orEmpty(),
                position = index
            )
        }.ifEmpty {
            // Plan B : on déduit les catégories des plats reçus
            meals.map { it.category }.distinct().sorted().mapIndexed { i, name ->
                CategoryEntity(name, "", "", i)
            }
        }

        db.withTransaction {
            dao.upsertCategories(categoryEntities)
            dao.upsertMeals(meals)
        }
        meals.size
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
        val lines = dao.getCart()
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
// MAPPING JSON → ENTITÉ
// =============================================================

private fun Map<String, String?>.toMealEntity(): MealEntity? {
    val id = this["idMeal"]?.takeIf { it.isNotBlank() } ?: return null
    val name = this["strMeal"]?.takeIf { it.isNotBlank() } ?: return null

    val ingredients = (1..20).mapNotNull { i ->
        val ingredient = this["strIngredient$i"]?.trim().orEmpty()
        if (ingredient.isEmpty()) null
        else Ingredient(ingredient, this["strMeasure$i"]?.trim().orEmpty())
    }

    // TheMealDB ne donne ni prix ni note : on les génère de façon stable à partir de l'id
    val seed = abs(id.toIntOrNull() ?: id.hashCode())

    return MealEntity(
        id = id,
        name = name.trim(),
        category = this["strCategory"]?.trim().orEmpty().ifEmpty { "Autre" },
        area = this["strArea"]?.trim().orEmpty().ifEmpty { "International" },
        thumb = this["strMealThumb"].orEmpty(),
        instructions = this["strInstructions"].orEmpty(),
        tags = this["strTags"].orEmpty(),
        youtube = this["strYoutube"].orEmpty(),
        ingredients = ingredients,
        price = (7.5 + (seed % 15) * 0.75).roundMoney(),
        rating = 4.0 + (seed % 10) / 10.0,
        prepMinutes = 15 + (seed % 9) * 5,
        calories = 320 + (seed % 12) * 45,
        updatedAt = System.currentTimeMillis()
    )
}

private fun Double.roundMoney(): Double = round(this * 100) / 100
