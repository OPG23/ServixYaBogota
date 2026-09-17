package com.servixyabogota.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val USERS_COLLECTION = "usuarios"
    }

    suspend fun obtenerEstadoVerificacion(): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            val estado = snapshot.getString("estadoVerificacion") ?: "NO_ENVIADO"
            Result.success(estado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun subirArchivosYEnviarRevision(
        context: Context,
        documentos: Map<String, Uri>
    ): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val urlsCargadas = mutableMapOf<String, String>()

            for ((docType, uri) in documentos) {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val extension = if (mimeType.contains("pdf", ignoreCase = true)) "pdf" else "jpg"

                val ref = storage.reference.child("documentos/$userId/$docType.$extension")

                // Usamos putFile directamente con el Uri (optimizado y seguro en RAM)
                ref.putFile(uri).await()

                val url = ref.downloadUrl.await().toString()
                urlsCargadas[docType] = url
            }

            val updateData = mapOf(
                "documentos" to urlsCargadas,
                "estadoVerificacion" to "PENDIENTE_VERIFICACION",
                "fechaEnvioDocumentos" to System.currentTimeMillis()
            )

            firestore.collection(USERS_COLLECTION).document(userId)
                .set(updateData, SetOptions.merge())
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}