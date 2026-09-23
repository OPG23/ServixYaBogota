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
// MODELO DE DATOS DE MENSAJE
// ==========================================
data class MensajeChat(
    val id: String = "",
    val emisorId: String = "",
    val texto: String = "",
    val fechaEnvio: Date = Date(),
    val imagenUrl: String = "",
    val esPropuesta: Boolean = false,
    val montoPropuesta: Double = 0.0
)

data class ChatUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val propuestaMonto: Double = 0.0,
    val estadoPropuesta: String = "PENDIENTE",
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

    private var solicitudListener: ListenerRegistration? = null
    private var mensajesListener: ListenerRegistration? = null

    private var solicitudIdActual: String = ""
    private var currentUserIdActual: String = ""

    fun inicializarChat(solicitudId: String, currentUserId: String, esCliente: Boolean) {
        this.solicitudIdActual = solicitudId
        this.currentUserIdActual = currentUserId

        _uiState.value = _uiState.value.copy(esCliente = esCliente, isLoading = true)

        escucharDetallesSolicitud(solicitudId, currentUserId, esCliente)
        escucharMensajesEnVivo(solicitudId)
    }

    private fun escucharDetallesSolicitud(solicitudId: String, currentUserId: String, esCliente: Boolean) {
        solicitudListener?.remove()
        solicitudListener = db.collection("solicitudes").document(solicitudId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val propuestaMonto = snapshot.getDouble("propuestaMonto") ?: 0.0
                val estadoPropuesta = snapshot.getString("estadoPropuesta") ?: "PENDIENTE"

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
            }
    }

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

    private fun escucharMensajesEnVivo(solicitudId: String) {
        mensajesListener?.remove()
        mensajesListener = db.collection("solicitudes")
            .document(solicitudId)
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

                        MensajeChat(
                            id = doc.id,
                            emisorId = emisorId,
                            texto = texto,
                            fechaEnvio = fecha,
                            imagenUrl = imagenUrl,
                            esPropuesta = esPropuesta,
                            montoPropuesta = montoPropuesta
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        mensajes = listaMensajes,
                        isLoading = false
                    )
                }
            }
    }

    fun enviarMensaje(texto: String, imagenUrl: String? = null) {
        if (solicitudIdActual.isBlank() || currentUserIdActual.isBlank() || texto.isBlank()) return

        val nuevoMensaje = hashMapOf(
            "emisorId" to currentUserIdActual,
            "texto" to texto.trim(),
            "fechaEnvio" to Timestamp.now(),
            "imagenUrl" to (imagenUrl ?: ""),
            "esPropuesta" to false,
            "montoPropuesta" to 0.0
        )

        db.collection("solicitudes")
            .document(solicitudIdActual)
            .collection("mensajes")
            .add(nuevoMensaje)
    }

    fun aceptarPropuesta() {
        if (solicitudIdActual.isBlank()) return

        db.collection("solicitudes")
            .document(solicitudIdActual)
            .update(
                mapOf(
                    "estadoPropuesta" to "ACEPTADA",
                    "estado" to "EN_PROCESO"
                )
            )
    }

    fun rechazarPropuesta() {
        if (solicitudIdActual.isBlank()) return

        db.collection("solicitudes")
            .document(solicitudIdActual)
            .update("estadoPropuesta", "RECHAZADA")
    }

    override fun onCleared() {
        super.onCleared()
        solicitudListener?.remove()
        mensajesListener?.remove()
    }
}