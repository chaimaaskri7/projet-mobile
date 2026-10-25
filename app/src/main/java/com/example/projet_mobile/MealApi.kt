package com.example.projet_mobile

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * API TheMealDB (gratuite, sans clé).
 * On récupère TOUT le catalogue en interrogeant chaque lettre de a à z
 * (~300 plats avec ingrédients, recette, vidéo) + les 14 catégories.
 */
interface MealApi {

    /** Tous les plats dont le nom commence par [letter]. */
    @GET("search.php")
    suspend fun searchByFirstLetter(
        @Query("f") letter: String
    ): MealsResponse

    /** Liste des catégories avec image et description. */
    @GET("categories.php")
    suspend fun categories(): CategoriesResponse

    companion object {
        private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

        val service: MealApi by lazy {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MealApi::class.java)
        }
    }
}
