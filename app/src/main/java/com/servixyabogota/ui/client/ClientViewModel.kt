package com.servixyabogota.ui.client

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.servixyabogota.data.model.Solicitud
import com.servixyabogota.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repository: SolicitudRepository = SolicitudRepository()

    // Estados del perfil
    var nombre by mutableStateOf("")
    var telefono by mutableStateOf("")
    var correo by mutableStateOf("")
    var ciudad by mutableStateOf("Bogotá, D.C.")
    var fotoUrl by mutableStateOf("")
    var tipoCliente by mutableStateOf("Cliente Residencial")

    // Estados de UI
    var estaCargando by mutableStateOf(false)
    var estaGuardando by mutableStateOf(false)

    init {
        cargarPerfilCliente()
    }

    /**
     * Carga la información del usuario concatenando Nombre y Apellido si existen
     */
    fun cargarPerfilCliente() {
        val user = auth.currentUser ?: return
        estaCargando = true

        db.collection("usuarios").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nombreCompletoDoc = doc.getString("nombreCompleto")
                    val primerNombre = doc.getString("nombre") ?: doc.getString("primerNombre") ?: ""
                    val apellido = doc.getString("apellido") ?: doc.getString("apellidos") ?: ""
                    val nameAttr = doc.getString("name") ?: ""

                    nombre = when {
                        !nombreCompletoDoc.isNullOrBlank() -> nombreCompletoDoc
                        primerNombre.isNotBlank() && apellido.isNotBlank() -> "$primerNombre $apellido"
                        primerNombre.isNotBlank() -> primerNombre
                        nameAttr.isNotBlank() -> nameAttr
                        else -> user.displayName ?: ""
                    }

                    telefono = doc.getString("telefono") ?: doc.getString("phone") ?: ""
                    correo = doc.getString("correo") ?: doc.getString("email") ?: user.email ?: ""
                    ciudad = doc.getString("ciudad") ?: doc.getString("city") ?: "Bogotá, D.C."
                    fotoUrl = doc.getString("fotoUrl") ?: doc.getString("photoUrl") ?: user.photoUrl?.toString() ?: ""
                    tipoCliente = doc.getString("tipoCliente") ?: "Cliente Residencial"
                } else {
                    correo = user.email ?: ""
                    nombre = user.displayName ?: ""
                    fotoUrl = user.photoUrl?.toString() ?: ""
                }
                estaCargando = false
            }
            .addOnFailureListener {
                estaCargando = false
            }
    }

    /**
     * Guarda el nombre completo e incrementa la compatibilidad con campos nombre / apellido
     */
    fun guardarCambiosPerfil(
        nuevoNombre: String,
        nuevoTelefono: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("No hay una sesión activa de usuario.")
            return
        }

        estaGuardando = true

        val partesNombre = nuevoNombre.trim().split("\\s+".toRegex())
        val nombrePropio = partesNombre.firstOrNull() ?: ""
        val apellidoPropio = if (partesNombre.size > 1) partesNombre.drop(1).joinToString(" ") else ""

        val updates = mapOf(
            "nombre" to nuevoNombre,
            "nombreCompleto" to nuevoNombre,
            "primerNombre" to nombrePropio,
            "apellido" to apellidoPropio,
            "telefono" to nuevoTelefono,
            "ultimaActualizacion" to Timestamp.now()
        )

        db.collection("usuarios").document(user.uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                nombre = nuevoNombre
                telefono = nuevoTelefono
                estaGuardando = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                estaGuardando = false
                onError(e.localizedMessage ?: "Error al actualizar la información.")
            }
    }

    /**
     * Publica la solicitud tomando de forma segura clienteId, clienteNombre y clienteFotoUrl
     */
    fun publicarSolicitud(
        categoria: String,
        detalle: String,
        urgencia: String,
        direccion: String,
        localidad: String,
        urisArchivos: List<Uri>,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("Usuario no autenticado")
            return
        }

        viewModelScope.launch {
            val nombreFinal = if (nombre.isNotBlank()) nombre else (user.displayName ?: "Cliente ServixYa")
            val fotoFinal = if (fotoUrl.isNotBlank()) fotoUrl else (user.photoUrl?.toString() ?: "")

            val solicitudTemp = Solicitud(
                clienteId = user.uid,
                clienteNombre = nombreFinal,
                clienteFotoUrl = fotoFinal,
                categoria = categoria,
                detalleProblema = detalle,
                nivelUrgencia = urgencia,
                direccion = direccion,
                localidad = localidad
            )

            val result = repository.crearSolicitud(solicitudTemp, urisArchivos, context)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error al publicar la solicitud")
            }
        }
    }

    /**
     * Actualiza la solicitud sincronizando también nombre, foto y context
     */
    fun actualizarSolicitud(
        solicitudId: String,
        categoria: String,
        detalle: String,
        urgencia: String,
        direccion: String,
        localidad: String,
        archivos: List<Uri>,
        context: Context, // <-- AÑADIDO PARÁMETRO CONTEXT
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("Usuario no autenticado")
            return
        }

        viewModelScope.launch {
            try {
                val nombreFinal = if (nombre.isNotBlank()) nombre else (user.displayName ?: "Cliente ServixYa")
                val fotoFinal = if (fotoUrl.isNotBlank()) fotoUrl else (user.photoUrl?.toString() ?: "")

                val result = repository.actualizarSolicitudConArchivos(
                    solicitudId = solicitudId,
                    clienteId = user.uid,
                    datos = mapOf(
                        "categoria" to categoria,
                        "detalleProblema" to detalle,
                        "nivelUrgencia" to urgencia,
                        "direccion" to direccion,
                        "localidad" to localidad,
                        "clienteNombre" to nombreFinal,
                        "clienteFotoUrl" to fotoFinal
                    ),
                    archivosUris = archivos,
                    context = context // <-- PASADO AL REPOSITORIO
                )

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.localizedMessage ?: "Error al actualizar")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error inesperado")
            }
        }
    }

    // Obtener "Mis Solicitudes"
    fun getMisSolicitudes(clienteId: String): StateFlow<List<Solicitud>> {
        return repository.obtenerMisSolicitudesCliente(clienteId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun cancelarSolicitud(
        solicitudId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.cancelarSolicitud(solicitudId)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "Error al cancelar")
        }
    }

    fun cerrarSesion(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }

    fun cambiarContrasena(
        contrasenaActual: String,
        nuevaContrasena: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrEmpty()) {
            onError("No hay una sesión activa de usuario.")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, contrasenaActual)

        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(nuevaContrasena).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        db.collection("usuarios").document(user.uid)
                            .update("ultimaActualizacionPassword", Timestamp.now())
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onSuccess() }
                    } else {
                        onError(updateTask.exception?.localizedMessage ?: "Error al actualizar la contraseña.")
                    }
                }
            } else {
                onError("La contraseña actual es incorrecta.")
            }
        }
    }

    fun actualizarPreferenciasNotificaciones(
        notificacionesPushChat: Boolean,
        alertasCorreo: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser ?: return

        val updates = mapOf(
            "notificacionesPushChat" to notificacionesPushChat,
            "alertasCorreo" to alertasCorreo
        )

        db.collection("usuarios").document(user.uid)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Error al actualizar preferencias")
            }
    }
}