package app.prontosoggiorno.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Versiones del esquema (los JSON exportados están en app/schemas):
 * 1 → inicial. 2 → pausa automática por "no encontrado" (columnas nuevas, migración automática).
 */
@Database(
    entities = [PraticaEntity::class, CheckEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun praticaDao(): PraticaDao
    abstract fun checkDao(): CheckDao

    companion object {
        private const val FILE_NAME = "pronto_soggiorno.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME).build()
    }
}
