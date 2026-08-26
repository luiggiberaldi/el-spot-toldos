package com.elspot.toldos.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_TIME = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "VE"))
private val DATE = SimpleDateFormat("dd/MM/yyyy", Locale("es", "VE"))

fun formatDateTime(millis: Long): String = DATE_TIME.format(Date(millis))
fun formatDate(millis: Long): String = DATE.format(Date(millis))
fun calculateReturnAt(startAt: Long, hours: Int): Long = startAt + hours * 60L * 60L * 1000L
