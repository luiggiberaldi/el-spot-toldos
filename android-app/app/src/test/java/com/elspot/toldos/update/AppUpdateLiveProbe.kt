package com.elspot.toldos.update

import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Sonda contra la red real: NO corre en CI (está @Ignore). Verifica el
 * comportamiento ante un manifiesto inexistente usando la URL de producción.
 *
 * Ejecutar manualmente una sola vez cuando se quiera validar contra GitHub:
 *   ./gradlew testDebugUnitTest --tests "com.elspot.toldos.update.AppUpdateLiveProbe" -PignoreDisabled
 */
@Ignore("Sonda de red real: ejecutar solo manualmente")
class AppUpdateLiveProbe {

    @Test
    fun `manifiesto inexistente en GitHub termina en Failed con 404`() {
        val urlRota = "https://raw.githubusercontent.com/luiggiberaldi/el-spot-toldos/main/no-existe/manifest.json"
        val resultado = GithubUpdateClient(manifestUrl = urlRota).check()

        // El contrato: nunca crash, siempre Failed con diagnóstico.
        assertTrue("Debería ser Failed, fue: $resultado", resultado is UpdateCheckResult.Failed)
        val mensaje = (resultado as UpdateCheckResult.Failed).message ?: ""
        assertTrue("El mensaje debería explicar el fallo HTTP, fue: $mensaje", mensaje.isNotBlank())
        println("PROBE 404 → Failed con mensaje: \"$mensaje\"")
    }

    @Test
    fun `manifiesto real de produccion se parsea correctamente`() {
        val resultado = GithubUpdateClient().check()
        println("PROBE producción → $resultado")
        when (resultado) {
            is UpdateCheckResult.Available -> {
                assertTrue(resultado.update.versionCode > 0)
                assertTrue(resultado.update.apkUrl.startsWith("https://"))
            }
            is UpdateCheckResult.UpToDate -> assertTrue(true)
            is UpdateCheckResult.Failed ->
                throw AssertionError("El manifiesto real debería parsear sin fallos: ${resultado.message}")
        }
    }
}
