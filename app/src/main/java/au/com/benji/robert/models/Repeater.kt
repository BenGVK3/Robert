package au.com.benji.robert.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Repeater(
    @SerialName("callsign") val callsign: String,
    @SerialName("name") val name: String? = null,
    @SerialName("frequency") val frequency: String,
    @SerialName("input_freq") val inputFreq: String? = null,
    @SerialName("offset") val offset: String,
    @SerialName("band") val band: String? = null,
    @SerialName("mode") val mode: String? = null,
    @SerialName("tone") val tone: String? = null, // CTCSS
    @SerialName("dcs") val dcs: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("town") val town: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("country") val country: String? = "Australia",
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("grid_square") val gridSquare: String? = null,
    @SerialName("elevation") val elevation: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("last_update") val lastUpdate: String? = null,
    
    // Calculated fields
    @SerialName("distance") val distance: Double = 0.0,
    @SerialName("bearing") val bearing: Double = 0.0,
    @SerialName("direction") val direction: String = "",
    
    // Application state
    val isFavorite: Boolean = false,
    val isRecent: Boolean = false,
    val lastViewed: Long = 0,
    val maidenhead: String = "",

    // Networking
    @SerialName("echolink_id") val echolinkId: String? = null,
    @SerialName("irlp_id") val irlpId: String? = null,
    @SerialName("allstar_node") val allstarNode: String? = null,
    @SerialName("wires_x") val wiresX: String? = null,
    @SerialName("dmr_id") val dmrId: String? = null,
    @SerialName("color_code") val colorCode: String? = null,
    @SerialName("time_slot") val timeSlot: String? = null,
    @SerialName("talkgroup") val talkgroup: String? = null
)

fun Repeater.toEntity(): au.com.benji.robert.database.RepeaterEntity {
    return au.com.benji.robert.database.RepeaterEntity(
        callsign = callsign,
        frequency = frequency,
        name = name,
        inputFreq = inputFreq,
        offset = offset,
        band = band,
        mode = mode,
        tone = tone,
        dcs = dcs,
        location = location,
        town = town,
        state = state,
        country = country,
        lat = lat,
        lng = lng,
        gridSquare = gridSquare,
        elevation = elevation,
        notes = notes,
        status = status,
        lastUpdate = lastUpdate,
        isFavorite = isFavorite,
        isRecent = isRecent,
        lastViewed = lastViewed,
        maidenhead = maidenhead,
        echolinkId = echolinkId,
        irlpId = irlpId,
        allstarNode = allstarNode,
        wiresX = wiresX,
        dmrId = dmrId,
        colorCode = colorCode,
        timeSlot = timeSlot,
        talkgroup = talkgroup
    )
}

fun au.com.benji.robert.database.RepeaterEntity.toModel(distance: Double = 0.0, bearing: Double = 0.0, direction: String = ""): Repeater {
    return Repeater(
        callsign = callsign,
        name = name,
        frequency = frequency,
        inputFreq = inputFreq,
        offset = offset,
        band = band,
        mode = mode,
        tone = tone,
        dcs = dcs,
        location = location,
        town = town,
        state = state,
        country = country,
        lat = lat,
        lng = lng,
        gridSquare = gridSquare,
        elevation = elevation,
        notes = notes,
        status = status,
        lastUpdate = lastUpdate,
        distance = distance,
        bearing = bearing,
        direction = direction,
        isFavorite = isFavorite,
        isRecent = isRecent,
        lastViewed = lastViewed,
        maidenhead = maidenhead,
        echolinkId = echolinkId,
        irlpId = irlpId,
        allstarNode = allstarNode,
        wiresX = wiresX,
        dmrId = dmrId,
        colorCode = colorCode,
        timeSlot = timeSlot,
        talkgroup = talkgroup
    )
}
