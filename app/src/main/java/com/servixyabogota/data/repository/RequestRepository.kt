package com.servixyabogota.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.servixyabogota.data.model.Propuesta
import com.servixyabogota.data.model.Solicitud
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SolicitudRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference

    suspend fun crearSolicitud(
        solicitud: Solicitud,
        urisArchivos: List<Uri>,
        context: Context
    ): Result<Unit> {
        return try {
            val docRef = db.collection("solicitudes").document()
            val urlsFinales = mutableListOf<String>()

            // 1. Subir archivos a Firebase Storage si se adjuntaron
            urisArchivos.forEachIndexed { index, uri ->
                val ref = storageRef.child("solicitudes/${solicitud.clienteId}/${docRef.id}/media_${System.currentTimeMillis()}_$index")
                ref.putFile(uri).await()
                val urlDescarga = ref.downloadUrl.await().toString()
                urlsFinales.add(urlDescarga)
            }

            // 2. Asignar el ID generado y la lista de URLs públicas obtenidas
            val solicitudFinal = solicitud.copy(
                id = docRef.id,
                archivosUrls = urlsFinales
            )

            // 3. Guardar el documento completo en Firestore
            docRef.set(solicitudFinal).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerMisSolicitudesCliente(clienteId: String): Flow<List<Solicitud>> = callbackFlow {
        val listener = db.collection("solicitudes")
            .whereEqualTo("clienteId", clienteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val categoriasNormalizadas = misCategorias.map { normalizarTexto(it) }
                val localidadesNormalizadas = misLocalidades.map { normalizarTexto(it) }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
                }?.filter { solicitud ->
                    // 1. Estado abierto/pendiente
                    val estado = solicitud.estado.toString().uppercase().trim()
                    val esAbierta = estado.isBlank() || estado == "PENDIENTE" || estado == "ABIERTA"

                    // 2. Coincidencia de Categoría (flexible)
                    val catSolicitudNorm = normalizarTexto(solicitud.categoria)
                    val coincideCategoria = misCategorias.isEmpty() || categoriasNormalizadas.any { catPrestador ->
                        catSolicitudNorm.contains(catPrestador) || catPrestador.contains(catSolicitudNorm)
                    }

                    // 3. Coincidencia de Localidad
                    val locSolicitudNorm = normalizarTexto(solicitud.localidad)
                    val coincideLocalidad = misLocalidades.isEmpty() || localidadesNormalizadas.any { locPrestador ->
                        locSolicitudNorm.contains(locPrestador) || locPrestador.contains(locSolicitudNorm)
                    }

                    esAbierta && coincideCategoria && coincideLocalidad
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
        archivosUris: List<Uri>
    ): Result<Unit> {
        return try {
            val urlsFinales = mutableListOf<String>()

            archivosUris.forEachIndexed { index, uri ->
                val uriString = uri.toString()
                if (uri.scheme == "http" || uri.scheme == "https" || uriString.startsWith("http")) {
                    urlsFinales.add(uriString)
                } else {
                    val ref = storageRef.child("solicitudes/$clienteId/$solicitudId/media_${System.currentTimeMillis()}_$index")
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