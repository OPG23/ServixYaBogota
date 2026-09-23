package com.servixyabogota.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.servixyabogota.data.model.Propuesta
import com.servixyabogota.data.model.Solicitud
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SolicitudRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference

    // Función auxiliar para obtener la extensión del archivo según su Uri
    private fun obtenerExtension(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)?.let { mime ->
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        } ?: if (uri.toString().lowercase().contains("video")) "mp4" else "jpg"
    }

    suspend fun crearSolicitud(
        solicitud: Solicitud,
        uris: List<Uri>,
        context: Context
    ): Result<Boolean> = try {
        val urlsSubidas = mutableListOf<String>()

        for (uri in uris) {
            if (uri.toString().startsWith("http")) {
                urlsSubidas.add(uri.toString())
            } else {
                val extension = obtenerExtension(context, uri)
                val fileName = "solicitudes/${UUID.randomUUID()}.$extension"
                val ref = storageRef.child(fileName)
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                urlsSubidas.add(downloadUrl)
            }
        }

        val solicitudFinal = solicitud.copy(archivosUrls = urlsSubidas)
        db.collection("solicitudes").add(solicitudFinal).await()

        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun obtenerMisSolicitudesCliente(clienteId: String): Flow<List<Solicitud>> = callbackFlow {
        val listener = db.collection("solicitudes")
            .whereEqualTo("clienteId", clienteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("SolicitudRepository", "Error en listener mis solicitudes: ${error.message}")
                    close()
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    suspend fun actualizarSolicitud(
        solicitudId: String,
        datosActualizados: Map<String, Any>
    ): Result<Unit> {
        return try {
            db.collection("solicitudes")
                .document(solicitudId)
                .update(datosActualizados)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelarSolicitud(solicitudId: String): Result<Unit> {
        return try {
            db.collection("solicitudes")
                .document(solicitudId)
                .update("estado", "CANCELADA")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // FUNCIONES PARA PRESTADORES
    // ==========================================

    private fun normalizarTexto(texto: String): String {
        return texto.replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]"), "")
            .lowercase()
            .trim()
    }

    fun obtenerSolicitudesDisponibles(
        misCategorias: List<String>,
        misLocalidades: List<String>
    ): Flow<List<Solicitud>> = callbackFlow {
        val listener = db.collection("solicitudes")
            .whereIn("estado", listOf("PENDIENTE", "ABIERTA"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }

                val categoriasNormalizadas = misCategorias.map { normalizarTexto(it) }
                val localidadesNormalizadas = misLocalidades.map { normalizarTexto(it) }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
                }?.filter { solicitud ->
                    val catSolicitudNorm = normalizarTexto(solicitud.categoria)
                    val coincideCategoria = misCategorias.isEmpty() || categoriasNormalizadas.any { catPrestador ->
                        catSolicitudNorm.contains(catPrestador) || catPrestador.contains(catSolicitudNorm)
                    }

                    val locSolicitudNorm = normalizarTexto(solicitud.localidad)
                    val coincideLocalidad = misLocalidades.isEmpty() || localidadesNormalizadas.any { locPrestador ->
                        locSolicitudNorm.contains(locPrestador) || locPrestador.contains(locSolicitudNorm)
                    }

                    coincideCategoria && coincideLocalidad
                } ?: emptyList()

                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    suspend fun enviarPropuesta(
        solicitudId: String,
        propuesta: Propuesta
    ): Result<Unit> {
        return try {
            db.collection("solicitudes")
                .document(solicitudId)
                .collection("propuestas")
                .document(propuesta.prestadorId)
                .set(propuesta)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarSolicitudConArchivos(
        solicitudId: String,
        clienteId: String,
        datos: Map<String, Any>,
        archivosUris: List<Uri>,
        context: Context
    ): Result<Unit> {
        return try {
            val urlsFinales = mutableListOf<String>()

            archivosUris.forEachIndexed { index, uri ->
                val uriString = uri.toString()
                if (uri.scheme == "http" || uri.scheme == "https" || uriString.startsWith("http")) {
                    urlsFinales.add(uriString)
                } else {
                    val extension = obtenerExtension(context, uri)
                    val ref = storageRef.child("solicitudes/$clienteId/$solicitudId/media_${System.currentTimeMillis()}_$index.$extension")
                    ref.putFile(uri).await()
                    val urlDescarga = ref.downloadUrl.await().toString()
                    urlsFinales.add(urlDescarga)
                }
            }

            val datosCompletos = datos.toMutableMap().apply {
                put("archivosUrls", urlsFinales)
            }

            db.collection("solicitudes")
                .document(solicitudId)
                .update(datosCompletos)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}