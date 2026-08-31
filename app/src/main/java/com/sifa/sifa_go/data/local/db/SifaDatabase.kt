package com.sifa.sifa_go.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sifa.sifa_go.data.local.dao.PendingInfraccionDao
import com.sifa.sifa_go.data.local.dao.TipoInfraccionDao
import com.sifa.sifa_go.data.local.entity.PendingInfraccionEntity
import com.sifa.sifa_go.data.local.entity.TipoInfraccionEntity

/**
 * Base de datos SQLite oficial de la app (a través de Room).
 * Contiene las tablas necesarias para la cola offline y la caché de tipologías.
 */
@Database(
    entities = [
        PendingInfraccionEntity::class,
        TipoInfraccionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SifaDatabase : RoomDatabase() {

    abstract fun pendingInfraccionDao(): PendingInfraccionDao

    abstract fun tipoInfraccionDao(): TipoInfraccionDao

    companion object {
        private const val DATABASE_NAME = "sifa_offline.db"

        /** v1 -> v2: añade la columna failureReason para diagnosticar envíos fallidos. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pending_infracciones " +
                        "ADD COLUMN failureReason TEXT"
                )
            }
        }

        /** v2 -> v3: crea la tabla caché de tipologías de infracción. No es destructiva. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tipos_infraccion (" +
                        "id INTEGER NOT NULL, " +
                        "nombre TEXT NOT NULL, " +
                        "PRIMARY KEY(id))"
                )
            }
        }

        @Volatile
        private var INSTANCE: SifaDatabase? = null

        fun getInstance(context: Context): SifaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SifaDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
