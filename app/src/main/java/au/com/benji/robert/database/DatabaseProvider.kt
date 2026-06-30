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

    @Volatile
    private var INSTANCE: RobertDatabase? = null

    fun getDatabase(context: Context): RobertDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                RobertDatabase::class.java,
                "robert.db"
            )
            .addMigrations(MIGRATION_11_12)
            .fallbackToDestructiveMigration() // Keep as safety, but migrations should prevent data loss
            .build()

            INSTANCE = instance

            instance
        }
    }
}
