package com.example.projet_mobile

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * API REST Supabase (PostgREST) : les plats sont dans la table "meals".
 * Les données sont en ligne : il suffit de cloner le projet pour les retrouver.
 */
interface MealApi {

    /** Tous les plats du catalogue. */
    @GET("meals")
    suspend fun meals(
        @Query("select") select: String = "*",
        @Query("order") order: String = "name.asc"
    ): List<SupabaseMealDto>

    companion object {
        private const val SUPABASE_URL = "https://kexnkzgnomnbdkymiyki.supabase.co"

        // Clé PUBLIQUE (publishable) : prévue pour être embarquée dans l'appli
        private const val SUPABASE_KEY = "sb_publishable_jTLUyRYG0a402DP7kJHOqw_z_1y-kTo"

        private const val BASE_URL = "$SUPABASE_URL/rest/v1/"

        val service: MealApi by lazy {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("apikey", SUPABASE_KEY)
                            .addHeader("Accept", "application/json")
                            .build()
                    )
                }
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
