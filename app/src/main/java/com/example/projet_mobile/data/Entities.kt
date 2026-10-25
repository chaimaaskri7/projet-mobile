package com.example.projet_mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// =============================================================
// TABLES ROOM
// =============================================================

/** Un plat du catalogue (cache local de l'API → l'appli marche hors ligne). */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val area: String,
    val thumb: String,
    val instructions: String,
    val tags: String,
    val youtube: String,
    val ingredients: List<Ingredient>,
    val price: Double,
    val rating: Double,
    val prepMinutes: Int,
    val calories: Int,
    val updatedAt: Long
)

data class Ingredient(
    val name: String,
    val measure: String
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val thumb: String,
    val description: String,
    val position: Int
)

/** Table séparée : les favoris survivent à une actualisation du catalogue. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mealId: String,
    val addedAt: Long
)

@Entity(tableName = "cart")
data class CartItemEntity(
    @PrimaryKey val mealId: String,
    val quantity: Int,
    val addedAt: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val items: List<OrderLine>,
    val subtotal: Double,
    val deliveryFee: Double,
    val total: Double,
    val address: String
)

/** Copie figée du plat au moment de la commande (le prix ne bouge plus). */
data class OrderLine(
    val mealId: String,
    val name: String,
    val thumb: String,
    val unitPrice: Double,
    val quantity: Int
)

/** Profil local (une seule ligne, id = 0). */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val address: String
)

/** Résultat d'une jointure panier ⨝ plats (pas une table). */
data class CartLine(
    val mealId: String,
    val name: String,
    val thumb: String,
    val category: String,
    val price: Double,
    val quantity: Int
)

object Pricing {
    const val FREE_DELIVERY_FROM = 30.0
    private const val DELIVERY_FEE = 2.99

    fun deliveryFee(subtotal: Double): Double =
        if (subtotal <= 0.0 || subtotal >= FREE_DELIVERY_FROM) 0.0 else DELIVERY_FEE
}
