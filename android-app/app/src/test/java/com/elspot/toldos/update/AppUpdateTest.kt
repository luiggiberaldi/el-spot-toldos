package com.elspot.toldos.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test
    fun parsesValidManifestAndComparesVersionCode() {
        val update = UpdateManifest.parse(
            """
            {
              "versionCode": 2,
              "versionName": "1.1.0",
              "apkUrl": "https://github.com/luiggiberaldi/el-spot-toldos/releases/download/v1.1.0/el-spot-toldos-1.1.0.apk?download=1",
              "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
              "notes": "Mejoras y correcciones",
              "mandatory": false
            }
            """.trimIndent()
        )

        assertEquals(2, update.versionCode)
        assertEquals("1.1.0", update.versionName)
        assertEquals("Mejoras y correcciones", update.notes)
        assertFalse(update.mandatory)
        assertTrue(update.isNewerThan(1))
        assertFalse(update.isNewerThan(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInsecureManifestUrl() {
        UpdateManifest.parse(
            """
            {"versionCode":2,"versionName":"1.1.0","apkUrl":"http://example.com/app.apk"}
            """
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonApkDownload() {
        UpdateManifest.parse(
            """
            {"versionCode":2,"versionName":"1.1.0","apkUrl":"https://example.com/app.zip"}
            """
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedSha256() {
        UpdateManifest.parse(
            """
            {"versionCode":2,"versionName":"1.1.0","apkUrl":"https://example.com/app.apk","sha256":"not-a-hash"}
            """
        )
    }

    @Test
    fun computesAccurateSha256ForFile() {
        val tempFile = java.io.File.createTempFile("test-sha", ".tmp")
        try {
            tempFile.writeText("EL SPOT TOLDOS TEST", Charsets.UTF_8)
            val hash = GithubUpdateClient.sha256(tempFile)
            // echo -n "EL SPOT TOLDOS TEST" | sha256sum
            assertEquals(64, hash.length)
            assertTrue(hash.matches(Regex("[a-f0-9]{64}")))
        } finally {
            tempFile.delete()
        }
    }
}
