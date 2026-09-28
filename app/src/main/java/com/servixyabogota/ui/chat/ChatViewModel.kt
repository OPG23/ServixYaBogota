package com.servixyabogota.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.servixyabogota.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class MensajeChat(
    val id: String = "",
    val emisorId: String = "",
    val texto: String = "",
    val fechaEnvio: Date = Date(),
    val mediaUrl: String? = null,
    val imagenUrl: String = "",
    val esVideo: Boolean = false,
    val esOferta: Boolean = false,
    val montoOferta: Double = 0.0,
    val estadoOferta: String = "PENDIENTE",
    val esPropuesta: Boolean = esOferta,
    val montoPropuesta: Double = montoOferta,
    val leido: Boolean = false
)

data class ChatUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val propuestaMonto: Double = 0.0,
    val estadoPropuesta: String = "SIN_PROPUESTA",
    val estadoSolicitud: String = "",
    val clienteCalifico: Boolean = false,
    val prestadorCalifico: Boolean = false,
    val idContraparte: String = "",
    val nombreContraparte: String = "",
    val fotoContraparte: String = "",
    val rolContraparte: String = "",
    val esCliente: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val solicitudRepository = SolicitudRepository()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var documentoListener: ListenerRegistration? = null
    private var mensajesListener: ListenerRegistration? = null

    private var chatIdActual: String = ""
    private var currentUserIdActual: String = ""
    private var esChatDirecto: Boolean = false

    fun inicializarChat(chatId: String, currentUserId: String, esCliente: Boolean) {
        if (chatIdActual == chatId && currentUserIdActual == currentUserId && _uiState.value.mensajes.isNotEmpty()) {
            return
        }

        detenerListeners()

        this.chatIdActual = chatId
        this.currentUserIdActual = currentUserId
        this.esChatDirecto = chatId.startsWith("chat_") || chatId.startsWith("direct_")

        _uiState.value = ChatUiState(
            esCliente = esCliente,
            isLoading = true
        )

        if (esChatDirecto) {
            escucharChatDirecto(chatId, currentUserId, esCliente)
        } else {
            escucharSolicitudChat(chatId, currentUserId, esCliente)
        }

        escucharMensajesEnVivo(chatId)
    }

    private fun escucharSolicitudChat(solicitudId: String, currentUserId: String, esCliente: Boolean) {
        documentoListener = db.collection("solicitudes").document(solicitudId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(error = error.message, isLoading = false)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val propuestaMonto = snapshot.getDouble("propuestaMonto") ?: snapshot.getDouble("montoOferta") ?: 0.0
                    val estadoPropuesta = snapshot.getString("estadoPropuesta") ?: snapshot.getString("estadoOferta") ?: "SIN_PROPUESTA"
                    val estadoSolicitud = snapshot.getString("estado") ?: ""
                    val clienteCalifico = snapshot.getBoolean("clienteCalifico") ?: false
                    val prestadorCalifico = snapshot.getBoolean("prestadorCalifico") ?: false

                    val clienteId = snapshot.getString("clienteId") ?: ""
                    val prestadorId = snapshot.getString("prestadorId")
                        ?: snapshot.getString("idPrestador")
                        ?: snapshot.getString("prestadorSeleccionadoId")
                        ?: ""

                    val idContraparte = if (esCliente) prestadorId else clienteId
                    val rolContraparte = if (esCliente) "Prestador" else "Cliente"

                    _uiState.value = _uiState.value.copy(
                        propuestaMonto = propuestaMonto,
                        estadoPropuesta = estadoPropuesta,
                        estadoSolicitud = estadoSolicitud,
                        clienteCalifico = clienteCalifico,
                        prestadorCalifico = prestadorCalifico,
                        idContraparte = idContraparte,
                        rolContraparte = rolContraparte
                    )

                    if (idContraparte.isNotBlank()) {
                        cargarDatosContraparte(idContraparte)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    private fun escucharChatDirecto(chatId: String, currentUserId: String, esCliente: Boolean) {
        documentoListener = db.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(error = error.message, isLoading = false)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val clienteId = snapshot.getString("clienteId") ?: ""
                    val prestadorId = snapshot.getString("prestadorId") ?: ""

                    val propuestaMonto = snapshot.getDouble("propuestaMonto") ?: snapshot.getDouble("montoOferta") ?: 0.0
                    val estadoPropuesta = snapshot.getString("estadoPropuesta") ?: snapshot.getString("estadoOferta") ?: "SIN_PROPUESTA"
                    val estadoSolicitud = snapshot.getString("estado") ?: ""
                    val clienteCalifico = snapshot.getBoolean("clienteCalifico") ?: false
                    val prestadorCalifico = snapshot.getBoolean("prestadorCalifico") ?: false

                    val idContraparte = if (esCliente) prestadorId else clienteId
                    val rolContraparte = if (esCliente) "Prestador" else "Cliente"

                    _uiState.value = _uiState.value.copy(
                        propuestaMonto = propuestaMonto,
                        estadoPropuesta = estadoPropuesta,
                        estadoSolicitud = estadoSolicitud,
                        clienteCalifico = clienteCalifico,
                        prestadorCalifico = prestadorCalifico,
                        idContraparte = idContraparte,
                        rolContraparte = rolContraparte
                    )

                    if (idContraparte.isNotBlank()) {
                        cargarDatosContraparte(idContraparte)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    private fun cargarDatosContraparte(userId: String) {
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombre = doc.getString("nombre")?.trim() ?: ""
                    val apellido = doc.getString("apellido")?.trim() ?: ""
                    val nombreCompletoDoc = doc.getString("nombreCompleto")?.trim() ?: ""

                    val nombreBase = when {
                        nombre.isNotBlank() && apellido.isNotBlank() -> {
                            if (nombre.endsWith(apellido, ignoreCase = true)) {
                                nombre
                            } else {
                                "$nombre $apellido"
                            }
                        }
                        nombreCompletoDoc.isNotBlank() -> nombreCompletoDoc
                        nombre.isNotBlank() -> nombre
                        apellido.isNotBlank() -> apellido
                        else -> "Usuario"
                    }

                    val nombreFinal = nombreBase.split("\\s+".toRegex())
                        .distinct()
                        .joinToString(" ")

                    val foto = doc.getString("fotoUrl") ?: doc.getString("fotoPerfil") ?: ""

                    _uiState.value = _uiState.value.copy(
                        nombreContraparte = nombreFinal,
                        fotoContraparte = foto
                    )
                }
            }
    }

    private fun escucharMensajesEnVivo(chatId: String) {
        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"

        mensajesListener = db.collection(coleccionPadre)
            .document(chatId)
            .collection("mensajes")
            .orderBy("fechaEnvio", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(error = error.message, isLoading = false)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val mensajesSinLeerList = mutableListOf<String>()

                    val listaMensajes = snapshot.documents.mapNotNull { doc ->
                        val emisorId = doc.getString("emisorId") ?: ""
                        val texto = doc.getString("texto") ?: ""
                        val timestamp = doc.getTimestamp("fechaEnvio")
                        val fecha = timestamp?.toDate() ?: Date()
                        val mediaUrl = doc.getString("mediaUrl") ?: doc.getString("imagenUrl")
                        val esVideo = doc.getBoolean("esVideo") ?: false

                        val esOferta = doc.getBoolean("esOferta") ?: doc.getBoolean("esPropuesta") ?: false
                        val montoOferta = doc.getDouble("montoOferta") ?: doc.getDouble("montoPropuesta") ?: 0.0
                        val estadoOferta = doc.getString("estadoOferta") ?: "PENDIENTE"
                        val leido = doc.getBoolean("leido") ?: false

                        if (!leido && emisorId != currentUserIdActual && emisorId.isNotBlank()) {
                            mensajesSinLeerList.add(doc.id)
                        }

                        MensajeChat(
                            id = doc.id,
                            emisorId = emisorId,
                            texto = texto,
                            fechaEnvio = fecha,
                            imagenUrl = mediaUrl ?: "",
                            mediaUrl = mediaUrl,
                            esVideo = esVideo,
                            esOferta = esOferta,
                            montoOferta = montoOferta,
                            estadoOferta = estadoOferta,
                            esPropuesta = esOferta,
                            montoPropuesta = montoOferta,
                            leido = leido
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        mensajes = listaMensajes,
                        isLoading = false
                    )

                    if (mensajesSinLeerList.isNotEmpty()) {
                        marcarMensajesComoLeidos(mensajesSinLeerList)
                    }
                }
            }
    }

    private fun marcarMensajesComoLeidos(idsMensajes: List<String>) {
        if (chatIdActual.isBlank()) return

        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"
        val batch = db.batch()

        idsMensajes.forEach { msgId ->
            val refMsg = db.collection(coleccionPadre)
                .document(chatIdActual)
                .collection("mensajes")
                .document(msgId)
            batch.update(refMsg, "leido", true)
        }

        val campoNoLeidos = if (_uiState.value.esCliente) "noLeidosCliente" else "noLeidosPrestador"
        val refChat = db.collection(coleccionPadre).document(chatIdActual)
        batch.set(refChat, mapOf(campoNoLeidos to 0), SetOptions.merge())

        batch.commit()
    }

    fun enviarMensaje(
        texto: String,
        mediaUrl: String? = null,
        esVideo: Boolean = false
    ) {
        if (chatIdActual.isBlank() || currentUserIdActual.isBlank() || (texto.isBlank() && mediaUrl.isNullOrEmpty())) return

        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"

        val nuevoMensaje = hashMapOf(
            "emisorId" to currentUserIdActual,
            "texto" to texto.trim(),
            "fechaEnvio" to Timestamp.now(),
            "mediaUrl" to (mediaUrl ?: ""),
            "imagenUrl" to (mediaUrl ?: ""),
            "esVideo" to esVideo,
            "esOferta" to false,
            "montoOferta" to 0.0,
            "estadoOferta" to "PENDIENTE",
            "leido" to false
        )

        val textoResumen = when {
            texto.isNotBlank() -> texto.trim()
            esVideo -> "📹 Video"
            else -> "📷 Imagen"
        }

        val campoNoLeidosDestinatario = if (_uiState.value.esCliente) "noLeidosPrestador" else "noLeidosCliente"

        db.collection(coleccionPadre)
            .document(chatIdActual)
            .collection("mensajes")
            .add(nuevoMensaje)
            .addOnSuccessListener {
                val datosUltimoMensaje = mapOf(
                    "ultimoMensaje" to textoResumen,
                    "fechaUltimoMensaje" to Timestamp.now(),
                    "ultimoEmisorId" to currentUserIdActual,
                    campoNoLeidosDestinatario to FieldValue.increment(1)
                )

                db.collection(coleccionPadre)
                    .document(chatIdActual)
                    .set(datosUltimoMensaje, SetOptions.merge())
            }
    }

    fun enviarMensajeConMedia(texto: String, mediaUri: Uri, context: Context) {
        if (chatIdActual.isBlank() || currentUserIdActual.isBlank()) return

        val mimeType = context.contentResolver.getType(mediaUri) ?: "image/jpeg"
        val esVideo = mimeType.startsWith("video")

        _uiState.value = _uiState.value.copy(isLoading = true)

        val extension = if (esVideo) "mp4" else "jpg"
        val nombreArchivo = "${System.currentTimeMillis()}_media.$extension"

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("chat_media")
            .child(chatIdActual)
            .child(nombreArchivo)

        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        storageRef.putFile(mediaUri, metadata)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    enviarMensaje(
                        texto = texto,
                        mediaUrl = downloadUrl.toString(),
                        esVideo = esVideo
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
            .addOnFailureListener { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Error al subir archivo: ${error.message}",
                    isLoading = false
                )
            }
    }

    fun enviarOferta(monto: Double, descripcion: String) {
        if (chatIdActual.isBlank() || currentUserIdActual.isBlank() || monto <= 0) return

        if (_uiState.value.estadoPropuesta == "ACEPTADA" || _uiState.value.estadoPropuesta == "PENDIENTE") return

        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"
        val refPadre = db.collection(coleccionPadre).document(chatIdActual)
        val refMensaje = refPadre.collection("mensajes").document()

        val textoFormateado = if (descripcion.isBlank()) "Oferta de servicio enviada" else descripcion.trim()

        val mensajeOferta = hashMapOf(
            "id" to refMensaje.id,
            "emisorId" to currentUserIdActual,
            "texto" to textoFormateado,
            "fechaEnvio" to Timestamp.now(),
            "mediaUrl" to "",
            "imagenUrl" to "",
            "esVideo" to false,
            "esOferta" to true,
            "montoOferta" to monto,
            "estadoOferta" to "PENDIENTE",
            "leido" to false
        )

        val batch = db.batch()
        batch.set(refMensaje, mensajeOferta)

        val campoNoLeidosDestinatario = if (_uiState.value.esCliente) "noLeidosPrestador" else "noLeidosCliente"

        val datosUltimoMensaje = mutableMapOf<String, Any>(
            "ultimoMensaje" to "Oferta de servicio: $$monto",
            "fechaUltimoMensaje" to Timestamp.now(),
            "ultimoEmisorId" to currentUserIdActual,
            "propuestaMonto" to monto,
            "estadoPropuesta" to "PENDIENTE",
            campoNoLeidosDestinatario to FieldValue.increment(1)
        )

        batch.set(refPadre, datosUltimoMensaje, SetOptions.merge())
        batch.commit()
    }

    fun responderOferta(mensajeId: String, aceptada: Boolean) {
        if (chatIdActual.isBlank() || mensajeId.isBlank()) return

        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"
        val nuevoEstado = if (aceptada) "ACEPTADA" else "RECHAZADA"

        val refPadre = db.collection(coleccionPadre).document(chatIdActual)
        val refMensaje = refPadre.collection("mensajes").document(mensajeId)

        val batch = db.batch()

        batch.update(refMensaje, "estadoOferta", nuevoEstado)

        val actualizacionesPadre = mutableMapOf<String, Any>(
            "estadoPropuesta" to nuevoEstado
        )

        if (aceptada) {
            actualizacionesPadre["estado"] = "EN_PROCESO"

            val emisorOferta = _uiState.value.mensajes.find { it.id == mensajeId }?.emisorId
            if (!emisorOferta.isNullOrBlank()) {
                actualizacionesPadre["prestadorId"] = emisorOferta
            }
        }

        batch.set(refPadre, actualizacionesPadre, SetOptions.merge())
        batch.commit()
    }

    /**
     * Permite al cliente completar el servicio y calificar al prestador.
     */
    fun completarServicioYCalificar(
        calificacion: Int,
        comentario: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (chatIdActual.isBlank()) return

        val prestadorId = _uiState.value.idContraparte
        if (prestadorId.isBlank() || currentUserIdActual.isBlank()) {
            onError("No se pudo obtener la información de las partes")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        db.collection("usuarios").document(currentUserIdActual).get()
            .addOnSuccessListener { docCliente ->
                val nombreCliente = docCliente.getString("nombreCompleto")
                    ?: docCliente.getString("nombre")
                    ?: "Cliente ServixYa"

                viewModelScope.launch {
                    val result = solicitudRepository.completarServicioYCalificar(
                        solicitudId = chatIdActual,
                        clienteId = currentUserIdActual,
                        prestadorId = prestadorId,
                        calificacion = calificacion,
                        comentario = comentario,
                        clienteNombre = nombreCliente,
                        esChatDirecto = esChatDirecto
                    )

                    val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"
                    db.collection(coleccionPadre).document(chatIdActual)
                        .set(mapOf("clienteCalifico" to true), SetOptions.merge())

                    _uiState.value = _uiState.value.copy(isLoading = false, clienteCalifico = true)

                    result.fold(
                        onSuccess = {
                            enviarMensaje("🎉 El cliente ha completado el servicio y ha dejado una evaluación de $calificacion ★.")
                            onSuccess()
                        },
                        onFailure = { error ->
                            onError(error.message ?: "Error al completar y calificar el servicio")
                        }
                    )
                }
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onError("Error al obtener datos del cliente")
            }
    }

    /**
     * Permite al prestador calificar al cliente una vez el servicio esté completado.
     */
    fun calificarCliente(
        calificacion: Int,
        comentario: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (chatIdActual.isBlank()) return

        val clienteId = _uiState.value.idContraparte
        if (clienteId.isBlank() || currentUserIdActual.isBlank()) {
            onError("No se pudo obtener la información del cliente")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"
        val refPadre = db.collection(coleccionPadre).document(chatIdActual)

        db.collection("usuarios").document(currentUserIdActual).get()
            .addOnSuccessListener { docPrestador ->
                val nombrePrestador = docPrestador.getString("nombreCompleto")
                    ?: docPrestador.getString("nombre")
                    ?: "Prestador ServixYa"

                val reseñaMap = hashMapOf(
                    "autorId" to currentUserIdActual,
                    "autorNombre" to nombrePrestador,
                    "clienteId" to clienteId,
                    "calificacion" to calificacion,
                    "comentario" to comentario.trim(),
                    "fecha" to Timestamp.now(),
                    "solicitudId" to chatIdActual
                )

                val batch = db.batch()
                val refReseña = db.collection("usuarios")
                    .document(clienteId)
                    .collection("calificacionesRecibidas")
                    .document()

                batch.set(refReseña, reseñaMap)
                batch.set(refPadre, mapOf("prestadorCalifico" to true), SetOptions.merge())

                batch.commit()
                    .addOnSuccessListener {
                        _uiState.value = _uiState.value.copy(isLoading = false, prestadorCalifico = true)
                        enviarMensaje("⭐ El prestador ha dejado una calificación de $calificacion ★ para el cliente.")
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onError(error.message ?: "Error al guardar la calificación")
                    }
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onError("Error al obtener datos del prestador")
            }
    }

    private fun detenerListeners() {
        documentoListener?.remove()
        mensajesListener?.remove()
        documentoListener = null
        mensajesListener = null
    }

    override fun onCleared() {
        super.onCleared()
        detenerListeners()
    }
}