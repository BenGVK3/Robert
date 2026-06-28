package au.com.benji.robert.database

import android.content.Context

object DatabaseModule {

    private var database: RobertDatabase? = null

    fun database(context: Context): RobertDatabase {

        return database ?: DatabaseProvider.getDatabase(context).also {
            database = it
        }
    }

    fun shackDao(context: Context): ShackDao {

        return database(context).shackDao()
    }

    fun logDao(context: Context): LogDao {

        return database(context).logDao()
    }
}