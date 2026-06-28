package au.com.benji.robert.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logbook ORDER BY timestamp DESC")
    fun getAll(): Flow<List<LogEntryEntity>>

    @Insert
    suspend fun insert(entry: LogEntryEntity): Long

    @Update
    suspend fun update(entry: LogEntryEntity)

    @Delete
    suspend fun delete(entry: LogEntryEntity)
}
