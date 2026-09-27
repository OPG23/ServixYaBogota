package com.servixyabogota.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Solicitud(
    val id: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val clienteFotoUrl: String = "",
    val categoria: String = "",             // Ej: "Plomería", "Electricidad"
    val detalleProblema: String = "",
    val nivelUrgencia: String = "Media",     // "Baja", "Media", "Urgente"
    val direccion: String = "",
    val localidad: String = "",             // Ej: "Usaquén", "Suba", "Chapinero"
    val archivosUrls: List<String> = emptyList(),
    val estado: String = EstadoSolicitud.PENDIENTE,
    val prestadorIdAsignado: String? = null,
    val cantidadPropuestas: Int = 0,
    val noLeidosCliente: Int = 0,
    val noLeidosPrestador: Int = 0,
    @ServerTimestamp
    val fechaCreacion: Date? = null,
    @ServerTimestamp
    val fechaActualizacion: Date? = null
) {
    val tieneMensajesNuevos: Boolean
        get() = noLeidosCliente > 0
}

object EstadoSolicitud {
    const val PENDIENTE = "PENDIENTE"
    const val EN_PROCESO = "EN_PROCESO"
    const val COMPLETADO = "COMPLETADO"
    const val CANCELADO = "CANCELADO"
}