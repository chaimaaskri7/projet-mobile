package com.example.projet_mobile

// =============================================================
// Modèles JSON renvoyés par TheMealDB (https://www.themealdb.com)
// =============================================================

/**
 * search.php renvoie chaque repas avec des champs strIngredient1..20 / strMeasure1..20.
 * On le lit comme une Map pour pouvoir parcourir ces 40 champs facilement.
 */
data class MealsResponse(
    val meals: List<Map<String, String?>>?
)

data class CategoriesResponse(
    val categories: List<CategoryDto>?
)

data class CategoryDto(
    val idCategory: String?,
    val strCategory: String?,
    val strCategoryThumb: String?,
    val strCategoryDescription: String?
)
