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

    // ---------- Favoris ----------

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT mealId FROM favorites")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mealId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mealId = :id")
    suspend fun removeFavorite(id: String)

    // ---------- Panier ----------

    @Query("SELECT * FROM cart ORDER BY addedAt ASC")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart ORDER BY addedAt ASC")
    suspend fun getCartItems(): List<CartItemEntity>

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
        FavoriteEntity::class,
        CartItemEntity::class,
        OrderEntity::class,
        ProfileEntity::class
    ],
    version = 2, // v2 : les plats ne sont plus dans Room (Supabase)
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
