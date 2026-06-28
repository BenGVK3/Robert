package au.com.benji.robert.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShackDao {

    @Query("SELECT * FROM shack ORDER BY category, manufacturer, model")
    fun getAll(): Flow<List<ShackEntity>>

    @Query("SELECT * FROM shack WHERE id = :id")
    suspend fun getById(id: Long): ShackEntity?

    @Insert
    suspend fun insert(item: ShackEntity): Long

    @Update
    suspend fun update(item: ShackEntity)

    @Delete
    suspend fun delete(item: ShackEntity)

    @Query("DELETE FROM shack")
    suspend fun deleteAll()
}