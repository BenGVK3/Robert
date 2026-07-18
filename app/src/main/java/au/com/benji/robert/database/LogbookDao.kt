package au.com.benji.robert.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogbookDao {
    // QSOs
    @Query("SELECT * FROM qsos ORDER BY timestamp DESC")
    fun getAllQsos(): Flow<List<QsoEntity>>

    @Query("SELECT * FROM qsos WHERE callWorked LIKE :search OR operatorCallsign LIKE :search OR name LIKE :search OR country LIKE :search OR potaRef LIKE :search OR sotaRef LIKE :search OR wwffRef LIKE :search OR vkShireRef LIKE :search OR satelliteName LIKE :search OR contestId LIKE :search OR notes LIKE :search OR mode LIKE :search ORDER BY timestamp DESC")
    fun searchQsosExtended(search: String): Flow<List<QsoEntity>>

    @Query("SELECT * FROM qsos WHERE band = :band ORDER BY timestamp DESC")
    fun getQsosByBand(band: String): Flow<List<QsoEntity>>

    @Query("SELECT * FROM qsos WHERE mode = :mode ORDER BY timestamp DESC")
    fun getQsosByMode(mode: String): Flow<List<QsoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQso(qso: QsoEntity): Long

    @Update
    suspend fun updateQso(qso: QsoEntity)

    @Delete
    suspend fun deleteQso(qso: QsoEntity)

    @Query("SELECT COUNT(*) as total, MAX(timestamp) as lastWorked, band as lastBand, mode as lastMode, rstReceived as lastRst FROM qsos WHERE callWorked = :call GROUP BY callWorked")
    suspend fun getCallsignHistory(call: String): CallsignHistorySummaryEntity?

    // Radio Profiles
    @Query("SELECT * FROM radio_profiles")
    fun getAllRadios(): Flow<List<RadioProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadio(radio: RadioProfileEntity): Long

    @Update
    suspend fun updateRadio(radio: RadioProfileEntity)

    @Delete
    suspend fun deleteRadio(radio: RadioProfileEntity)

    // Antenna Profiles
    @Query("SELECT * FROM antenna_profiles")
    fun getAllAntennas(): Flow<List<AntennaProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAntenna(antenna: AntennaProfileEntity): Long

    @Update
    suspend fun updateAntenna(antenna: AntennaProfileEntity)

    @Delete
    suspend fun deleteAntenna(antenna: AntennaProfileEntity)

    // Operator Profiles
    @Query("SELECT * FROM operator_profiles")
    fun getAllOperators(): Flow<List<OperatorProfileEntity>>

    @Query("SELECT * FROM operator_profiles WHERE id = :id")
    suspend fun getOperatorById(id: Long): OperatorProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: OperatorProfileEntity): Long

    @Update
    suspend fun updateOperator(operator: OperatorProfileEntity)

    @Delete
    suspend fun deleteOperator(operator: OperatorProfileEntity)
}
