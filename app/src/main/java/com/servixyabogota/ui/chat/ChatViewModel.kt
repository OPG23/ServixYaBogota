package com.servixyabogota.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

// ==========================================
// MODELOS DE DATOS DEL CHAT
// ==========================================
data class MensajeChat(
    val id: String = "",
    val emisorId: String = "",
    val texto: String = "",
    val fechaEnvio: Date = Date(),
    val mediaUrl: String? = null,
    val imagenUrl: String = "",
    val esVideo: Boolean = false,

    // Nuevos campos
    val esOferta: Boolean = false,
    val montoOferta: Double = 0.0,
    val estadoOferta: String = "PENDIENTE",

    // Aliases para compatibilidad con código anterior
    val esPropuesta: Boolean = esOferta,
    val montoPropuesta: Double = montoOferta,
    val leido: Boolean = false
)

data class ChatUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val propuestaMonto: Double = 0.0,
    val estadoPropuesta: String = "SIN_PROPUESTA", // SIN_PROPUESTA, PENDIENTE, ACEPTADA, RECHAZADA
    val estadoSolicitud: String = "", // PENDIENTE, EN_PROCESO, COMPLETADO, etc.
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

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var documentoListener: ListenerRegistration? = null
    private var mensajesListener: ListenerRegistration? = null

    private var chatIdActual: String = ""
    private var currentUserIdActual: String = ""
    private var esChatDirecto: Boolean = false

    /**
     * Inicializa los escuchadores en tiempo real de Firestore.
     */
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

    // 1. ESCUCHAR SOLICITUD DE SERVICIO (CHAT VINCULADO A SOLICITUD)
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

    // 2. ESCUCHAR CHAT DIRECTO ENTRE USUARIOS
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

                    val idContraparte = if (esCliente) prestadorId else clienteId
                    val rolContraparte = if (esCliente) "Prestador" else "Cliente"

                    _uiState.value = _uiState.value.copy(
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

    // 3. CARGAR PERFIL DE LA CONTRAPARTE (NOMBRE Y FOTO)
    private fun cargarDatosContraparte(userId: String) {
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombre = doc.getString("nombre") ?: doc.getString("nombreCompleto") ?: "Usuario"
                    val foto = doc.getString("fotoUrl") ?: doc.getString("fotoPerfil") ?: ""
                    _uiState.value = _uiState.value.copy(
                        nombreContraparte = nombre,
                        fotoContraparte = foto
                    )
                }
            }
    }

    // 4. LISTENER DE MENSAJES EN TIEMPO REAL
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
                            leido = doc.getBoolean("leido") ?: false
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        mensajes = listaMensajes,
                        isLoading = false
                    )
                }
            }
    }

    // 5. ENVIAR MENSAJE DE TEXTO O MULTIMEDIA
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
            "estadoOferta" to "PENDIENTE"
        )

        val textoResumen = when {
            texto.isNotBlank() -> texto.trim()
            esVideo -> "📹 Video"
            else -> "📷 Imagen"
        }

        db.collection(coleccionPadre)
            .document(chatIdActual)
            .collection("mensajes")
            .add(nuevoMensaje)
            .addOnSuccessListener {
                val datosUltimoMensaje = mapOf(
                    "ultimoMensaje" to textoResumen,
                    "fechaUltimoMensaje" to Timestamp.now(),
                    "ultimoEmisorId" to currentUserIdActual
                )

                db.collection(coleccionPadre)
                    .document(chatIdActual)
                    .set(datosUltimoMensaje, SetOptions.merge())
            }
    }

    // 6. ENVIAR MULTIMEDIA (FOTO O VIDEO)
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

    // 7. ENVIAR OFERTA / COTIZACIÓN DE SERVICIO (PRESTADOR)
    fun enviarOferta(monto: Double, descripcion: String) {
        if (chatIdActual.isBlank() || currentUserIdActual.isBlank() || monto <= 0) return

        // Bloqueo de seguridad adicional en el ViewModel
        if (_uiState.value.estadoPropuesta == "ACEPTADA") return

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
            "estadoOferta" to "PENDIENTE"
        )

        val batch = db.batch()
        batch.set(refMensaje, mensajeOferta)

        val datosUltimoMensaje = mutableMapOf<String, Any>(
            "ultimoMensaje" to "Oferta de servicio: $$monto",
            "fechaUltimoMensaje" to Timestamp.now(),
            "ultimoEmisorId" to currentUserIdActual
        )

        if (!esChatDirecto) {
            datosUltimoMensaje["propuestaMonto"] = monto
            datosUltimoMensaje["estadoPropuesta"] = "PENDIENTE"
        }

        batch.set(refPadre, datosUltimoMensaje, SetOptions.merge())
        batch.commit()
    }

    // 8. ACEPTAR O RECHAZAR OFERTA (CLIENTE)
    fun responderOferta(mensajeId: String, aceptada: Boolean) {
        if (chatIdActual.isBlank() || mensajeId.isBlank()) return

        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"
        val nuevoEstado = if (aceptada) "ACEPTADA" else "RECHAZADA"

        val refPadre = db.collection(coleccionPadre).document(chatIdActual)
        val refMensaje = refPadre.collection("mensajes").document(mensajeId)

        val batch = db.batch()

        batch.update(refMensaje, "estadoOferta", nuevoEstado)

        if (!esChatDirecto) {
            val actualizacionesSolicitud = mutableMapOf<String, Any>(
                "estadoPropuesta" to nuevoEstado
            )
            if (aceptada) {
                actualizacionesSolicitud["estado"] = "EN_PROCESO"

                val emisorOferta = _uiState.value.mensajes.find { it.id == mensajeId }?.emisorId
                if (!emisorOferta.isNullOrBlank()) {
                    actualizacionesSolicitud["prestadorId"] = emisorOferta
                }
            }
            batch.update(refPadre, actualizacionesSolicitud)
        }

        batch.commit()
    }

    // 9. COMPLETAR SERVICIO (CLIENTE)
    fun completarServicio(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (chatIdActual.isBlank() || esChatDirecto) return

        db.collection("solicitudes").document(chatIdActual)
            .update("estado", "COMPLETADO")
            .addOnSuccessListener {
                enviarMensaje("🎉 El cliente ha marcado el servicio como COMPLETADO.")
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al completar el servicio")
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

    // FUNCIONES DE COMPATIBILIDAD CON CÓDIGO ANTERIOR
    fun aceptarPropuesta() {
        if (chatIdActual.isBlank() || esChatDirecto) return

        db.collection("solicitudes")
            .document(chatIdActual)
            .update(
                mapOf(
                    "estadoPropuesta" to "ACEPTADA",
                    "estado" to "EN_PROCESO"
                )
            )
    }

    fun rechazarPropuesta() {
        if (chatIdActual.isBlank() || esChatDirecto) return

        db.collection("solicitudes")
            .document(chatIdActual)
            .update("estadoPropuesta", "RECHAZADA")
    }
}