package com.sifa.sifa_go.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sifa.sifa_go.data.local.dao.PendingInfraccionDao
import com.sifa.sifa_go.data.local.entity.PendingInfraccionEntity

/**
 * Base de datos SQLite oficial de la app (a través de Room).
 * Contiene las tablas necesarias para la cola offline.
 */
@Database(
    entities = [PendingInfraccionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SifaDatabase : RoomDatabase() {

    abstract fun pendingInfraccionDao(): PendingInfraccionDao

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

        @Volatile
        private var INSTANCE: SifaDatabase? = null

        fun getInstance(context: Context): SifaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SifaDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
