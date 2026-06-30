package au.com.benji.robert.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ShackEntity::class,
        LogEntryEntity::class,
        RepeaterEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RobertDatabase : RoomDatabase() {

    abstract fun shackDao(): ShackDao
    abstract fun logDao(): LogDao
    abstract fun repeaterDao(): RepeaterDao
}