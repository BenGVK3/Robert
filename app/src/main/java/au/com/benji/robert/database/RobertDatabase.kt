package au.com.benji.robert.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ShackEntity::class,
        LogEntryEntity::class,
        RepeaterEntity::class,
        MoonDataEntity::class,
        WeatherEntity::class,
        PropagationHistoryEntity::class,
        SolarDataEntity::class,
        SatelliteEntity::class,
        DxSpotEntity::class,
        AprsPacketEntity::class,
        FavoriteFrequencyEntity::class,
        FavoriteSdrEntity::class,
        UserSettingsEntity::class,
        CallsignCacheEntity::class,
        NetEntity::class,
        RadioProfileEntity::class,
        AntennaProfileEntity::class,
        OperatorProfileEntity::class,
        QsoEntity::class
    ],
    version = 21,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RobertDatabase : RoomDatabase() {

    abstract fun shackDao(): ShackDao
    abstract fun logDao(): LogDao
    abstract fun logbookDao(): LogbookDao
    abstract fun repeaterDao(): RepeaterDao
    abstract fun moonDao(): MoonDao
    abstract fun weatherDao(): WeatherDao
    abstract fun propagationDao(): PropagationDao
    abstract fun cacheDao(): CacheDao
    abstract fun netDao(): NetDao
}
