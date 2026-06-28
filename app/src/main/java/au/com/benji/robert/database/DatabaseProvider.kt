package au.com.benji.robert.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: RobertDatabase? = null

    fun getDatabase(context: Context): RobertDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                RobertDatabase::class.java,
                "robert.db"
            ).build()

            INSTANCE = instance

            instance
        }
    }
}