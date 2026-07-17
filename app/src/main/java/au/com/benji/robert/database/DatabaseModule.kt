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

    fun repeaterDao(context: Context): RepeaterDao {

        return database(context).repeaterDao()
    }

    fun moonDao(context: Context): MoonDao {
        return database(context).moonDao()
    }

    fun weatherDao(context: Context): WeatherDao {
        return database(context).weatherDao()
    }

    fun propagationDao(context: Context): PropagationDao {
        return database(context).propagationDao()
    }

    fun cacheDao(context: Context): CacheDao {
        return database(context).cacheDao()
    }

    fun netDao(context: Context): NetDao {
        return database(context).netDao()
    }

    fun logbookDao(context: Context): LogbookDao {
        return database(context).logbookDao()
    }

    private var logbookRepository: au.com.benji.robert.repository.LogbookRepository? = null

    fun logbookRepository(context: Context): au.com.benji.robert.repository.LogbookRepository {
        return logbookRepository ?: au.com.benji.robert.repository.LogbookRepository(
            logbookDao(context),
            context.applicationContext
        ).also { logbookRepository = it }
    }

    private var bandConditionsRepository: au.com.benji.robert.repository.propagation.BandConditionsRepository? = null

    fun bandConditionsRepository(context: Context): au.com.benji.robert.repository.propagation.BandConditionsRepository {
        return bandConditionsRepository ?: au.com.benji.robert.repository.propagation.BandConditionsRepository(
            propagationDao(context)
        ).also { bandConditionsRepository = it }
    }
}