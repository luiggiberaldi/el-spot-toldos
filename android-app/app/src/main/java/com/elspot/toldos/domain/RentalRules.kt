package com.elspot.toldos.domain

import com.elspot.toldos.data.RentalDraft
import com.elspot.toldos.data.RentalMode
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.data.ToldoEntity

/** Tarifa efectiva de una línea de alquiler. */
fun effectiveTariffCents(baseCents: Long, mode: RentalMode = RentalMode.H24): Long = baseCents

/** Total calculado desde las líneas del alquiler. Cada línea ya contiene la tarifa unitaria correspondiente a la modalidad seleccionada. */
fun calculateRentalTotal(items: List<com.elspot.toldos.data.RentalItemDraft>, mode: RentalMode = RentalMode.H24): Long {
    return items.sumOf { it.tariffCents * it.quantity.toLong() }
}

/**
 * Detecta la firma del "doble 50 %" que v1.0.4 y anteriores persistían en alquileres 12 h:
 * `montoTotalCents = round(Σ líneas / 2)`. Al ser la suma siempre el valor correcto, esta
 * firma solo puede provenir de ese error (el total nunca se edita a mano).
 *
 * Espejo Kotlin de la migración `MIGRATION_4_5` (AppDatabase.kt); mantener ambos en sincronía.
 * Se usa al restaurar respaldos creados con versiones viejas, donde la migración no corre
 * porque la base se crea ya en la versión actual.
 */
fun totalH12ConDobleMitad(modalidad: String, storedTotalCents: Long, linesTotalCents: Long): Boolean =
    RentalMode.from(modalidad) == RentalMode.H12 && storedTotalCents == (linesTotalCents + 1) / 2

/**
 * Devuelve el primer error de un alquiler o null cuando el borrador es válido.
 * La función no accede a Android ni a Room, por lo que puede probarse en JVM.
 */
fun validateRentalDraft(
    draft: RentalDraft,
    clientExists: Boolean,
    tentsById: Map<String, ToldoEntity>,
    occupiedUnitsByTent: Map<String, Int>
): String? {
    if (!clientExists) return "El cliente seleccionado ya no existe."
    if (draft.items.isEmpty()) return "Agrega al menos un toldo."
    if ((draft.latitude == null) != (draft.longitude == null)) {
        return "La ubicación GPS debe incluir latitud y longitud."
    }
    if (draft.address.isBlank() && !(draft.latitude != null && draft.longitude != null)) {
        return "Indica la dirección o captura la ubicación GPS."
    }
    if (draft.latitude != null && draft.latitude !in -90.0..90.0) {
        return "La latitud GPS no es válida."
    }
    if (draft.longitude != null && draft.longitude !in -180.0..180.0) {
        return "La longitud GPS no es válida."
    }
    if (draft.startAt <= 0L) return "La fecha de inicio no es válida."
    if (draft.items.any { it.tentId.isBlank() || it.quantity <= 0 || it.tariffCents < 0L }) {
        return "Revisa las líneas del alquiler."
    }
    if (draft.items.map { it.tentId }.distinct().size != draft.items.size) {
        return "No repitas el mismo toldo en varias líneas."
    }
    if (draft.items.any { it.tentId !in tentsById }) {
        return "Uno de los toldos seleccionados ya no existe."
    }
    if (draft.items.any { it.quantity > tentsById.getValue(it.tentId).unidades.coerceAtLeast(1) }) {
        return "La cantidad solicitada supera las unidades registradas del toldo."
    }
    val calculatedTotal = calculateRentalTotal(draft.items, draft.mode)
    if (draft.depositCents !in 0L..calculatedTotal) {
        return "El abono no puede superar el total."
    }
    val occupiesInventory = draft.status == RentalStatus.ACTIVE || draft.status == RentalStatus.DELIVERED
    if (occupiesInventory) {
        val blockedByManualStatus = draft.items.any {
            TentStatus.from(tentsById.getValue(it.tentId).estado) in setOf(TentStatus.REPAIR, TentStatus.RETIRED)
        }
        if (blockedByManualStatus) return "No puedes alquilar un toldo en reparación o retirado."
        val exceedsAvailability = draft.items.any { item ->
            val tent = tentsById.getValue(item.tentId)
            item.quantity + occupiedUnitsByTent.getOrDefault(item.tentId, 0) > tent.unidades.coerceAtLeast(1)
        }
        if (exceedsAvailability) {
            return "La cantidad solicitada supera las unidades disponibles del inventario."
        }
    }
    return null
}
