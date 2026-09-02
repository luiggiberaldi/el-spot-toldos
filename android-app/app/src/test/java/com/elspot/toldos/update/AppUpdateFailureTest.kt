package com.elspot.toldos.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Verifica que el autoupdate maneja con elegancia los escenarios de falla
 * documentados:
 *
 *  1. Manifiesto inaccesible (404 u otro error HTTP) o basura (HTML) →
 *     `UpdateCheckResult.Failed` con mensaje útil; nunca un crash ni una
 *     actualización aceptada a partir de datos corruptos.
 *  2. Descarga corrupta (SHA-256 del manifiesto distinto al de la APK) →
 *     el archivo parcial se elimina y se propaga un error claro; nunca se
 *     notifica la instalación de un binario no verificado.
 */
class AppUpdateFailureTest {

    // ---- Fakes sin red --------------------------------------------------------

    private class FakeConnection(
        private val statusCode: Int,
        private val body: ByteArray,
    ) : HttpURLConnection(URL("https://falso.example/recurso")) {
        override fun getResponseCode(): Int = statusCode
        override fun getInputStream(): InputStream = ByteArrayInputStream(body)
        override fun getErrorStream(): InputStream? =
            if (statusCode >= 400) ByteArrayInputStream(body) else null
        override fun usingProxy(): Boolean = false
        override fun connect() {}
        override fun disconnect() {}
    }

    /** Cliente cuyo transporte devuelve las conexiones falsas en orden. */
    private fun cliente(vararg conexiones: HttpURLConnection): GithubUpdateClient {
        val cola = conexiones.toMutableList()
        return GithubUpdateClient(
            manifestUrl = "https://falso.example/manifest.json",
            abrirConexion = {
                require(cola.isNotEmpty()) { "Conexión inesperada: no quedan fakes en la cola." }
                cola.removeAt(0)
            },
        )
    }

    private fun manifiestoValido(): String = """
        {
          "versionCode": 99,
          "versionName": "9.9.9",
          "apkUrl": "https://github.com/luiggiberaldi/el-spot-toldos/releases/download/v9.9.9/el-spot-toldos-9.9.9.apk",
          "sha256": "3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a",
          "notes": "Prueba",
          "mandatory": false
        }
    """.trimIndent()

    // ---- 1. Manifiesto inaccesible o corrupto ---------------------------------

    @Test
    fun `manifiesto con 404 termina en Failed con mensaje util`() {
        val resultado = cliente(FakeConnection(404, "404: Not Found".toByteArray())).check()
        assertTrue(resultado is UpdateCheckResult.Failed)
        val fallo = resultado as UpdateCheckResult.Failed
        assertTrue(
            "El mensaje debería mencionar el código HTTP, fue: ${fallo.message}",
            fallo.message.contains("404"),
        )
    }

    @Test
    fun `manifiesto con 500 termina en Failed`() {
        val resultado = cliente(FakeConnection(500, "boom".toByteArray())).check()
        assertTrue(resultado is UpdateCheckResult.Failed)
        assertTrue((resultado as UpdateCheckResult.Failed).message.contains("500"))
    }

    @Test
    fun `manifiesto con HTML de error termina en Failed y no acepta versiones`() {
        // raw.githubusercontent podría servir una página de error con 200; el
        // parseo de JSON debe rechazarla en lugar de aceptar versiones inventadas.
        val html = "<html><body>Servicio no disponible</body></html>".toByteArray()
        val resultado = cliente(FakeConnection(200, html)).check()
        assertTrue(resultado is UpdateCheckResult.Failed)
    }

    @Test
    fun `manifiesto con JSON truncado termina en Failed`() {
        val truncado = """{"versionCode": 99, "versionName": "9.9"""".toByteArray()
        val resultado = cliente(FakeConnection(200, truncado)).check()
        assertTrue(resultado is UpdateCheckResult.Failed)
    }

    // ---- 2. Descarga verificada: SHA-256 y errores de servidor -----------------

    private fun apkDe(contenido: ByteArray): File =
        File.createTempFile("apk-prueba", ".apk").apply { writeBytes(contenido) }

    @Test
    fun `descarga con sha256 distinto borra el archivo y lanza error claro`() {
        val contenido = "APK FALSA CON HASH INCORRECTO".toByteArray()
        val hashReal = GithubUpdateClient.sha256(apkDe(contenido).also { it.deleteOnExit() })
        val manifiesto = manifiestoValido()
            .replace("3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a", hashReal.take(60) + "beef")
        val update = UpdateManifest.parse(manifiesto)
        val destino = File.createTempFile("apk-destino", ".apk")

        val error = runCatching {
            cliente(
                FakeConnection(200, contenido),
            ).download(update, destino)
        }.exceptionOrNull()

        assertTrue("Debería fallar por hash distinto", error is IllegalArgumentException)
        assertTrue(
            "El error debería mencionar SHA-256, fue: ${error?.message}",
            error?.message?.contains("SHA-256") == true,
        )
        assertFalse("El archivo parcial debe eliminarse", destino.exists())
    }

    @Test
    fun `descarga correcta pasa la verificacion de sha256`() {
        val contenido = "APK VALIDA DE PRUEBA".toByteArray()
        val hashReal = GithubUpdateClient.sha256(apkDe(contenido).also { it.deleteOnExit() })
        val manifiesto = manifiestoValido()
            .replace("3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a3f0a", hashReal)
        val update = UpdateManifest.parse(manifiesto)
        val destino = File.createTempFile("apk-destino-ok", ".apk")

        cliente(FakeConnection(200, contenido)).download(update, destino)

        assertTrue(destino.exists())
        assertEquals(contenido.size.toLong(), destino.length())
        destino.delete()
    }

    @Test
    fun `error del servidor al descargar borra el archivo parcial`() {
        val update = UpdateManifest.parse(manifiestoValido())
        val destino = File.createTempFile("apk-destino-500", ".apk")

        val error = runCatching {
            cliente(FakeConnection(503, "maintenance".toByteArray())).download(update, destino)
        }.exceptionOrNull()

        assertTrue(error is java.io.IOException)
        assertTrue((error as java.io.IOException).message?.contains("503") == true)
        assertFalse("No debe quedar archivo parcial", destino.exists())
    }

    @Test
    fun `descarga vacia del servidor borra el archivo parcial`() {
        val update = UpdateManifest.parse(manifiestoValido())
        val destino = File.createTempFile("apk-destino-vacia", ".apk")

        val error = runCatching {
            cliente(FakeConnection(200, ByteArray(0))).download(update, destino)
        }.exceptionOrNull()

        assertTrue(error != null)
        assertFalse(destino.exists())
    }
}
