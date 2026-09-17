package com.servixyabogota.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Propuesta(
    val id: String = "",
    val solicitudId: String = "",
    val prestadorId: String = "",
    val prestadorNombre: String = "",
    val prestadorFotoUrl: String = "",
    val calificacionPrestador: Double = 5.0,
    val precioEstimado: Double = 0.0,
    val mensaje: String = "",
    val estado: String = EstadoPropuesta.PENDIENTE,
    @ServerTimestamp
    val fechaPropuesta: Date? = null
)

object EstadoPropuesta {
    const val PENDIENTE = "PENDIENTE"
    const val ACEPTADA = "ACEPTADA"
    const val RECHAZADA = "RECHAZADA"
}