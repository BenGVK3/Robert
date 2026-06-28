package au.com.benji.robert.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class LocationService(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        try {
            Tasks.await(fusedLocationClient.lastLocation)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLocationName(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown Location"
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            "Unknown Location"
        }
    }
}
