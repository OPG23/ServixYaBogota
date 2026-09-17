package com.servixyabogota.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val cedula: String = "",
    val profesion: String = "",
    val telefono: String = "",
    val rol: String = "cliente",
    val estadoVerificacion: String = "NO_ENVIADO", // "NO_ENVIADO", "PENDIENTE_VERIFICACION", "APROBADO", "RECHAZADO"
    val fotoUrl: String = "",
    val documentos: Map<String, String> = emptyMap(),
    val motivoRechazo: String = "",
    val justificacionRechazo: String = "",
    val documentosRechazados: List<String> = emptyList(),
    val fechaActualizacion: Long = System.currentTimeMillis(),
    val fechaEnvioDocumentos: Long = 0L,

    // CAMPOS PARA PRESTADOR APROBADO
    val localidades: List<String> = emptyList(),
    val categorias: List<String> = emptyList(),
    val portafolio: List<String> = emptyList(), // Lista de URLs descargables de las fotos
    val descripcion: String = ""
) {
    val nombreCompleto: String
        get() = if (apellido.isNotBlank()) "$nombre $apellido".trim() else nombre
}