package au.com.benji.robert

import android.app.Application
import au.com.benji.robert.database.DatabaseModule
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger

class RobertApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Force initialization of database to check for migrations early
        DatabaseModule.database(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // 5% of internal storage
                    .build()
            }
            .logger(DebugLogger())
            .respectCacheHeaders(false) // Prefer local cache
            .build()
    }
}
