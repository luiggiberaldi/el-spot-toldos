package com.elspot.toldos

import android.app.Application
import androidx.room.Room
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
                AppDatabase.MIGRATION_4_5
            )
            .build()
    }

    val settings: SettingsStore by lazy { SettingsStore(this) }
    val repository: AppRepository by lazy { AppRepository(database, settings) }
    val backupManager: BackupManager by lazy { BackupManager(this, repository) }
    val notificationScheduler: NotificationScheduler by lazy { NotificationScheduler(this) }
}
