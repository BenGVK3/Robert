package au.com.benji.robert.repository.lookup

import android.util.Log
import android.util.Xml
import au.com.benji.robert.models.CallsignLookupResult
import au.com.benji.robert.models.ServiceCredential
import au.com.benji.robert.network.ApiService
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class QrzProvider : ICallsignLookupProvider {
    override val name: String = "QRZ"
    private var sessionKey: String? = null

    override suspend fun lookup(callsign: String, credential: ServiceCredential): CallsignLookupResult? {
        if (credential.username.isEmpty() || credential.passwordEncrypted.isEmpty()) return null
        
        // 1. Ensure we have a session key
        if (sessionKey == null) {
            sessionKey = login(credential.username, credential.passwordEncrypted) ?: return null
        }

        // 2. Perform Lookup
        try {
            val url = "https://xmldata.qrz.com/xml/current/?s=$sessionKey;callsign=$callsign"
            val xml = ApiService.fetchData(url) ?: return null
            return parseQrzXml(xml, callsign)
        } catch (e: Exception) {
            sessionKey = null // Clear session on error to force re-login next time
            return null
        }
    }

    override suspend fun testConnection(credential: ServiceCredential): Boolean {
        return login(credential.username, credential.passwordEncrypted) != null
    }

    private suspend fun login(username: String, pass: String): String? {
        try {
            val url = "https://xmldata.qrz.com/xml/current/?username=$username;password=$pass;agent=RobertV1"
            val xml = ApiService.fetchData(url) ?: return null
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "Key") {
                    return parser.nextText()
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("QRZ", "Login Error", e)
        }
        return null
    }

    private fun parseQrzXml(xml: String, call: String): CallsignLookupResult? {
        val data = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tag = parser.name
                    if (tag != "QRZDatabase" && tag != "Callsign" && tag != "Session") {
                        data[tag] = parser.nextText()
                    }
                }
                eventType = parser.next()
            }
            
            if (data.isEmpty()) return null

            return CallsignLookupResult(
                callsign = call,
                name = "${data["fname"]} ${data["name"]}".trim(),
                qth = data["addr2"] ?: data["city"] ?: "",
                gridsquare = data["grid"] ?: "",
                country = data["country"] ?: "",
                dxcc = data["country"] ?: "",
                cqZone = data["cqzone"]?.toIntOrNull() ?: 0,
                ituZone = data["ituzone"]?.toIntOrNull() ?: 0,
                source = "QRZ"
            )
        } catch (e: Exception) {
            return null
        }
    }
}
