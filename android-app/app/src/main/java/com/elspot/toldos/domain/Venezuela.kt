package com.elspot.toldos.domain

import java.util.Locale

/** Capitaliza la primera letra de cada palabra para nombres propios de clientes, toldos y negocios. */
fun capitalizeWords(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { word ->
        val sb = StringBuilder()
        var capitalizeNext = true
        for (ch in word.lowercase(Locale.ROOT)) {
            if (capitalizeNext && ch.isLetter()) {
                sb.append(ch.titlecaseChar())
                capitalizeNext = false
            } else {
                sb.append(ch)
                if (ch.isDigit()) {
                    capitalizeNext = false
                } else if (!ch.isLetter()) {
                    capitalizeNext = true
                }
            }
        }
        sb.toString()
    }
}

fun formatVenezuelanDocument(value: String): String {
    val raw = value.trim().uppercase().replace(" ", "")
    if (raw.isBlank()) return ""
    val prefix = raw.firstOrNull { it in "VEJGPT" } ?: 'V'
    val digits = raw.removePrefix(prefix.toString()).replace(Regex("[^0-9]"), "")
    if (digits.isBlank()) return raw
    return if (prefix == 'V' || prefix == 'E') {
        val grouped = digits.take(9).replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ".")
        "$prefix-$grouped"
    } else {
        "$prefix-${digits.take(10)}"
    }
}

fun formatVenezuelanPhone(value: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return ""
    val digits = raw.replace(Regex("[^0-9]"), "")
    val national = when {
        digits.length == 12 && digits.startsWith("58") -> "0${digits.drop(2)}"
        digits.length == 10 && (digits.startsWith("2") || digits.startsWith("4")) -> "0$digits"
        else -> digits
    }
    return if (national.length == 11 && national.startsWith("0")) {
        "${national.take(4)}-${national.drop(4)}"
    } else raw
}

fun safeFilePart(value: String, fallback: String = "Cliente"): String = value.trim()
    .replace(Regex("[^A-Za-z0-9ÁÉÍÓÚáéíóúÑñ]+"), "-")
    .trim('-')
    .ifBlank { fallback }
