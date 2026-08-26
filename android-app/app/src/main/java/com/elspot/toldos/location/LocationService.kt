package com.elspot.toldos.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Locale

class LocationService(private val context: Context) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun current(): LocationResult = suspendCancellableCoroutine { continuation ->
        if (!hasPermission()) {
            continuation.resumeWithException(IllegalStateException("Permiso de ubicación no concedido."))
            return@suspendCancellableCoroutine
        }
        val cancellation = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    continuation.resumeWithException(IllegalStateException("No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo."))
                } else {
                    continuation.resume(LocationResult(location.latitude, location.longitude, reverseGeocode(location.latitude, location.longitude)))
                }
            }
            .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            val addresses: List<Address> = Geocoder(context, Locale("es", "VE")).getFromLocation(latitude, longitude, 1).orEmpty()
            addresses.firstOrNull()?.getAddressLine(0)?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

data class LocationResult(val latitude: Double, val longitude: Double, val address: String? = null)
