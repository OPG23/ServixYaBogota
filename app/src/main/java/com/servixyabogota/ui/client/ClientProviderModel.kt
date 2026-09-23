package com.servixyabogota.ui.client

data class CategoriaItem(
    val nombre: String,
    val emoji: String
)

data class ClientProviderModel(
    val id: String,
    val nombre: String,
    val fotoUrl: String,
    val calificacion: Double,
    val totalResenas: Int,
    val categorias: List<String>,
    val disponibleHoy: Boolean = true,
    val verificado: Boolean = true,
    val descripcion: String = "",
    val experienciaAnos: Int = 1,
    val portafolioUrls: List<String> = emptyList(),
    val localidades: List<String> = emptyList() // <-- NUEVO CAMPO
)