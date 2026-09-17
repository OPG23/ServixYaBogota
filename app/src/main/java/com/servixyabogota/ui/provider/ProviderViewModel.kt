package com.servixyabogota.ui.provider

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.servixyabogota.data.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider

import androidx.lifecycle.viewModelScope
import com.servixyabogota.data.model.Propuesta
import com.servixyabogota.data.model.Solicitud
import com.servixyabogota.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class EstadoProveedorUiState(
    val nombreCompleto: String = "",
    val profesion: String = "",
    val fotoUrl: String = "",
    val telefono: String = "",
    val correo: String = "",
    val estadoVerificacion: String = "NO_ENVIADO",
    val motivoRechazo: String = "",
    val justificacionRechazo: String = "",
    val documentosRechazados: List<String> = emptyList(),
    val localidades: List<String> = emptyList(),
    val categorias: List<String> = emptyList(),
    val portafolioUrls: List<String> = emptyList(),
    val descripcion: String = ""
)

class ProviderViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uiState = MutableLiveData(EstadoProveedorUiState())
    val uiState: LiveData<EstadoProveedorUiState> = _uiState

    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean> = _isUploading

    private val repository: SolicitudRepository = SolicitudRepository()

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("usuarios").document(uid).get().await()
                if (snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    user?.let {
                        _uiState.value = EstadoProveedorUiState(
                            nombreCompleto = it.nombreCompleto,
                            profesion = it.profesion,
                            fotoUrl = snapshot.getString("fotoUrl") ?: "",
                            telefono = snapshot.getString("telefono") ?: snapshot.getString("celular") ?: "",
                            correo = snapshot.getString("correo") ?: auth.currentUser?.email ?: "",
                            estadoVerificacion = it.estadoVerificacion,
                            motivoRechazo = it.motivoRechazo,
                            justificacionRechazo = it.justificacionRechazo,
                            documentosRechazados = it.documentosRechazados,
                            localidades = (snapshot.get("localidades") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            categorias = (snapshot.get("categorias") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            portafolioUrls = (snapshot.get("portafolioUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            descripcion = snapshot.getString("descripcion") ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun actualizarContactoPerfil(
        telefono: String,
        correo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onError("Usuario no autenticado.")
            return
        }

        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "telefono" to telefono,
                    "correo" to correo
                )
                db.collection("usuarios").document(uid).set(updates, SetOptions.merge()).await()
                cargarPerfil()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al actualizar contacto.")
            }
        }
    }

    fun subirDocumentosPrestador(
        profesion: String,
        fotoPerfil: Uri?,
        cedulaFrente: Uri?,
        cedulaAtras: Uri?,
        antecedentes: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onError("Usuario no autenticado.")
            return
        }

        _isUploading.value = true

        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "profesion" to profesion,
                    "estadoVerificacion" to "PENDIENTE_VERIFICACION",
                    "documentosRechazados" to emptyList<String>(),
                    "fechaEnvioDocumentos" to System.currentTimeMillis()
                )

                fotoPerfil?.let { uri ->
                    val ref = storage.reference.child("usuarios/$uid/foto_perfil.jpg")
                    ref.putFile(uri).await()
                    updates["fotoUrl"] = ref.downloadUrl.await().toString()
                }

                cedulaFrente?.let { uri ->
                    val ref = storage.reference.child("usuarios/$uid/cedula_frente.jpg")
                    ref.putFile(uri).await()
                    updates["documentos.cedulaFrente"] = ref.downloadUrl.await().toString()
                }

                cedulaAtras?.let { uri ->
                    val ref = storage.reference.child("usuarios/$uid/cedula_atras.jpg")
                    ref.putFile(uri).await()
                    updates["documentos.cedulaAtras"] = ref.downloadUrl.await().toString()
                }

                antecedentes?.let { uri ->
                    val ref = storage.reference.child("usuarios/$uid/antecedentes.pdf")
                    ref.putFile(uri).await()
                    updates["documentos.antecedentes"] = ref.downloadUrl.await().toString()
                }

                db.collection("usuarios").document(uid).update(updates).await()
                cargarPerfil()
                _isUploading.value = false
                onSuccess()

            } catch (e: Exception) {
                _isUploading.value = false
                onError(e.localizedMessage ?: "Error al subir los documentos.")
            }
        }
    }

    fun guardarCoberturaYPortafolio(
        localidades: List<String>,
        categorias: List<String>,
        itemsPortafolio: List<ItemPortafolio>,
        descripcion: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onError("Usuario no autenticado.")
            return
        }

        _isUploading.value = true

        viewModelScope.launch {
            try {
                val urlsAnteriores = _uiState.value?.portafolioUrls ?: emptyList()

                val urlsConservadas = itemsPortafolio
                    .mapNotNull { it.uri?.toString() }
                    .filter { it.startsWith("http://") || it.startsWith("https://") }

                val urlsABorrar = urlsAnteriores.filter { url -> !urlsConservadas.contains(url) }

                urlsABorrar.forEach { url ->
                    try {
                        storage.getReferenceFromUrl(url).delete().await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val urlsFinales = mutableListOf<String>()

                itemsPortafolio.forEachIndexed { index, item ->
                    val uri = item.uri
                    if (uri != null) {
                        val uriString = uri.toString()
                        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                            urlsFinales.add(uriString)
                        } else {
                            val ext = if (item.esVideo) "mp4" else "jpg"
                            val ref = storage.reference.child("usuarios/$uid/portafolio/item_${System.currentTimeMillis()}_$index.$ext")
                            ref.putFile(uri).await()
                            val urlDescarga = ref.downloadUrl.await().toString()
                            urlsFinales.add(urlDescarga)
                        }
                    }
                }

                val updates = mapOf(
                    "localidades" to localidades,
                    "categorias" to categorias,
                    "portafolioUrls" to urlsFinales,
                    "descripcion" to descripcion
                )

                db.collection("usuarios").document(uid).set(updates, SetOptions.merge()).await()
                cargarPerfil()
                _isUploading.value = false
                onSuccess()

            } catch (e: Exception) {
                _isUploading.value = false
                onError(e.localizedMessage ?: "Error al guardar el portafolio.")
            }
        }
    }

    fun cambiarContrasena(
        contrasenaActual: String,
        nuevaContrasena: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email

        if (user == null || email.isNullOrEmpty()) {
            onError("No hay una sesión activa de usuario.")
            return
        }

        // 1. Crear credencial con el correo actual y la contraseña ingresada
        val credential = EmailAuthProvider.getCredential(email, contrasenaActual)

        // 2. Reautenticar al usuario
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                // 3. Si la clave actual es correcta, actualizar en Firebase Auth
                user.updatePassword(nuevaContrasena).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        // 4. Opcional: Actualizar registro de auditoría en Firestore
                        val db = FirebaseFirestore.getInstance()
                        db.collection("usuarios").document(user.uid)
                            .update("ultimaActualizacionPassword", Timestamp.now())
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onSuccess() } // Se completa con éxito aunque el log falle
                    } else {
                        onError(updateTask.exception?.localizedMessage ?: "Error al actualizar la contraseña.")
                    }
                }
            } else {
                onError("La contraseña actual es incorrecta.")
            }
        }
    }

    // Dentro de ProviderViewModel class:

    fun actualizarPreferenciasNotificaciones(
        notificacionesPushChat: Boolean,
        alertasCorreo: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

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

    // Obtener solicitudes disponibles que coincidan con las categorías y localidades del prestador
    fun getSolicitudesDisponibles(
        misCategorias: List<String>,
        misLocalidades: List<String>
    ): StateFlow<List<Solicitud>> {
        return repository.obtenerSolicitudesDisponibles(misCategorias, misLocalidades)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    // Responder / Cotizar una solicitud
    fun enviarCotizacion(
        solicitudId: String,
        prestadorId: String,
        prestadorNombre: String,
        precio: Double,
        mensaje: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val propuesta = Propuesta(
                prestadorId = prestadorId,
                prestadorNombre = prestadorNombre,
                precioEstimado = precio,
                mensaje = mensaje
            )
            val result = repository.enviarPropuesta(solicitudId, propuesta)
            onResult(result.isSuccess)
        }
    }
}

