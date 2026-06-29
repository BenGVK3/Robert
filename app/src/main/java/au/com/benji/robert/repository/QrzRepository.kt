package au.com.benji.robert.repository

import android.util.Log
import au.com.benji.robert.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QrzRepository {
    private var sessionKey: String? = null

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        if (username.isEmpty() || password.isEmpty()) return@withContext false
        
        val url = "https://xmldata.qrz.com/xml/current/?username=$username;password=$password;agent=RobertApp"
        val response = ApiService.fetchData(url)
        
        sessionKey = response?.let { extractValue(it, "Key") }
        sessionKey != null
    }

    suspend fun lookup(callsign: String): QrzData? = withContext(Dispatchers.IO) {
        val key = sessionKey ?: return@withContext null
        val url = "https://xmldata.qrz.com/xml/current/?s=$key;callsign=$callsign"
        val response = ApiService.fetchData(url) ?: return@withContext null
        
        if (response.contains("Session Timeout") || response.contains("Invalid session key")) {
            sessionKey = null // Force re-login next time
            return@withContext null
        }

        QrzData(
            callsign = extractValue(response, "call") ?: callsign,
            name = "${extractValue(response, "fname") ?: ""} ${extractValue(response, "name") ?: ""}".trim(),
            qth = "${extractValue(response, "addr2") ?: ""}, ${extractValue(response, "state") ?: ""} ${extractValue(response, "country") ?: ""}".trim(),
            grid = extractValue(response, "grid") ?: "---",
            class_ = extractValue(response, "class") ?: "---",
            qslCount = extractValue(response, "qslmgr") ?: "None",
            image = extractValue(response, "image") ?: ""
        )
    }

    private fun extractValue(xml: String, tag: String): String? {
        val startTag = "<$tag>"
        val endTag = "</$tag>"
        val start = xml.indexOf(startTag)
        if (start == -1) return null
        val end = xml.indexOf(endTag, start)
        if (end == -1) return null
        return xml.substring(start + startTag.length, end)
    }
}

data class QrzData(
    val callsign: String,
    val name: String,
    val qth: String,
    val grid: String,
    val class_: String,
    val qslCount: String,
    val image: String
)
