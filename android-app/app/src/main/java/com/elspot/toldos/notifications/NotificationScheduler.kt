package com.elspot.toldos.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.elspot.toldos.ElSpotApplication
import com.elspot.toldos.MainActivity
import com.elspot.toldos.R
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.update.UpdateCheckWorker
import java.util.concurrent.TimeUnit

object NotificationChannels {
    const val RETURNS = "returns"
    const val UPDATES = "updates"
}

class NotificationScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(RETURNS_ID, "Devoluciones", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(UPDATES_ID, "Actualizaciones", NotificationManager.IMPORTANCE_DEFAULT)
            )
        )
    }

    fun scheduleRentalReminder(rentalId: String, rentalFolio: String, returnAt: Long, reminderMinutes: Int) {
        val delay = returnAt - System.currentTimeMillis() - reminderMinutes * 60_000L
        if (delay <= 0L) return
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_RENTAL_ID, rentalId)
            .putString(ReminderWorker.KEY_FOLIO, rentalFolio)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tagFor(rentalId))
            .addTag(ALL_REMINDERS_TAG)
            .build()
        workManager.enqueueUniqueWork(
            "return-$rentalId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleExpiredReminder(rentalId: String, rentalFolio: String, returnAt: Long) {
        val delay = returnAt - System.currentTimeMillis()
        if (delay <= 0L) return
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_RENTAL_ID, rentalId)
            .putString(ReminderWorker.KEY_FOLIO, rentalFolio)
            .putBoolean(ReminderWorker.KEY_EXPIRED, true)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tagFor(rentalId))
            .addTag(ALL_REMINDERS_TAG)
            .build()
        workManager.enqueueUniqueWork(
            "expired-$rentalId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelRental(rentalId: String) {
        workManager.cancelUniqueWork("return-$rentalId")
        workManager.cancelUniqueWork("expired-$rentalId")
    }

    fun cancelAllReminders() {
        workManager.cancelAllWorkByTag(ALL_REMINDERS_TAG)
    }

    fun scheduleUpdateChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun checkForUpdatesNow(manual: Boolean = true) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putBoolean(UpdateCheckWorker.KEY_MANUAL, manual).build())
            .build()
        workManager.enqueueUniqueWork(
            MANUAL_UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun tagFor(id: String) = "rental-reminder-$id"

    companion object {
        const val ALL_REMINDERS_TAG = "rental-reminders"
        const val RETURNS_ID = NotificationChannels.RETURNS
        const val UPDATES_ID = NotificationChannels.UPDATES
        const val UPDATE_WORK_NAME = "elspot-update-check"
        const val MANUAL_UPDATE_WORK_NAME = "elspot-manual-update-check"
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val rentalId = inputData.getString(KEY_RENTAL_ID) ?: return Result.success()
        val expired = inputData.getBoolean(KEY_EXPIRED, false)

        // Verificación fáctica en Room DB antes de emitir notificación
        val app = applicationContext as? ElSpotApplication
        val rental = app?.database?.alquileres()?.findById(rentalId)

        // Si el alquiler ya no existe o ya fue devuelto o cancelado, no alertar
        if (app != null && (rental == null || (rental.estado != RentalStatus.ACTIVE.name && rental.estado != RentalStatus.DELIVERED.name))) {
            return Result.success()
        }

        val folio = rental?.folio?.ifBlank { null } ?: inputData.getString(KEY_FOLIO) ?: "alquiler"
        val title = if (expired) "Devolución vencida" else "Devolución próxima"
        val text = if (expired) {
            "El alquiler $folio alcanzó su hora de devolución pactada."
        } else {
            "El alquiler $folio se acerca a su hora de devolución."
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "rentals")
            putExtra(EXTRA_RENTAL_ID, rentalId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (if (expired) 20_000 else 10_000) + (rentalId.hashCode() and 0x7FFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationScheduler.RETURNS_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (expired) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationId = if (expired) (10_000 + (rentalId.hashCode() and 0x7FFF)) else (rentalId.hashCode() and 0x7FFF)
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        return Result.success()
    }

    companion object {
        const val KEY_RENTAL_ID = "rental_id"
        const val KEY_FOLIO = "folio"
        const val KEY_EXPIRED = "expired"
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_RENTAL_ID = "rental_id"
    }
}
