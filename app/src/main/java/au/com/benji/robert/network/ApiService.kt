package au.com.benji.robert.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiService {
    private const val TAG = "ApiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchData(url: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching: $url")
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response: ${response.code} for $url")
                    return@withContext null
                }
                response.body?.string()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching $url: ${e.message}")
            null
        }
    }
}
