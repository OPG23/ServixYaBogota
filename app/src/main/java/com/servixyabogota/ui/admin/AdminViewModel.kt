package com.servixyabogota.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.servixyabogota.data.repository.AdminRepository
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

    private val repository = AdminRepository()
    private val db = FirebaseFirestore.getInstance()

    private val _solicitudes = MutableLiveData<List<SolicitudPrestador>>(emptyList())
    val solicitudes: LiveData<List<SolicitudPrestador>> = _solicitudes

    private val _usuarios = MutableLiveData<List<UsuarioAdmin>>(emptyList())
    val usuarios: LiveData<List<UsuarioAdmin>> = _usuarios

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        cargarSolicitudes()
        cargarUsuarios()
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
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cargarUsuarios() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.obtenerTodosUsuarios()
                result.onSuccess { rawList ->
                    val listaMappeada = rawList.mapNotNull { map ->
                        val uid = map["uid"] as? String ?: return@mapNotNull null
                        val rolStr = (map["rol"] as? String ?: "cliente").lowercase()

                        // 1. EXCLUIR ADMINISTRADORES
                        if (rolStr == "admin") return@mapNotNull null

                        val nombre = map["nombre"] as? String ?: ""
                        val apellido = map["apellido"] as? String ?: ""
                        val nombreCompleto = map["nombreCompleto"] as? String
                            ?: if (apellido.isNotBlank()) "$nombre $apellido".trim() else nombre.ifEmpty { "Usuario" }

                        val email = map["email"] as? String ?: map["correo"] as? String ?: ""
                        val cedula = map["cedula"] as? String ?: map["numeroCedula"] as? String ?: ""
                        val fotoUrl = map["fotoUrl"] as? String ?: map["foto"] as? String

                        val rolEnum = when (rolStr) {
                            "prestador" -> RolUsuario.PRESTADOR
                            else -> RolUsuario.CLIENTE
                        }

                        // 3. ESTADO NO VERIFICADO POR DEFECTO PARA USUARIOS NUEVOS
                        val estadoStr = (map["estadoVerificacion"] as? String ?: "NO_VERIFICADO").uppercase()
                        val estadoEnum = when (estadoStr) {
                            "APROBADO", "VERIFICADO" -> EstadoVerificacion.VERIFICADO
                            "DESHABILITADO" -> EstadoVerificacion.DESHABILITADO
                            "PENDIENTE_VERIFICACION", "PENDIENTE" -> EstadoVerificacion.PENDIENTE
                            else -> EstadoVerificacion.NO_VERIFICADO // "NO_VERIFICADO", "NO_ENVIADO", o vacíos
                        }

                        // 2. CALIFICACIÓN NULL SI NO TIENE
                        val calificacionRaw = (map["calificacion"] as? Number)?.toDouble()
                            ?: (map["calificacionPromedio"] as? Number)?.toDouble()

                        val calificacionFinal = if (calificacionRaw != null && calificacionRaw > 0.0) {
                            calificacionRaw
                        } else {
                            null // Permite mostrar "-.-" en la interfaz
                        }

                        val fechaMs = (map["fechaActualizacion"] as? Number)?.toLong()
                            ?: (map["fechaCreacion"] as? Number)?.toLong()
                            ?: 0L

                        UsuarioAdmin(
                            id = uid,
                            nombre = nombreCompleto,
                            correo = email,
                            cedula = cedula,
                            fotoUrl = fotoUrl,
                            rol = rolEnum,
                            estado = estadoEnum,
                            calificacion = calificacionFinal,
                            fechaRegistroMs = fechaMs
                        )
                    }
                    _usuarios.value = listaMappeada
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cambiarEstadoUsuario(uid: String, nuevoEstado: EstadoVerificacion) {
        viewModelScope.launch {
            val dbEstado = when (nuevoEstado) {
                EstadoVerificacion.VERIFICADO -> "APROBADO"
                EstadoVerificacion.NO_VERIFICADO -> "NO_VERIFICADO"
                EstadoVerificacion.DESHABILITADO -> "DESHABILITADO"
                EstadoVerificacion.PENDIENTE -> "PENDIENTE_VERIFICACION"
                EstadoVerificacion.TODOS -> "APROBADO"
            }
            val result = repository.cambiarEstadoUsuario(uid, dbEstado)
            if (result.isSuccess) {
                cargarUsuarios()
            }
        }
    }

    fun eliminarUsuario(uid: String) {
        viewModelScope.launch {
            val result = repository.eliminarUsuario(uid)
            if (result.isSuccess) {
                cargarUsuarios()
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
                cargarUsuarios()
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
        documentosRechazados: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "estadoVerificacion" to "RECHAZADO",
                    "motivoRechazo" to motivo,
                    "justificacionRechazo" to justificacion,
                    "documentosRechazados" to documentosRechazados,
                    "fechaActualizacion" to System.currentTimeMillis()
                )

                db.collection("usuarios").document(uid).update(updates).await()
                cargarSolicitudes()
                cargarUsuarios()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al procesar el rechazo")
            }
        }
    }
}