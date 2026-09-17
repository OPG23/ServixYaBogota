package com.servixyabogota.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.servixyabogota.data.model.EstadoPropuesta
import com.servixyabogota.data.model.EstadoSolicitud
import com.servixyabogota.data.model.Propuesta
import com.servixyabogota.data.model.Solicitud
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SolicitudRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val solicitudesCollection = firestore.collection("solicitudes")

    // =========================================================================
    // 1. PUBLICAR SOLICITUD (Subir archivos + Firestore)
    // =========================================================================
    suspend fun crearSolicitud(
        solicitud: Solicitud,
        urisArchivos: List<Uri>,
        context: Context
    ): Result<String> {
        return try {
            val solicitudId = solicitudesCollection.document().id
            val urlsDescarga = mutableListOf<String>()

            // Subir archivos a Firebase Storage si existen
            for ((index, uri) in urisArchivos.withIndex()) {
                val extension = getExtensionFromUri(context, uri)
                val fileName = "archivo_${index}_${System.currentTimeMillis()}.$extension"
                val storageRef = storage.reference
                    .child("solicitudes/${solicitud.clienteId}/$solicitudId/$fileName")

                storageRef.putFile(uri).await()
                val url = storageRef.downloadUrl.await().toString()
                urlsDescarga.add(url)
            }

            // Crear el objeto final de Solicitud con las URLs obtenidas
            val nuevaSolicitud = solicitud.copy(
                id = solicitudId,
                archivosUrls = urlsDescarga
            )

            // Guardar en Firestore
            solicitudesCollection.document(solicitudId).set(nuevaSolicitud).await()

            Result.success(solicitudId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. CONSULTA PARA CLIENTE ("Mis Solicitudes" en tiempo real)
    // =========================================================================
    fun obtenerMisSolicitudesCliente(clienteId: String): Flow<List<Solicitud>> = callbackFlow {
        val listener = solicitudesCollection
            .whereEqualTo("clienteId", clienteId)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Solicitud::class.java) ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // 3. CONSULTA PARA PRESTADOR ("Solicitudes Disponibles" en tiempo real)
    // =========================================================================
    fun obtenerSolicitudesDisponibles(
        categoriasPrestador: List<String>,
        localidadesFiltro: List<String> = emptyList()
    ): Flow<List<Solicitud>> = callbackFlow {
        if (categoriasPrestador.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }

        // Firestore permite un máximo de 30 elementos en 'whereIn'
        val categoriasProcesadas = categoriasPrestador.take(30)

        var query: Query = solicitudesCollection
            .whereIn("categoria", categoriasProcesadas)
            .whereEqualTo("estado", EstadoSolicitud.PENDIENTE)

        // Filtro opcional por localidad
        if (localidadesFiltro.isNotEmpty()) {
            val localidadesProcesadas = localidadesFiltro.take(30)
            query = query.whereIn("localidad", localidadesProcesadas)
        }

        val listener = query.orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Solicitud::class.java) ?: emptyList()
                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    // =========================================================================
    // 4. CREAR PROPUESTA (Prestador cotiza la solicitud)
    // =========================================================================
    suspend fun enviarPropuesta(solicitudId: String, propuesta: Propuesta): Result<Boolean> {
        return try {
            val solicitudRef = solicitudesCollection.document(solicitudId)
            val propuestaRef = solicitudRef.collection("propuestas").document()

            firestore.runTransaction { transaction ->
                val propuestaFinal = propuesta.copy(
                    id = propuestaRef.id,
                    solicitudId = solicitudId
                )

                // 1. Guardar la propuesta en la subcolección
                transaction.set(propuestaRef, propuestaFinal)

                // 2. Incrementar el contador de propuestas en la solicitud
                transaction.update(solicitudRef, "cantidadPropuestas", FieldValue.increment(1))
                transaction.update(solicitudRef, "fechaActualizacion", FieldValue.serverTimestamp())
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. CONSULTAR PROPUESTAS DE UNA SOLICITUD (Para el Cliente)
    // =========================================================================
    fun obtenerPropuestasDeSolicitud(solicitudId: String): Flow<List<Propuesta>> = callbackFlow {
        val listener = solicitudesCollection.document(solicitudId)
            .collection("propuestas")
            .orderBy("fechaPropuesta", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Propuesta::class.java) ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // 6. ACEPTAR PROPUESTA (Cliente selecciona a un prestador)
    // =========================================================================
    suspend fun aceptarPropuesta(
        solicitudId: String,
        propuestaId: String,
        prestadorId: String
    ): Result<Boolean> {
        return try {
            val solicitudRef = solicitudesCollection.document(solicitudId)
            val propuestaRef = solicitudRef.collection("propuestas").document(propuestaId)

            firestore.runTransaction { transaction ->
                // Actualizar estado de la solicitud
                transaction.update(solicitudRef, "estado", EstadoSolicitud.EN_PROCESO)
                transaction.update(solicitudRef, "prestadorIdAsignado", prestadorId)
                transaction.update(solicitudRef, "fechaActualizacion", FieldValue.serverTimestamp())

                // Actualizar estado de la propuesta a ACEPTADA
                transaction.update(propuestaRef, "estado", EstadoPropuesta.ACEPTADA)
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getExtensionFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
    }
}