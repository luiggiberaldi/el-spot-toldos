package com.elspot.toldos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val cedula: String,
    val telefono: String,
    val email: String,
    val direccion: String,
    val notas: String,
    val creadoEn: Long
)

@Entity(tableName = "toldos")
data class ToldoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val tamano: String,
    val tarifaCents: Long,
    val tarifa12hCents: Long? = null,
    val unidades: Int = 1,
    val estado: String,
    val notas: String,
    val creadoEn: Long
)

@Entity(tableName = "alquileres")
data class AlquilerEntity(
    @PrimaryKey val id: String,
    val folio: String,
    val clienteId: String,
    val modalidad: String,
    val inicio: Long,
    val devolucion: Long,
    val direccion: String,
    val latitud: Double?,
    val longitud: Double?,
    val montoTotalCents: Long,
    val abonoCents: Long,
    val estado: String,
    val notas: String,
    val creadoEn: Long,
    val actualizadoEn: Long
)

@Entity(tableName = "alquiler_items", primaryKeys = ["alquilerId", "linea"])
data class AlquilerItemEntity(
    val alquilerId: String,
    val linea: Int,
    val toldoId: String,
    val cantidad: Int,
    val tarifaCents: Long
)

@Entity(tableName = "recibos")
data class ReciboEntity(
    @PrimaryKey val id: String,
    val folio: String,
    val alquilerId: String,
    val emitidoEn: Long,
    val concepto: String,
    val montoCents: Long,
    val snapshotJson: String,
    /** Estado financiero congelado para el recibo. Recibos antiguos migran a POR_PAGAR. */
    val estadoPago: String = "POR_PAGAR"
)

@Entity(tableName = "bitacora")
data class BitacoraEntity(
    @PrimaryKey val id: String,
    val fecha: Long,
    val tipo: String,
    val entidad: String,
    val descripcion: String
)
