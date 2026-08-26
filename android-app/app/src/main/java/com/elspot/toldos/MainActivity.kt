package com.elspot.toldos

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.elspot.toldos.ui.ElSpotApp
import com.elspot.toldos.ui.theme.ElSpotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val elSpotApplication = application as ElSpotApplication
        elSpotApplication.notificationScheduler.createChannels()
        elSpotApplication.notificationScheduler.scheduleUpdateChecks()
        handleUpdateIntent(intent)
        setContent {
            ElSpotTheme {
                ElSpotApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumePendingUpdate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
        resumePendingUpdate()
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.action != ACTION_INSTALL_UPDATE) return
        val fileName = intent.getStringExtra(EXTRA_UPDATE_FILE) ?: return
        updatePreferences.edit()
            .putString(PENDING_UPDATE_FILE, fileName)
            .putBoolean(AWAITING_INSTALL_PERMISSION, false)
            .apply()
    }

    private fun resumePendingUpdate() {
        val fileName = updatePreferences.getString(PENDING_UPDATE_FILE, null) ?: return
        val file = File(filesDir, "updates/$fileName")
        if (!file.isFile) {
            clearPendingUpdate()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            if (updatePreferences.getBoolean(AWAITING_INSTALL_PERMISSION, false)) {
                updatePreferences.edit().putBoolean(AWAITING_INSTALL_PERMISSION, false).apply()
                return
            }
            updatePreferences.edit().putBoolean(AWAITING_INSTALL_PERMISSION, true).apply()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
            })
            return
        }
        clearPendingUpdate()
        val uri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), file)
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun clearPendingUpdate() {
        updatePreferences.edit()
            .remove(PENDING_UPDATE_FILE)
            .putBoolean(AWAITING_INSTALL_PERMISSION, false)
            .apply()
    }

    private val updatePreferences by lazy {
        getSharedPreferences(UPDATE_PREFERENCES, MODE_PRIVATE)
    }

    companion object {
        const val ACTION_INSTALL_UPDATE = "com.elspot.toldos.action.INSTALL_UPDATE"
        const val EXTRA_UPDATE_FILE = "update_file"
        const val EXTRA_UPDATE_VERSION = "update_version"
        private const val UPDATE_PREFERENCES = "elspot_updates"
        private const val PENDING_UPDATE_FILE = "pending_update_file"
        private const val AWAITING_INSTALL_PERMISSION = "awaiting_install_permission"
    }
}
