package com.servixyabogota.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SolicitudVerificacion(
    val userId: String = "",
    val nombre: String = "",
    val cedula: String = "",
    val profesion: String = "",
    val fotoUrl: String = "",
    val fechaSolicitud: String = "",
    val documentos: Map<String, String> = emptyMap()
)

class AdminRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val USERS_COLLECTION = "usuarios"
    }

    suspend fun obtenerSolicitudesPendientes(): Result<List<SolicitudVerificacion>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("estadoVerificacion", "PENDIENTE_VERIFICACION")
                .get()
                .await()

            val lista = snapshot.documents.map { doc ->
                @Suppress("UNCHECKED_CAST")
                val docsMap = doc.get("documentos") as? Map<String, String> ?: emptyMap()

                val nombre = doc.getString("nombre") ?: doc.getString("nombreCompleto") ?: "Usuario"
                val apellido = doc.getString("apellido") ?: ""
                val nombreCompleto = if (apellido.isNotEmpty()) "$nombre $apellido".trim() else nombre

                val timestamp = doc.getLong("fechaEnvioDocumentos") ?: System.currentTimeMillis()
                val fechaFormateada = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO")).format(Date(timestamp))

                SolicitudVerificacion(
                    userId = doc.id,
                    nombre = nombreCompleto,
                    cedula = doc.getString("cedula") ?: doc.getString("numDocumento") ?: "C.C. No registrada",
                    profesion = doc.getString("profesion") ?: doc.getString("oficio") ?: "Prestador",
                    fotoUrl = doc.getString("fotoUrl") ?: "",
                    fechaSolicitud = fechaFormateada,
                    documentos = docsMap
                )
            }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun aprobarUsuario(userId: String): Result<Boolean> {
        return try {
            firestore.collection(USERS_COLLECTION).document(userId)
                .set(mapOf("estadoVerificacion" to "APROBADO"), SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rechazarUsuario(
        userId: String,
        motivoPrincipal: String,
        justificacion: String
    ): Result<Boolean> {
        return try {
            val datosRechazo = mapOf(
                "estadoVerificacion" to "RECHAZADO",
                "motivoRechazo" to motivoPrincipal,
                "justificacionRechazo" to justificacion,
                "fechaRechazo" to System.currentTimeMillis()
            )

            firestore.collection(USERS_COLLECTION).document(userId)
                .set(datosRechazo, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}