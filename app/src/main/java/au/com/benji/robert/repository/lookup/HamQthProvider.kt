package au.com.benji.robert.repository.lookup

import android.util.Log
import android.util.Xml
import au.com.benji.robert.models.CallsignLookupResult
import au.com.benji.robert.models.ServiceCredential
import au.com.benji.robert.network.ApiService
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class HamQthProvider : ICallsignLookupProvider {
    override val name: String = "HamQTH"
    private var sessionKey: String? = null

    override suspend fun lookup(callsign: String, credential: ServiceCredential): CallsignLookupResult? {
        if (credential.username.isEmpty() || credential.passwordEncrypted.isEmpty()) return null
        
        if (sessionKey == null) {
            sessionKey = login(credential.username, credential.passwordEncrypted) ?: return null
        }

        try {
            val url = "https://www.hamqth.com/xml.php?id=$sessionKey&callsign=$callsign&prg=Robert"
            val xml = ApiService.fetchData(url) ?: return null
            return parseHamQthXml(xml, callsign)
        } catch (e: Exception) {
            sessionKey = null
            return null
        }
    }

    override suspend fun testConnection(credential: ServiceCredential): Boolean {
        return login(credential.username, credential.passwordEncrypted) != null
    }

    private suspend fun login(username: String, pass: String): String? {
        try {
            val url = "https://www.hamqth.com/xml.php?u=$username&p=$pass"
            val xml = ApiService.fetchData(url) ?: return null
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "session_id") {
                    return parser.nextText()
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("HamQTH", "Login Error", e)
        }
        return null
    }

    private fun parseHamQthXml(xml: String, call: String): CallsignLookupResult? {
        val data = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    data[parser.name] = parser.nextText()
                }
                eventType = parser.next()
            }
            
            if (data.isEmpty()) return null

            return CallsignLookupResult(
                callsign = call,
                name = data["nick"] ?: data["name"] ?: "",
                qth = data["adr_city"] ?: "",
                gridsquare = data["grid"] ?: "",
                country = data["country"] ?: "",
                dxcc = data["country"] ?: "",
                cqZone = data["cq"]?.toIntOrNull() ?: 0,
                ituZone = data["itu"]?.toIntOrNull() ?: 0,
                source = "HamQTH"
            )
        } catch (e: Exception) {
            return null
        }
    }
}
