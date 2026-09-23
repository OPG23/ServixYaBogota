package com.servixyabogota.ui.chat

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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
    val imagenUrl: String = "",
    val esPropuesta: Boolean = false,
    val montoPropuesta: Double = 0.0,
    val leido: Boolean = false
)

data class ChatUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val propuestaMonto: Double = 0.0,
    val estadoPropuesta: String = "SIN_PROPUESTA", // SIN_PROPUESTA, PENDIENTE, ACEPTADA, RECHAZADA
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
        // Evita duplicar la suscripción si ya está activa la misma sesión de chat
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
                    val propuestaMonto = snapshot.getDouble("propuestaMonto") ?: 0.0
                    val estadoPropuesta = snapshot.getString("estadoPropuesta") ?: "SIN_PROPUESTA"

                    val clienteId = snapshot.getString("clienteId") ?: ""
                    val prestadorId = snapshot.getString("prestadorId") ?: ""

                    val idContraparte = if (esCliente) prestadorId else clienteId
                    val rolContraparte = if (esCliente) "Prestador" else "Cliente"

                    _uiState.value = _uiState.value.copy(
                        propuestaMonto = propuestaMonto,
                        estadoPropuesta = estadoPropuesta,
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

                    _uiState.value = _uiState.value.copy(rolContraparte = rolContraparte)

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
                        val imagenUrl = doc.getString("imagenUrl") ?: ""
                        val esPropuesta = doc.getBoolean("esPropuesta") ?: false
                        val montoPropuesta = doc.getDouble("montoPropuesta") ?: 0.0
                        val leido = doc.getBoolean("leido") ?: false

                        MensajeChat(
                            id = doc.id,
                            emisorId = emisorId,
                            texto = texto,
                            fechaEnvio = fecha,
                            imagenUrl = imagenUrl,
                            esPropuesta = esPropuesta,
                            montoPropuesta = montoPropuesta,
                            leido = leido
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        mensajes = listaMensajes,
                        isLoading = false
                    )
                }
            }
    }

    // 5. ENVIAR MENSAJE DE TEXTO (CORREGIDO)
    fun enviarMensaje(texto: String, imagenUrl: String? = null) {
        if (chatIdActual.isBlank() || currentUserIdActual.isBlank() || texto.isBlank()) return

        val coleccionPadre = if (esChatDirecto) "chats" else "solicitudes"

        val nuevoMensaje = hashMapOf(
            "emisorId" to currentUserIdActual,
            "texto" to texto.trim(),
            "fechaEnvio" to Timestamp.now(),
            "imagenUrl" to (imagenUrl ?: ""),
            "esPropuesta" to false,
            "montoPropuesta" to 0.0,
            "leido" to false
        )

        db.collection(coleccionPadre)
            .document(chatIdActual)
            .collection("mensajes")
            .add(nuevoMensaje)
            .addOnSuccessListener {
                // Se usa SetOptions.merge() para garantizar que nunca falle ni se cancele la actualización
                val datosUltimoMensaje = mapOf(
                    "ultimoMensaje" to texto.trim(),
                    "fechaUltimoMensaje" to Timestamp.now(),
                    "ultimoEmisorId" to currentUserIdActual
                )

                db.collection(coleccionPadre)
                    .document(chatIdActual)
                    .set(datosUltimoMensaje, com.google.firebase.firestore.SetOptions.merge())
            }
    }

    // 6. ENVIAR PROPUESTA O COTIZACIÓN
    fun enviarPropuesta(monto: Double, descripcion: String) {
        if (chatIdActual.isBlank() || currentUserIdActual.isBlank() || monto <= 0) return

        val batch = db.batch()
        val docSolicitud = db.collection("solicitudes").document(chatIdActual)
        val nuevoMensajeRef = docSolicitud.collection("mensajes").document()

        val mensajePropuesta = hashMapOf(
            "emisorId" to currentUserIdActual,
            "texto" to "Propuesta formal de servicio: \$${monto.toLong()} COP\n$descripcion",
            "fechaEnvio" to Timestamp.now(),
            "imagenUrl" to "",
            "esPropuesta" to true,
            "montoPropuesta" to monto,
            "leido" to false
        )
        batch.set(nuevoMensajeRef, mensajePropuesta)

        val actualizacionSolicitud = mapOf(
            "propuestaMonto" to monto,
            "estadoPropuesta" to "PENDIENTE",
            "ultimoMensaje" to "Propuesta enviada: \$${monto.toLong()} COP",
            "fechaUltimoMensaje" to Timestamp.now()
        )
        batch.update(docSolicitud, actualizacionSolicitud)

        batch.commit()
    }

    // 7. ACEPTAR O RECHAZAR PROPUESTAS
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