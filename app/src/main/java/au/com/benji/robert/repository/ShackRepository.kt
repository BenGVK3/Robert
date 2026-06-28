package au.com.benji.robert.repository

import au.com.benji.robert.database.ShackDao
import au.com.benji.robert.database.ShackEntity
import kotlinx.coroutines.flow.Flow

class ShackRepository(
    private val dao: ShackDao
) {

    fun equipment(): Flow<List<ShackEntity>> {
        return dao.getAll()
    }

    suspend fun addEquipment(
        item: ShackEntity
    ): Long {
        return dao.insert(item)
    }

    suspend fun updateEquipment(
        item: ShackEntity
    ) {
        dao.update(item)
    }

    suspend fun deleteEquipment(
        item: ShackEntity
    ) {
        dao.delete(item)
    }

    suspend fun getEquipment(
        id: Long
    ): ShackEntity? {
        return dao.getById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}