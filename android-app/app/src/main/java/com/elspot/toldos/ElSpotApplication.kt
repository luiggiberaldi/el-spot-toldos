package com.elspot.toldos

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elspot.toldos.data.AppDatabase
import com.elspot.toldos.data.AppRepository
import com.elspot.toldos.data.BackupManager
import com.elspot.toldos.data.SettingsStore
import com.elspot.toldos.notifications.NotificationScheduler

class ElSpotApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "elspot.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL(
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
            })
            .build()
    }

    val settings: SettingsStore by lazy { SettingsStore(this) }
    val repository: AppRepository by lazy { AppRepository(database, settings) }
    val backupManager: BackupManager by lazy { BackupManager(this, repository) }
    val notificationScheduler: NotificationScheduler by lazy { NotificationScheduler(this) }
}
