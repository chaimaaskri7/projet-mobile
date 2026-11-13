package com.example.projet_mobile

import com.example.projet_mobile.data.Ingredient
import com.google.gson.annotations.SerializedName

// =============================================================
// Modèle JSON renvoyé par Supabase (table "meals")
// =============================================================

data class SupabaseMealDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("area") val area: String?,
    @SerializedName("thumb") val thumb: String?,
    @SerializedName("instructions") val instructions: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("youtube") val youtube: String?,
    @SerializedName("ingredients") val ingredients: List<Ingredient>?,
    @SerializedName("price") val price: Double?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("prep_minutes") val prepMinutes: Int?,
    @SerializedName("calories") val calories: Int?
)
