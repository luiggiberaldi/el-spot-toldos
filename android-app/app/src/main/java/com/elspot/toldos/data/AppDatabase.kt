package com.elspot.toldos.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClienteEntity::class,
        ToldoEntity::class,
        AlquilerEntity::class,
        AlquilerItemEntity::class,
        ReciboEntity::class,
        BitacoraEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientes(): ClienteDao
    abstract fun toldos(): ToldoDao
    abstract fun alquileres(): AlquilerDao
    abstract fun recibos(): ReciboDao
    abstract fun bitacora(): BitacoraDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE toldos ADD COLUMN tarifa12hCents INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recibos ADD COLUMN estadoPago TEXT NOT NULL DEFAULT 'POR_PAGAR'")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.query("PRAGMA table_info(toldos)").use { cursor ->
                    var hasUnits = false
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0 && cursor.getString(nameIndex) == "unidades") {
                            hasUnits = true
                            break
                        }
                    }
                    if (!hasUnits) {
                        database.execSQL("ALTER TABLE toldos ADD COLUMN unidades INTEGER NOT NULL DEFAULT 1")
                    }
                }
            }
        }
    }
}
