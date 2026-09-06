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
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientes(): ClienteDao
    abstract fun toldos(): ToldoDao
    abstract fun alquileres(): AlquilerDao
    abstract fun recibos(): ReciboDao
    abstract fun bitacora(): BitacoraDao

    companion object {
        /** v1.2.2: Corrección automática de recibos históricos marcados erróneamente como DUE. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    UPDATE recibos
                    SET estadoPago = 'PAID'
                    WHERE estadoPago != 'PAID'
                      AND (
                        LOWER(concepto) LIKE '%ya recibido%'
                        OR LOWER(concepto) LIKE '%abono recibido%'
                        OR LOWER(concepto) LIKE '%saldado%'
                        OR LOWER(concepto) LIKE '%cancelación%'
                        OR LOWER(concepto) LIKE '%cancelacion%'
                        OR LOWER(concepto) LIKE '%comprobante de pago%'
                      )
                    """.trimIndent()
                )
            }
        }

        /** v1.2.0: Costo de flete/transporte y fotografía de comprobante de entrega en alquileres. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alquileres ADD COLUMN fleteCents INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE alquileres ADD COLUMN fotoEntregaUri TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Reparación única de datos creados por v1.0.4 y anteriores.
         *
         * Esa versión persistía `montoTotalCents = round(Σ líneas / 2)` en alquileres de 12 h
         * (el "doble 50 %" que v1.0.5 corrigió solo hacia adelante, commit 1928ead).
         *
         * Se repara EXCLUSIVAMENTE la firma demostrable de ese error, sin valores mágicos:
         * alquileres 12 h cuyo total guardado coincide con la mitad redondeada de la suma de
         * sus propias líneas. Es idempotente (tras reparar, total == Σ y la condición deja de
         * aplicar) y no se ejecuta en onOpen: corre una sola vez por dispositivo, al migrar 4→5.
         *
         * Intencionalmente NO toca:
         *  - toldos: un `tarifa12hCents` NULL o igual a la mitad de la base es un precio 12 h
         *    legítimo (la regla de negocio rellena el campo vacío con el 50 % de las 24 h);
         *  - alquiler_items: la línea congela la tarifa pactada al crearse el alquiler
         *    (precio 12 h configurado, o 50 % de la base si el toldo no lo tenía);
         *  - recibos: documentos históricos con snapshot financiero congelado.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Espejo SQL de `totalH12ConDobleMitad` (RentalRules.kt); mantener ambas en sincronía.
                database.execSQL(
                    """
                    UPDATE alquileres
                    SET montoTotalCents = (
                        SELECT COALESCE(SUM(cantidad * tarifaCents), 0)
                        FROM alquiler_items
                        WHERE alquiler_items.alquilerId = alquileres.id
                    )
                    WHERE UPPER(TRIM(modalidad)) IN ('H12', '12H', '12 HORAS', '12 HORAS (MITAD DE TARIFA)')
                      AND montoTotalCents = (
                          (SELECT COALESCE(SUM(cantidad * tarifaCents), 0)
                           FROM alquiler_items
                           WHERE alquiler_items.alquilerId = alquileres.id) + 1
                      ) / 2
                    """.trimIndent()
                )
            }
        }

        /** v1.0.10: referencia libre de ubicación en alquileres (opcional, texto vacío). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alquileres ADD COLUMN referenciaUbicacion TEXT NOT NULL DEFAULT ''")
            }
        }

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
