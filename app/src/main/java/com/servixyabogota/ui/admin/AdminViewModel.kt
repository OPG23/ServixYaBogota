package com.servixyabogota.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SolicitudPrestador(
    val id: String = "",
    val nombreCompleto: String = "",
    val email: String = "",
    val telefono: String = "",
    val cedula: String = "",
    val profesion: String = "No especificada",
    val fotoUrl: String = "",
    val documentos: Map<String, String> = emptyMap(),
    val estadoVerificacion: String = "NO_ENVIADO",
    val fechaEnvioDocumentos: Long = 0L,
    val motivoRechazo: String = "",
    val justificacionRechazo: String = ""
)

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _solicitudes = MutableLiveData<List<SolicitudPrestador>>(emptyList())
    val solicitudes: LiveData<List<SolicitudPrestador>> = _solicitudes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        cargarSolicitudes()
    }

    fun cargarSolicitudes() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = db.collection("usuarios")
                    .whereEqualTo("rol", "prestador")
                    .get()
                    .await()

                val lista = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    SolicitudPrestador(
                        id = doc.id,
                        nombreCompleto = data["nombreCompleto"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        telefono = data["telefono"] as? String ?: "",
                        cedula = data["cedula"] as? String ?: data["numeroCedula"] as? String ?: "No registrada",
                        profesion = data["profesion"] as? String ?: "No especificada",
                        fotoUrl = data["fotoUrl"] as? String ?: "",
                        documentos = (data["documentos"] as? Map<String, String>) ?: emptyMap(),
                        estadoVerificacion = data["estadoVerificacion"] as? String ?: "NO_ENVIADO",
                        fechaEnvioDocumentos = (data["fechaEnvioDocumentos"] as? Long) ?: 0L,
                        motivoRechazo = data["motivoRechazo"] as? String ?: "",
                        justificacionRechazo = data["justificacionRechazo"] as? String ?: ""
                    )
                }

                _solicitudes.value = lista.sortedByDescending { it.fechaEnvioDocumentos }
                _isLoading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    fun aprobarPrestador(uid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "estadoVerificacion" to "APROBADO",
                    "motivoRechazo" to "",
                    "justificacionRechazo" to ""
                )
                db.collection("usuarios").document(uid).update(updates).await()
                cargarSolicitudes()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al aprobar")
            }
        }
    }

    fun rechazarPrestador(
        uid: String,
        motivo: String,
        justificacion: String,
        documentosRechazados: List<String>, // <-- AGREGAR ESTE PARÁMETRO
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "estadoVerificacion" to "RECHAZADO",
                    "motivoRechazo" to motivo,
                    "justificacionRechazo" to justificacion,
                    "documentosRechazados" to documentosRechazados, // <-- Guardar en Firestore
                    "fechaActualizacion" to System.currentTimeMillis()
                )

                db.collection("usuarios").document(uid).update(updates).await()
                cargarSolicitudes() // Recarga la lista local de solicitudes
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al procesar el rechazo")
            }
        }
    }
}