package au.com.benji.robert.repository

import au.com.benji.robert.database.LogDao
import au.com.benji.robert.database.LogEntryEntity
import kotlinx.coroutines.flow.Flow

class LogRepository(
    private val dao: LogDao
) {
    fun getAllLogs(): Flow<List<LogEntryEntity>> = dao.getAll()

    suspend fun addLog(entry: LogEntryEntity) = dao.insert(entry)

    suspend fun deleteLog(entry: LogEntryEntity) = dao.delete(entry)
}
