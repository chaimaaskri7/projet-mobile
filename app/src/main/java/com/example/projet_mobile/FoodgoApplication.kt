package com.example.projet_mobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/**
 * Configure Coil pour toute l'appli :
 * - plus de téléchargements en parallèle (toutes les images viennent du même serveur)
 * - fondu à l'apparition des images
 * - cache disque gardé même sans connexion (les images restent visibles hors ligne)
 */
class FoodgoApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .dispatcher(Dispatcher().apply {
                        maxRequests = 32
                        maxRequestsPerHost = 12
                    })
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this).maxSizePercent(0.25).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
}
