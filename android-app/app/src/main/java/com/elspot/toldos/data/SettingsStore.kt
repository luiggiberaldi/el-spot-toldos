package com.elspot.toldos.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.elSpotDataStore by preferencesDataStore(name = "elspot_config")

class SettingsStore(private val context: Context) {
    private object Keys {
        val businessName = stringPreferencesKey("business_name")
        val rif = stringPreferencesKey("rif")
        val phone = stringPreferencesKey("phone")
        val address = stringPreferencesKey("address")
        val exchangeRate = doublePreferencesKey("exchange_rate")
        val logoUri = stringPreferencesKey("logo_uri")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val reminderMinutes = intPreferencesKey("reminder_minutes")
        val lastReceipt = intPreferencesKey("last_receipt")
        val lastRental = intPreferencesKey("last_rental")
    }

    val configFlow: Flow<ConfigSnapshot> = context.elSpotDataStore.data.map { prefs ->
        ConfigSnapshot(
            businessName = prefs[Keys.businessName] ?: "EL SPOT",
            rif = prefs[Keys.rif] ?: "",
            phone = prefs[Keys.phone] ?: "",
            address = prefs[Keys.address] ?: "",
            exchangeRate = prefs[Keys.exchangeRate] ?: 0.0,
            logoUri = prefs[Keys.logoUri] ?: "",
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: true,
            reminderMinutes = prefs[Keys.reminderMinutes] ?: 60,
            lastReceiptNumber = prefs[Keys.lastReceipt] ?: 0,
            lastRentalNumber = prefs[Keys.lastRental] ?: 0
        )
    }

    suspend fun current(): ConfigSnapshot = configFlow.first()

    suspend fun save(config: ConfigSnapshot) {
        context.elSpotDataStore.edit { prefs ->
            prefs[Keys.businessName] = config.businessName
            prefs[Keys.rif] = config.rif
            prefs[Keys.phone] = config.phone
            prefs[Keys.address] = config.address
            prefs[Keys.exchangeRate] = config.exchangeRate.coerceAtLeast(0.0)
            prefs[Keys.logoUri] = config.logoUri
            prefs[Keys.notificationsEnabled] = config.notificationsEnabled
            prefs[Keys.reminderMinutes] = config.reminderMinutes.coerceIn(5, 10080)
            prefs[Keys.lastReceipt] = config.lastReceiptNumber.coerceAtLeast(0)
            prefs[Keys.lastRental] = config.lastRentalNumber.coerceAtLeast(0)
        }
    }

    suspend fun nextReceiptFolio(): String {
        var number = 0
        context.elSpotDataStore.edit { prefs ->
            number = (prefs[Keys.lastReceipt] ?: 0) + 1
            prefs[Keys.lastReceipt] = number
        }
        return "REC-${number.toString().padStart(4, '0')}"
    }

    suspend fun nextRentalFolio(): String {
        var number = 0
        context.elSpotDataStore.edit { prefs ->
            number = (prefs[Keys.lastRental] ?: 0) + 1
            prefs[Keys.lastRental] = number
        }
        return "ALQ-${number.toString().padStart(4, '0')}"
    }
}
