package com.elspot.toldos.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elspot.toldos.MainActivity
import com.elspot.toldos.R
import com.elspot.toldos.notifications.NotificationScheduler
import java.io.File

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val manual = inputData.getBoolean(KEY_MANUAL, false)
        return when (val result = GithubUpdateClient().check()) {
            UpdateCheckResult.UpToDate -> {
                if (manual) showStatus("EL SPOT TOLDOS está actualizado", "No hay una versión nueva disponible.")
                Result.success()
            }
            is UpdateCheckResult.Failed -> {
                if (manual) showStatus("No se pudo buscar actualización", result.message)
                Result.success()
            }
            is UpdateCheckResult.Available -> downloadAndNotify(result.update, manual)
        }
    }

    private fun downloadAndNotify(update: AppUpdate, manual: Boolean): Result {
        val directory = File(applicationContext.filesDir, UPDATE_DIRECTORY).apply { mkdirs() }
        // Mismo nombre canónico que la release y update.json: el-spot-toldos-<versionName>.apk
        val target = File(directory, "el-spot-toldos-${update.versionName}.apk")
        return runCatching {
            GithubUpdateClient().download(update, target)
            showUpdateReady(update, target)
            Result.success()
        }.getOrElse { error ->
            if (manual) showStatus("No se pudo descargar la actualización", error.message ?: "Intenta nuevamente.")
            Result.success()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showUpdateReady(update: AppUpdate, apk: File) {
        if (!notificationsAllowed()) return
        val installIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = MainActivity.ACTION_INSTALL_UPDATE
            putExtra(MainActivity.EXTRA_UPDATE_FILE, apk.name)
            putExtra(MainActivity.EXTRA_UPDATE_VERSION, update.versionName)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            update.versionCode,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = buildString {
            append("Versión ${update.versionName} lista para instalar.")
            if (update.notes.isNotBlank()) append(" ${update.notes}")
        }
        val notification = NotificationCompat.Builder(applicationContext, NotificationScheduler.UPDATES_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Actualización disponible")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun showStatus(title: String, text: String) {
        if (!notificationsAllowed()) return
        val notification = NotificationCompat.Builder(applicationContext, NotificationScheduler.UPDATES_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(STATUS_NOTIFICATION_ID, notification)
    }

    private fun notificationsAllowed(): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val KEY_MANUAL = "manual_check"
        const val UPDATE_DIRECTORY = "updates"
        const val UPDATE_NOTIFICATION_ID = 4_201
        const val STATUS_NOTIFICATION_ID = 4_202
    }
}
