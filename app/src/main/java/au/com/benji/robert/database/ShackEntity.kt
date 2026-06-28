package au.com.benji.robert.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shack")
data class ShackEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val category: String,

    val manufacturer: String,

    val model: String,

    val nickname: String = "",

    val serialNumber: String = "",

    val notes: String = "",

    val tags: List<String> = emptyList()
)