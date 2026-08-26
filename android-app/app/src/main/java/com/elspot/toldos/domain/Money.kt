package com.elspot.toldos.domain

import com.elspot.toldos.data.ConfigSnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val VENEZUELA = Locale("es", "VE")

fun parseDollarCents(raw: String): Long? = runCatching {
    val value = raw.trim().replace(" ", "")
    if (value.isBlank()) return null
    // Acepta tanto el formato de teclado 100.50 como el venezolano 100,50.
    val normalized = if (value.contains(',') && value.contains('.')) {
        value.replace(".", "").replace(',', '.')
    } else {
        value.replace(',', '.')
    }
    BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
}.getOrNull()

fun centsToDollarText(cents: Long): String {
    val format = NumberFormat.getNumberInstance(VENEZUELA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$ ${format.format(BigDecimal.valueOf(cents, 2))}"
}

fun centsToBolivarText(cents: Long, exchangeRate: Double): String {
    if (exchangeRate <= 0.0) return ""
    val bolivares = BigDecimal.valueOf(cents, 2)
        .multiply(BigDecimal.valueOf(exchangeRate))
        .setScale(2, RoundingMode.HALF_UP)
    val format = NumberFormat.getNumberInstance(VENEZUELA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "Bs ${format.format(bolivares)}"
}

fun formatDual(cents: Long, config: ConfigSnapshot): String {
    val dollars = centsToDollarText(cents)
    val bolivares = centsToBolivarText(cents, config.exchangeRate)
    return if (bolivares.isBlank()) dollars else "$dollars ($bolivares)"
}

fun exchangeRateText(rate: Double): String {
    val format = NumberFormat.getNumberInstance(VENEZUELA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "Bs ${format.format(rate)} por 1 $"
}
