package com.elspot.toldos.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.elspot.toldos.data.ReceiptPaymentStatus
import com.elspot.toldos.data.ReceiptSnapshot
import com.elspot.toldos.domain.capitalizeWords
import com.elspot.toldos.domain.centsToBolivarText
import com.elspot.toldos.domain.centsToDollarText
import com.elspot.toldos.domain.exchangeRateText
import com.elspot.toldos.domain.formatDateTime
import com.elspot.toldos.domain.safeFilePart
import java.io.File
import java.io.FileOutputStream

class ReceiptPdfService(private val context: Context) {
    private val pageWidth = 595f
    private val pageHeight = 842f
    private val margin = 42f
    private val contentWidth = pageWidth - margin * 2
    private val primary = android.graphics.Color.rgb(32, 125, 181)
    private val darkBlue = android.graphics.Color.rgb(32, 91, 132)
    private val lightBlue = android.graphics.Color.rgb(170, 210, 240)
    private val lightGray = android.graphics.Color.rgb(190, 202, 216)
    private val headerDark = android.graphics.Color.rgb(241, 246, 250)
    private val pageBackground = android.graphics.Color.WHITE
    private val text = android.graphics.Color.rgb(24, 39, 58)
    private val muted = android.graphics.Color.rgb(91, 108, 127)
    private val border = android.graphics.Color.rgb(211, 222, 233)
    private val pale = android.graphics.Color.rgb(247, 250, 253)

    fun create(snapshot: ReceiptSnapshot): File {
        val directory = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(directory, "Recibo-${safeFilePart(snapshot.clientName)}-${snapshot.folio}.pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = pageBackground
        canvas.drawRect(0f, 0f, pageWidth, pageHeight, paint)
        var y = drawHeader(canvas, paint, snapshot)

        paint.color = primary
        canvas.drawRect(margin, y, pageWidth - margin, y + 3f, paint)
        y += 23f
        drawText(canvas, paint, "COMPROBANTE DE ALQUILER", margin, y, 10f, muted, true)
        drawText(canvas, paint, snapshot.concept.ifBlank { "Servicio de alquiler de toldos" }, pageWidth - margin, y, 10f, text, false, true)
        y += 18f

        val cardGap = 14f
        val cardWidth = (contentWidth - cardGap) / 2f
        val clientFields = buildClientFields(snapshot)
        val rentalFields = buildRentalFields(snapshot)
        val clientHeight = measureCardHeight(paint, clientFields, cardWidth - 28f)
        val rentalHeight = measureCardHeight(paint, rentalFields, cardWidth - 28f)
        val cardHeight = maxOf(166f, clientHeight, rentalHeight)

        drawCard(canvas, paint, margin, y, cardWidth, cardHeight)
        drawCard(canvas, paint, margin + cardWidth + cardGap, y, cardWidth, cardHeight)
        drawCardSection(canvas, paint, "CLIENTE", clientFields, margin, y, cardWidth)
        drawCardSection(canvas, paint, "DETALLE DEL SERVICIO", rentalFields, margin + cardWidth + cardGap, y, cardWidth)
        y += cardHeight + 24f

        drawText(canvas, paint, "CONCEPTOS DEL ALQUILER", margin, y, 10f, primary, true)
        y += 13f
        val headerBottom = y + 28f
        paint.color = darkBlue
        canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, headerBottom), 5f, 5f, paint)
        drawText(canvas, paint, "DESCRIPCIÓN", margin + 12f, y + 18f, 9f, android.graphics.Color.WHITE, true)
        drawText(canvas, paint, "CANT.", pageWidth - 190f, y + 18f, 9f, android.graphics.Color.WHITE, true, true)
        drawText(canvas, paint, "TARIFA", pageWidth - 112f, y + 18f, 9f, android.graphics.Color.WHITE, true, true)
        drawText(canvas, paint, "SUBTOTAL", pageWidth - margin - 10f, y + 18f, 9f, android.graphics.Color.WHITE, true, true)
        y = headerBottom

