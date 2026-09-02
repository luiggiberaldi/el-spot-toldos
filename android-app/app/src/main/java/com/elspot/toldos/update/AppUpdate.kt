package com.elspot.toldos.update

import com.elspot.toldos.BuildConfig
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Contrato publicado en update.json del repositorio oficial de distribución. */
data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String,
    val mandatory: Boolean
) {
    fun isNewerThanInstalled(): Boolean = isNewerThan(BuildConfig.VERSION_CODE)

    fun isNewerThan(installedVersionCode: Int): Boolean = versionCode > installedVersionCode
}

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val update: AppUpdate) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object UpdateManifest {
    const val URL = "https://raw.githubusercontent.com/luiggiberaldi/el-spot-toldos/main/update.json"

    fun parse(raw: String): AppUpdate {
        val json = JSONObject(raw)
        val versionCode = json.getInt("versionCode")
        require(versionCode > 0) { "versionCode inválido." }
        val versionName = json.getString("versionName").trim()
        require(versionName.isNotBlank()) { "versionName vacío." }
        val apkUrl = json.getString("apkUrl").trim()
        val parsedApkUrl = runCatching { URL(apkUrl) }.getOrNull()
        require(parsedApkUrl?.protocol.equals("https", ignoreCase = true)) {
            "La APK debe descargarse mediante HTTPS."
        }
        require(parsedApkUrl?.path?.endsWith(".apk", ignoreCase = true) == true) {
            "La URL no apunta a una APK."
        }
        val sha256 = json.optString("sha256").trim().lowercase()
        require(sha256.isBlank() || sha256.matches(Regex("[a-f0-9]{64}"))) {
            "sha256 debe contener 64 caracteres hexadecimales."
        }
        return AppUpdate(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            sha256 = sha256,
            notes = json.optString("notes").trim(),
            mandatory = json.optBoolean("mandatory", false)
        )
    }
}

class GithubUpdateClient(
    private val manifestUrl: String = UpdateManifest.URL,
    // Punto de extensión para pruebas unitarias: en producción abre una conexión
    // HTTPS real; en los tests se inyecta una conexión falsa sin red.
    private val abrirConexion: (String) -> HttpURLConnection = { url ->
        URL(url).openConnection() as HttpURLConnection
    }
) {
    fun check(): UpdateCheckResult {
        return runCatching {
            val raw = requestText(manifestUrl)
            val update = UpdateManifest.parse(raw)
            if (update.isNewerThanInstalled()) UpdateCheckResult.Available(update) else UpdateCheckResult.UpToDate
        }.getOrElse { error ->
            UpdateCheckResult.Failed(error.message ?: "No se pudo consultar la actualización.")
        }
    }

    fun download(update: AppUpdate, target: java.io.File): java.io.File {
        require(URL(update.apkUrl).protocol.equals("https", ignoreCase = true)) { "URL de descarga no segura." }
        val connection = abrirConexion(update.apkUrl).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = DOWNLOAD_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "EL-SPOT-TOLDOS/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream")
        }
        return try {
            if (connection.responseCode !in 200..299) {
                throw IOException("El servidor respondió ${connection.responseCode}.")
            }
            require(connection.url.protocol.equals("https", ignoreCase = true)) {
                "La descarga fue redirigida a una conexión no segura."
            }
            val length = connection.contentLengthLong
            require(length != 0L) { "La descarga está vacía." }
            target.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            require(target.length() > 0L) { "La APK descargada está vacía." }
            if (length > 0L) require(target.length() == length) { "La descarga quedó incompleta." }
            if (update.sha256.isNotBlank()) {
                require(sha256(target) == update.sha256) { "La firma SHA-256 de la APK no coincide." }
            }
            target
        } catch (error: Throwable) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun requestText(url: String): String {
        require(url.startsWith("https://")) { "El manifiesto debe usar HTTPS." }
        val connection = abrirConexion(url).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "EL-SPOT-TOLDOS/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) {
                throw IOException("El manifiesto respondió ${connection.responseCode}.")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000
        private const val DOWNLOAD_TIMEOUT_MS = 120_000

        fun sha256(file: java.io.File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
