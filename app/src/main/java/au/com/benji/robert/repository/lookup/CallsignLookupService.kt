package au.com.benji.robert.repository.lookup

import android.util.Log
import au.com.benji.robert.database.LogbookDao
import au.com.benji.robert.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CallsignLookupService(
    private val logbookDao: LogbookDao,
    private val credentialsFlow: Flow<List<ServiceCredential>>
) {
    private val providers = mapOf(
        "QRZ" to QrzProvider(),
        "HamQTH" to HamQthProvider(),
        "Callook" to CallookProvider()
    )

    /**
     * Intelligent lookup: Cache -> History -> Online (by priority)
     */
    suspend fun lookup(callsign: String): Pair<CallsignLookupResult?, LookupStatus> {
        val upper = callsign.uppercase().trim()
        if (upper.length < 3) return null to LookupStatus.IDLE

        // 1. Check Logbook History (Instant)
        val history = logbookDao.getCallsignHistory(upper)
        if (history != null) {
            // Found in history, could use this to populate basic info
            // But we still want to try online for full details
        }

        // 2. Try Online Providers in Priority Order
        val credentials = credentialsFlow.first().filter { it.isEnabled }.sortedBy { it.priority }
        
        for (cred in credentials) {
            val provider = providers[cred.serviceName] ?: continue
            try {
                val result = provider.lookup(upper, cred)
                if (result != null) {
                    return result to LookupStatus.MATCHED
                }
            } catch (e: Exception) {
                Log.e("Lookup", "Provider ${cred.serviceName} failed", e)
            }
        }

        return null to LookupStatus.NO_MATCH
    }

    suspend fun getHistorySummary(call: String): CallsignHistorySummary? {
        return logbookDao.getCallsignHistory(call)?.let { 
            CallsignHistorySummary(
                totalQsos = it.total,
                lastWorked = it.lastWorked,
                lastBand = it.lastBand,
                lastMode = it.lastMode,
                lastRst = it.lastRst
            )
        }
    }
}