        snapshot.items.forEachIndexed { index, item ->
            val itemLabel = if (item.size.isBlank()) capitalizeWords(item.name) else "${capitalizeWords(item.name)} · ${item.size}"
            val rowHeight = 31f
            if (index % 2 == 0) {
                paint.color = pale
                canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, paint)
            }
            drawText(canvas, paint, itemLabel.take(45), margin + 12f, y + 20f, 10f, text, false)
            drawText(canvas, paint, item.quantity.toString(), pageWidth - 190f, y + 20f, 10f, text, false, true)
            val tariff = item.tariffCents
            drawText(canvas, paint, centsToDollarText(tariff), pageWidth - 112f, y + 20f, 10f, text, false, true)
            drawText(canvas, paint, centsToDollarText(tariff * item.quantity), pageWidth - margin - 10f, y + 20f, 10f, text, false, true)
            y += rowHeight
        }
        paint.color = border
        paint.strokeWidth = 1f
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        if (snapshot.mode.hours == 12) {
            drawText(canvas, paint, "Modalidad: 12 horas.", margin, y + 17f, 8.5f, muted)
            y += 31f
        } else {
            y += 16f
        }

        val summaryTop = y
        val summaryHeight = 126f
        val summaryWidth = 235f
        val paymentWidth = contentWidth - summaryWidth - 12f
        // Etiqueta inteligente: el recuadro refleja si este pago deja el alquiler saldado.
        // snapshot.rentalDepositCents ya trae el abono POSTERIOR al recibo (lo fija el repositorio al emitir).
        val paidInFull = snapshot.rentalTotalCents > 0L && snapshot.rentalDepositCents >= snapshot.rentalTotalCents
        val boxLabel = when {
            paidInFull && snapshot.amountCents >= snapshot.rentalTotalCents -> "ALQUILER PAGADO"
            paidInFull -> "MONTO CANCELADO · ALQUILER SALDADO"
            else -> "MONTO A CANCELAR"
        }
        val amountColor = if (paidInFull) android.graphics.Color.rgb(134, 239, 172) else android.graphics.Color.WHITE
        paint.color = darkBlue
        canvas.drawRoundRect(RectF(margin, summaryTop, margin + paymentWidth, summaryTop + summaryHeight), 7f, 7f, paint)
        drawText(canvas, paint, boxLabel, margin + 15f, summaryTop + 27f, 9f, lightBlue, true)
        drawText(canvas, paint, centsToDollarText(snapshot.amountCents), margin + 15f, summaryTop + 65f, 20f, amountColor, true)
        val bs = centsToBolivarText(snapshot.amountCents, snapshot.exchangeRate)
        if (bs.isNotBlank()) {
            drawText(canvas, paint, "$bs · ${exchangeRateText(snapshot.exchangeRate)}", margin + 15f, summaryTop + 95f, 9f, lightGray)
        }
        paint.color = pale
        canvas.drawRoundRect(RectF(pageWidth - margin - summaryWidth, summaryTop, pageWidth - margin, summaryTop + summaryHeight), 7f, 7f, paint)
        val rightX = pageWidth - margin - 16f
        drawAmountRow(canvas, paint, "Total del alquiler", centsToDollarText(snapshot.rentalTotalCents), rightX, summaryTop + 30f, text, false)
        drawAmountRow(canvas, paint, "Abono recibido", centsToDollarText(snapshot.rentalDepositCents), rightX, summaryTop + 58f, muted, false)
        val balance = (snapshot.rentalTotalCents - snapshot.rentalDepositCents).coerceAtLeast(0L)
        drawAmountRow(canvas, paint, "Pendiente", centsToDollarText(balance), rightX, summaryTop + 93f, if (balance > 0) android.graphics.Color.rgb(190, 65, 30) else android.graphics.Color.rgb(22, 130, 90), true)
        y = summaryTop + summaryHeight + 25f

        // Pie de página profesional, organizado y centrado
        val footerLineY = 760f
        paint.color = border
        paint.strokeWidth = 1f
        canvas.drawLine(margin, footerLineY, pageWidth - margin, footerLineY, paint)

        val bizName = snapshot.businessName.ifBlank { "EL SPOT TOLDOS" }
        val bizMeta = buildList {
            if (snapshot.businessRif.isNotBlank()) add("RIF: ${snapshot.businessRif}")
            if (snapshot.businessPhone.isNotBlank()) add("Tel: ${snapshot.businessPhone}")
        }
        val headerText = if (bizMeta.isEmpty()) bizName else "$bizName · ${bizMeta.joinToString(" · ")}"
        drawText(canvas, paint, headerText, pageWidth / 2f, 782f, 9.5f, text, bold = true, center = true)
        drawText(canvas, paint, "Comprobante digital de servicio · Generado por el sistema", pageWidth / 2f, 799f, 8f, muted, center = true)
        drawText(canvas, paint, "¡Gracias por su preferencia y confianza!", pageWidth / 2f, 815f, 9f, primary, bold = true, center = true)

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun share(snapshot: ReceiptSnapshot, chooserTitle: String = "Compartir recibo") {
        val file = create(snapshot)
        val uri = FileProvider.getUriForFile(context, "com.elspot.toldos.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, professionalMessage(snapshot))
            putExtra(Intent.EXTRA_TITLE, "Recibo ${snapshot.folio}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchChooser(intent, chooserTitle)
    }

    fun shareWhatsApp(snapshot: ReceiptSnapshot) {
        val file = create(snapshot)
        val uri = FileProvider.getUriForFile(context, "com.elspot.toldos.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, professionalMessage(snapshot))
            putExtra(Intent.EXTRA_TITLE, "Recibo ${snapshot.folio}")
        }
        val packageName = listOf("com.whatsapp", "com.whatsapp.w4b")
            .firstOrNull { context.packageManager.getLaunchIntentForPackage(it) != null }
        if (packageName == null) {
            share(snapshot, "WhatsApp no está instalado · compartir recibo")
        } else {
            intent.setPackage(packageName)
            try {
                if (intent.resolveActivity(context.packageManager) == null) share(snapshot, "Compartir recibo")
                else context.startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                share(snapshot, "Compartir recibo")
            }
        }
    }

    fun shareText(snapshot: ReceiptSnapshot) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, professionalMessage(snapshot))
        }
        launchChooser(intent, "Enviar resumen del recibo")
    }

    private fun launchChooser(intent: Intent, title: String) {
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun drawHeader(canvas: Canvas, paint: Paint, snapshot: ReceiptSnapshot): Float {
        paint.color = headerDark
        canvas.drawRect(0f, 0f, pageWidth, 112f, paint)
        val logo = loadPdfLogo()
        if (logo != null) {
            val logoWidth = 92f
            val logoHeight = 98f
            canvas.drawBitmap(logo, null, RectF((pageWidth - logoWidth) / 2f, 7f, (pageWidth + logoWidth) / 2f, 7f + logoHeight), paint)
        }
        drawText(canvas, paint, "RECIBO N° ${snapshot.folio}", pageWidth - margin, 31f, 15f, text, true, true)
        drawText(canvas, paint, "Emitido el ${formatDateTime(snapshot.emittedAt)}", pageWidth - margin, 53f, 9.5f, muted, false, true)
        // Sello del encabezado coherente con el estado del alquiler tras este recibo.
        val headerStatus = if (snapshot.rentalTotalCents > 0L && snapshot.rentalDepositCents >= snapshot.rentalTotalCents) ReceiptPaymentStatus.PAID else snapshot.paymentStatus
        val stateColor = if (headerStatus == ReceiptPaymentStatus.PAID) android.graphics.Color.rgb(22, 130, 90) else android.graphics.Color.rgb(180, 115, 10)
        drawText(canvas, paint, headerStatus.label.uppercase(), pageWidth - margin, 77f, 11f, stateColor, true, true)
        return 135f
    }

    private fun buildClientFields(snapshot: ReceiptSnapshot): List<Pair<String, String>> = buildList {
        add("NOMBRE" to capitalizeWords(snapshot.clientName).ifBlank { "—" })
        if (snapshot.clientDocument.isNotBlank()) add("DOCUMENTO" to snapshot.clientDocument)
        if (snapshot.clientPhone.isNotBlank()) add("TELÉFONO" to snapshot.clientPhone)
        if (snapshot.clientAddress.isNotBlank()) add("DIRECCIÓN" to snapshot.clientAddress)
    }

    private fun buildRentalFields(snapshot: ReceiptSnapshot): List<Pair<String, String>> = buildList {
        add("FOLIO DE ALQUILER" to snapshot.rentalFolio)
        add("MODALIDAD" to snapshot.mode.label)
        val eventAddressValue = when {
            snapshot.eventAddress.isNotBlank() -> snapshot.eventAddress
            snapshot.eventReference.isNotBlank() -> snapshot.eventReference
            else -> ""
        }
        if (eventAddressValue.isNotBlank()) add("DIRECCIÓN DEL EVENTO" to eventAddressValue)
        if (snapshot.eventAddress.isNotBlank() && snapshot.eventReference.isNotBlank()) {
            add("REFERENCIA" to snapshot.eventReference)
        }
        if (snapshot.latitude != null && snapshot.longitude != null) {
            add("UBICACIÓN GPS" to "${snapshot.latitude}, ${snapshot.longitude}")
        }
    }

    private fun wrapText(paint: Paint, text: String, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun measureCardHeight(paint: Paint, fields: List<Pair<String, String>>, contentWidth: Float): Float {
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        var total = 25f + 29f
        for ((_, value) in fields) {
            val lines = wrapText(paint, value.replace("\n", " ").trim().ifBlank { "—" }, contentWidth)
            val lineCount = lines.size.coerceIn(1, 2)
            total += 14f + (lineCount * 13f) + 4f
        }
        return total + 10f
    }

    private fun drawCardSection(canvas: Canvas, paint: Paint, title: String, fields: List<Pair<String, String>>, x: Float, y: Float, width: Float) {
        var current = y + 25f
        drawText(canvas, paint, title, x + 14f, current, 9f, primary, true)
        paint.color = border
        paint.strokeWidth = 1f
        canvas.drawLine(x + 14f, current + 9f, x + width - 14f, current + 9f, paint)
        current += 29f
        for ((label, value) in fields) {
            current = drawCardField(canvas, paint, label, value, x + 14f, current, width - 28f)
        }
    }

    private fun drawCard(canvas: Canvas, paint: Paint, x: Float, y: Float, width: Float, height: Float) {
        paint.color = pale
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 7f, 7f, paint)
        paint.color = border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 7f, 7f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawCardField(canvas: Canvas, paint: Paint, label: String, value: String, x: Float, y: Float, width: Float): Float {
        drawText(canvas, paint, label, x, y, 7f, muted, true)
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        val lines = wrapText(paint, value.replace("\n", " ").trim().ifBlank { "—" }, width)
        val visibleLines = lines.take(2)
        visibleLines.forEachIndexed { index, line ->
            drawText(canvas, paint, line, x, y + 14f + (index * 13f), 9.5f, text)
        }
        return y + 14f + (visibleLines.size * 13f) + 4f
    }

    private fun drawAmountRow(canvas: Canvas, paint: Paint, label: String, value: String, right: Float, y: Float, color: Int, bold: Boolean) {
        drawText(canvas, paint, label, right - 157f, y, 10f, if (bold) color else muted, bold)
        drawText(canvas, paint, value, right, y, 10f, color, bold, true)
    }

    private fun drawText(canvas: Canvas, paint: Paint, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, alignRight: Boolean = false, center: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        paint.textSize = size
        paint.textAlign = when {
            center -> Paint.Align.CENTER
            alignRight -> Paint.Align.RIGHT
            else -> Paint.Align.LEFT
        }
        canvas.drawText(value, x, y, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun loadPdfLogo(): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, com.elspot.toldos.R.drawable.logo_pdf) ?: return null
        val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 120, 120)
        drawable.draw(logoCanvas)
        return bitmap
    }

    private fun professionalMessage(snapshot: ReceiptSnapshot): String {
        val bs = centsToBolivarText(snapshot.amountCents, snapshot.exchangeRate)
        return buildString {
            appendLine(capitalizeWords(snapshot.businessName.ifBlank { "EL SPOT" }))
            appendLine("Recibo N° ${snapshot.folio}")
            val settled = snapshot.rentalTotalCents > 0L && snapshot.rentalDepositCents >= snapshot.rentalTotalCents
            val estadoTexto = (if (settled) ReceiptPaymentStatus.PAID else snapshot.paymentStatus).label.uppercase()
            appendLine("Estado: $estadoTexto")
            appendLine("Hola ${capitalizeWords(snapshot.clientName)},")
            appendLine("Adjuntamos el recibo correspondiente a tu alquiler de toldo.")
            appendLine("Modalidad: ${snapshot.mode.label}")
            appendLine("Monto: ${centsToDollarText(snapshot.amountCents)}")
            if (bs.isNotBlank()) appendLine("Equivalente: $bs")
            if (snapshot.eventAddress.isNotBlank()) appendLine("Dirección: ${snapshot.eventAddress}")
            if (snapshot.eventReference.isNotBlank()) appendLine("Referencia: ${snapshot.eventReference}")
            if (snapshot.latitude != null && snapshot.longitude != null) appendLine("Ubicación: https://maps.google.com/?q=${snapshot.latitude},${snapshot.longitude}")
            appendLine("Gracias por preferirnos.")
        }
    }
}
