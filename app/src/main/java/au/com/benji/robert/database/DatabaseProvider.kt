package au.com.benji.robert.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE logbook ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN qth TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN power TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN sotaRef TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN potaRef TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN wwffRef TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN hemaRef TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN siotaRef TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE logbook ADD COLUMN vkShireRef TEXT NOT NULL DEFAULT ''")
        }
    }
    
    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 14 to 15 usually involves adding the base cache tables if they weren't there
            // Based on previous logs, we'll ensure they exist or are prepared for 17
        }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Placeholder for intermediary jump
        }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS solar_cache (
                    id INTEGER NOT NULL PRIMARY KEY,
                    solarFlux INTEGER NOT NULL,
                    kIndex INTEGER NOT NULL,
                    aIndex INTEGER NOT NULL,
                    sunspots INTEGER NOT NULL,
                    muf TEXT NOT NULL,
                    xRay TEXT NOT NULL,
                    solarWind TEXT NOT NULL,
                    protonFlux TEXT NOT NULL,
                    electronFlux TEXT NOT NULL,
                    aurora TEXT NOT NULL,
                    magneticField TEXT NOT NULL,
                    foF2 TEXT NOT NULL,
                    hfConditionsDay TEXT NOT NULL,
                    hfConditionsNight TEXT NOT NULL,
                    vhfAurora TEXT NOT NULL,
                    eSkip TEXT NOT NULL,
                    lastUpdated INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS satellite_cache (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    lastPositionLat REAL NOT NULL,
                    lastPositionLon REAL NOT NULL,
                    lastUpdated INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS dx_spots_cache (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    frequency TEXT NOT NULL,
                    callsign TEXT NOT NULL,
                    date TEXT NOT NULL,
                    de TEXT NOT NULL,
                    band TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    comment TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS aprs_cache (
                    callsign TEXT NOT NULL PRIMARY KEY,
                    lat REAL NOT NULL,
                    lon REAL NOT NULL,
                    symbol TEXT NOT NULL,
                    comment TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    distance REAL NOT NULL,
                    bearing REAL NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS favorite_frequencies (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    frequency TEXT NOT NULL,
                    label TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    band TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS favorite_sdrs (
                    url TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    location TEXT NOT NULL
                )
            """.trimIndent())

            // Create user_settings if it doesn't exist
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS user_settings (
                    id INTEGER NOT NULL PRIMARY KEY,
                    callsign TEXT NOT NULL,
                    gridSquare TEXT NOT NULL,
                    homeLat REAL NOT NULL,
                    homeLon REAL NOT NULL,
                    theme TEXT NOT NULL,
                    lastSync INTEGER NOT NULL
                )
            """.trimIndent())

            // Add missing columns to user_settings if they don't exist
            addColumnIfNotExists(db, "user_settings", "name", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "user_settings", "country", "TEXT NOT NULL DEFAULT 'Australia'")
            addColumnIfNotExists(db, "user_settings", "licenceClass", "TEXT NOT NULL DEFAULT 'foundation'")
            addColumnIfNotExists(db, "user_settings", "kiwisdrUrl", "TEXT NOT NULL DEFAULT 'http://kiwisdr.com/public/'")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS callsign_cache (
                    callsign TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    qth TEXT NOT NULL,
                    grid TEXT NOT NULL,
                    licenceClass TEXT NOT NULL,
                    imageUrl TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS club_nets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    frequency TEXT NOT NULL,
                    dayOfWeek INTEGER,
                    specificDate INTEGER,
                    time TEXT NOT NULL,
                    type TEXT NOT NULL,
                    notes TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS radio_profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    manufacturer TEXT NOT NULL,
                    maxPower REAL NOT NULL,
                    supportedModes TEXT NOT NULL,
                    supportedBands TEXT NOT NULL,
                    notes TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS antenna_profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    gain REAL NOT NULL,
                    type TEXT NOT NULL,
                    polarisation TEXT NOT NULL,
                    notes TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS operator_profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    callsign TEXT NOT NULL,
                    portableCallsign TEXT NOT NULL,
                    name TEXT NOT NULL,
                    defaultRadioId INTEGER,
                    defaultAntennaId INTEGER,
                    defaultPower REAL NOT NULL,
                    preferredMode TEXT NOT NULL,
                    preferredBands TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS qsos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    operatorCallsign TEXT NOT NULL,
                    onAirCallsign TEXT NOT NULL,
                    callWorked TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    frequency REAL NOT NULL,
                    band TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    rstSent TEXT NOT NULL,
                    rstReceived TEXT NOT NULL,
                    name TEXT NOT NULL,
                    qth TEXT NOT NULL,
                    gridsquare TEXT NOT NULL,
                    power REAL NOT NULL,
                    radioId INTEGER,
                    antennaId INTEGER,
                    notes TEXT NOT NULL,
                    qslSent INTEGER NOT NULL,
                    qslReceived INTEGER NOT NULL,
                    lotwStatus TEXT NOT NULL,
                    eqslStatus TEXT NOT NULL,
                    clublogStatus TEXT NOT NULL,
                    sotaRef TEXT NOT NULL,
                    potaRef TEXT NOT NULL,
                    wwffRef TEXT NOT NULL,
                    hemaRef TEXT NOT NULL,
                    siotaRef TEXT NOT NULL,
                    vkShireRef TEXT NOT NULL,
                    mySotaRef TEXT NOT NULL,
                    myPotaRef TEXT NOT NULL,
                    myWwffRef TEXT NOT NULL,
                    myHemaRef TEXT NOT NULL,
                    mySiotaRef TEXT NOT NULL,
                    myVkShireRef TEXT NOT NULL,
                    satelliteName TEXT NOT NULL DEFAULT '',
                    satelliteMode TEXT NOT NULL DEFAULT '',
                    contestId TEXT NOT NULL DEFAULT '',
                    country TEXT NOT NULL DEFAULT '',
                    dxcc TEXT NOT NULL DEFAULT '',
                    cqZone INTEGER NOT NULL DEFAULT 0,
                    ituZone INTEGER NOT NULL DEFAULT 0,
                    continent TEXT NOT NULL DEFAULT '',
                    stationLat REAL,
                    stationLon REAL
                )
            """.trimIndent())
            
            db.execSQL("CREATE INDEX IF NOT EXISTS index_qsos_callWorked ON qsos (callWorked)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_qsos_timestamp ON qsos (timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_qsos_band ON qsos (band)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_qsos_mode ON qsos (mode)")
        }
    }

    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Drop and recreate the cache table to ensure clean schema with new columns and unique index
            db.execSQL("DROP TABLE IF EXISTS dx_spots_cache")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS dx_spots_cache (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    frequency TEXT NOT NULL,
                    callsign TEXT NOT NULL,
                    date TEXT NOT NULL,
                    de TEXT NOT NULL,
                    band TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    comment TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    source TEXT NOT NULL DEFAULT 'DX_CLUSTER',
                    location TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_dx_spots_cache_callsign_frequency_timestamp ON dx_spots_cache (callsign, frequency, timestamp)")
        }
    }

    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Recreate dx_spots_cache with full provider fields
            db.execSQL("DROP TABLE IF EXISTS dx_spots_cache")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS dx_spots_cache (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    frequency TEXT NOT NULL,
                    callsign TEXT NOT NULL,
                    date TEXT NOT NULL,
                    de TEXT NOT NULL,
                    band TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    comment TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    source TEXT NOT NULL DEFAULT 'DX_CLUSTER',
                    location TEXT NOT NULL DEFAULT '',
                    activator TEXT NOT NULL DEFAULT '',
                    reference TEXT NOT NULL DEFAULT '',
                    name TEXT NOT NULL DEFAULT '',
                    country TEXT NOT NULL DEFAULT '',
                    latitude REAL,
                    longitude REAL,
                    spotUrl TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_dx_spots_cache_callsign_frequency_timestamp ON dx_spots_cache (callsign, frequency, timestamp)")
        }
    }

    private fun addColumnIfNotExists(db: SupportSQLiteDatabase, tableName: String, columnName: String, columnDef: String) {
        val cursor = db.query("PRAGMA table_info($tableName)")
        var exists = false
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            if (name == columnName) {
                exists = true
                break
            }
        }
        cursor.close()
        if (!exists) {
            db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDef")
        }
    }

    @Volatile
    private var INSTANCE: RobertDatabase? = null

    fun getDatabase(context: Context): RobertDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                RobertDatabase::class.java,
                "robert.db"
            )
            .addMigrations(MIGRATION_11_12, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
            .build()

            INSTANCE = instance

            instance
        }
    }
}
