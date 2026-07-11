package au.com.benji.robert.repository

import au.com.benji.robert.database.NetDao
import au.com.benji.robert.database.NetEntity
import kotlinx.coroutines.flow.Flow

class NetRepository(private val netDao: NetDao) {
    fun getAllNets(): Flow<List<NetEntity>> = netDao.getAllNets()

    suspend fun addNet(net: NetEntity) = netDao.insertNet(net)

    suspend fun updateNet(net: NetEntity) = netDao.updateNet(net)

    suspend fun deleteNet(net: NetEntity) = netDao.deleteNet(net)
}
