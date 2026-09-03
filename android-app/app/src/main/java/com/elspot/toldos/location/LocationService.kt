package com.elspot.toldos.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
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

fun openGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String = "Ubicación del evento") {
    val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val opened = runCatching {
        context.startActivity(mapIntent)
        true
    }.getOrDefault(false)

    if (!opened) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(webIntent) }
    }
}

fun parseCoordinates(input: String): Pair<Double, Double>? {
    val text = input.trim()
    if (text.isEmpty()) return null

    // Pattern 1: URL with @lat,lng (e.g. Google Maps URLs)
    val atPattern = Regex("""@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""")
    atPattern.find(text)?.let {
        val lat = it.groupValues[1].toDoubleOrNull()
        val lng = it.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
            return Pair(lat, lng)
        }
    }

    // Pattern 2: URL with q=lat,lng or ll=lat,lng
    val queryPattern = Regex("""[?&](?:q|ll)=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""")
    queryPattern.find(text)?.let {
        val lat = it.groupValues[1].toDoubleOrNull()
        val lng = it.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
            return Pair(lat, lng)
        }
    }

    // Pattern 3: direct pair (e.g. "10.142918, -68.016897" or "10.142918 -68.016897")
    val directPattern = Regex("""(-?\d+(?:\.\d+)?)\s*[,;\s]\s*(-?\d+(?:\.\d+)?)""")
    directPattern.find(text)?.let {
        val lat = it.groupValues[1].toDoubleOrNull()
        val lng = it.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
            return Pair(lat, lng)
        }
    }

    return null
}
