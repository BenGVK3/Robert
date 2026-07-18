package au.com.benji.robert.repository.lookup

import au.com.benji.robert.models.CallsignLookupResult
import au.com.benji.robert.models.ServiceCredential

interface ICallsignLookupProvider {
    val name: String
    
    suspend fun lookup(callsign: String, credential: ServiceCredential): CallsignLookupResult?
    
    suspend fun testConnection(credential: ServiceCredential): Boolean
}
