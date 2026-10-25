package com.example.projet_mobile.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

// =============================================================
// CONVERTISSEURS : listes ⇄ JSON (stockées en TEXT dans SQLite)
// =============================================================

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun ingredientsToJson(value: List<Ingredient>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToIngredients(value: String): List<Ingredient> =
        gson.fromJson<List<Ingredient>>(value, object : TypeToken<List<Ingredient>>() {}.type)
            ?: emptyList()

    @TypeConverter
    fun orderLinesToJson(value: List<OrderLine>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToOrderLines(value: String): List<OrderLine> =
        gson.fromJson<List<OrderLine>>(value, object : TypeToken<List<OrderLine>>() {}.type)
            ?: emptyList()
}

// =============================================================
// DAO
// =============================================================

@Dao
interface FoodDao {

    // ---------- Catalogue ----------

    @Query("SELECT COUNT(*) FROM meals")
    suspend fun mealCount(): Int

    @Query("SELECT COUNT(*) FROM meals")
    fun observeMealCount(): Flow<Int>

    @Upsert
    suspend fun upsertMeals(meals: List<MealEntity>)

    @Upsert
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories ORDER BY position")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM meals
        WHERE (:category = 'All' OR category = :category)
          AND (:query = ''
               OR name LIKE '%' || :query || '%'
               OR area LIKE '%' || :query || '%'
               OR category LIKE '%' || :query || '%')
        ORDER BY rating DESC, name ASC
        """
    )
    fun observeMeals(category: String, query: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    fun observeMeal(id: String): Flow<MealEntity?>

    // ---------- Favoris ----------

    @Query(
        """
        SELECT m.* FROM meals m
        INNER JOIN favorites f ON f.mealId = m.id
        ORDER BY f.addedAt DESC
        """
    )
    fun observeFavoriteMeals(): Flow<List<MealEntity>>

    @Query("SELECT mealId FROM favorites")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mealId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mealId = :id")
    suspend fun removeFavorite(id: String)

    // ---------- Panier ----------

    @Query(
        """
        SELECT c.mealId AS mealId, m.name AS name, m.thumb AS thumb,
               m.category AS category, m.price AS price, c.quantity AS quantity
        FROM cart c INNER JOIN meals m ON m.id = c.mealId
        ORDER BY c.addedAt ASC
        """
    )
    fun observeCart(): Flow<List<CartLine>>

    @Query(
        """
        SELECT c.mealId AS mealId, m.name AS name, m.thumb AS thumb,
               m.category AS category, m.price AS price, c.quantity AS quantity
        FROM cart c INNER JOIN meals m ON m.id = c.mealId
        ORDER BY c.addedAt ASC
        """
    )
    suspend fun getCart(): List<CartLine>

    @Query("SELECT quantity FROM cart WHERE mealId = :id")
    suspend fun cartQuantity(id: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Query("UPDATE cart SET quantity = :quantity WHERE mealId = :id")
    suspend fun setCartQuantity(id: String, quantity: Int)

    @Query("DELETE FROM cart WHERE mealId = :id")
    suspend fun removeFromCart(id: String)

    @Query("DELETE FROM cart")
    suspend fun clearCart()

    // ---------- Commandes ----------

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    // ---------- Profil ----------

    @Query("SELECT * FROM profile WHERE id = 0")
    fun observeProfile(): Flow<ProfileEntity?>

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)
}

// =============================================================
// BASE DE DONNÉES
// =============================================================

@Database(
    entities = [
        MealEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        CartItemEntity::class,
        OrderEntity::class,
        ProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FoodDatabase : RoomDatabase() {

    abstract fun dao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: FoodDatabase? = null

        fun get(context: Context): FoodDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FoodDatabase::class.java,
                    "foodgo.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
