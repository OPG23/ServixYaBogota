package com.servixyabogota.ui.provider

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.servixyabogota.data.model.Propuesta
import com.servixyabogota.data.model.Solicitud
import com.servixyabogota.data.model.User
import com.servixyabogota.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FieldValue


// Modelo UI para los chats directos recibidos por el prestador
data class ProviderDirectChatUi(
    val id: String = "",
    val clienteId: String = "",
    val clientName: String = "Cliente",
    val clientPhoto: String = "",
    val lastMessage: String = "",
    val timeFormatted: String = "",
    val fechaUltimoMensaje: Date? = null
)

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

    // ESTADO PARA CHATS DIRECTOS REALES
    var listaChatsDirectos by mutableStateOf<List<ProviderDirectChatUi>>(emptyList())
        private set
    var estaCargandoChats by mutableStateOf(false)
        private set

    init {
        cargarPerfil()
        escucharChatsDirectos()
    }

    /**
     * Escucha en tiempo real la colección "chats" en Firestore para el Prestador autenticado
     */
    fun escucharChatsDirectos() {
        val uid = auth.currentUser?.uid ?: return
        estaCargandoChats = true

        db.collection("chats")
            .whereEqualTo("prestadorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    estaCargandoChats = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val docs = snapshot.documents
                    if (docs.isEmpty()) {
                        listaChatsDirectos = emptyList()
                        estaCargandoChats = false
                        return@addSnapshotListener
                    }

                    val chatsTemp = mutableListOf<ProviderDirectChatUi>()
                    var procesados = 0

                    for (doc in docs) {
                        val chatId = doc.id
                        val clienteId = doc.getString("clienteId") ?: ""
                        val ultimoMensaje = doc.getString("ultimoMensaje") ?: "Conversación iniciada"
                        val timestamp = doc.getTimestamp("fechaUltimoMensaje")
                        val fecha = timestamp?.toDate()

                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val horaFormateada = if (fecha != null) sdf.format(fecha) else ""

                        if (clienteId.isNotBlank()) {
                            db.collection("usuarios").document(clienteId).get()
                                .addOnSuccessListener { clientDoc ->
                                    val nombreDoc = clientDoc.getString("nombreCompleto")
                                    val primerNombre = clientDoc.getString("nombre") ?: ""
                                    val apellido = clientDoc.getString("apellido") ?: ""

                                    val nombreFinal = when {
                                        !nombreDoc.isNullOrBlank() -> nombreDoc
                                        primerNombre.isNotBlank() -> "$primerNombre $apellido".trim()
                                        else -> "Cliente ServixYa"
                                    }

                                    val fotoFinal = clientDoc.getString("fotoUrl")
                                        ?: clientDoc.getString("photoUrl")
                                        ?: ""

                                    chatsTemp.add(
                                        ProviderDirectChatUi(
                                            id = chatId,
                                            clienteId = clienteId,
                                            clientName = nombreFinal,
                                            clientPhoto = fotoFinal,
                                            lastMessage = ultimoMensaje,
                                            timeFormatted = horaFormateada,
                                            fechaUltimoMensaje = fecha
                                        )
                                    )

                                    procesados++
                                    if (procesados == docs.size) {
                                        listaChatsDirectos = chatsTemp.sortedByDescending { it.fechaUltimoMensaje }
                                        estaCargandoChats = false
                                    }
                                }
                                .addOnFailureListener {
                                    procesados++
                                    if (procesados == docs.size) {
                                        listaChatsDirectos = chatsTemp.sortedByDescending { it.fechaUltimoMensaje }
                                        estaCargandoChats = false
                                    }
                                }
                        } else {
                            procesados++
                            if (procesados == docs.size) {
                                listaChatsDirectos = chatsTemp.sortedByDescending { it.fechaUltimoMensaje }
                                estaCargandoChats = false
                            }
                        }
                    }
                }
            }
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

        val credential = EmailAuthProvider.getCredential(email, contrasenaActual)

        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(nuevaContrasena).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        val db = FirebaseFirestore.getInstance()
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

    fun getSolicitudesDisponibles(
        misCategorias: List<String>,
        misLocalidades: List<String>
    ): StateFlow<List<Solicitud>> {
        return repository.obtenerSolicitudesDisponibles(misCategorias, misLocalidades)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

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

    fun postularASolicitud(
        solicitudId: String,
        montoPropuesta: Double,
        mensajePresentacion: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val prestadorId = auth.currentUser?.uid
        if (prestadorId.isNullOrEmpty()) {
            onError("Debes iniciar sesión para postularte")
            return
        }

        viewModelScope.launch {
            // 1. Obtener datos del prestador
            db.collection("usuarios").document(prestadorId).get()
                .addOnSuccessListener { docPrestador ->
                    val nombrePrestador = docPrestador.getString("nombreCompleto")
                        ?: docPrestador.getString("nombre") ?: "Prestador ServixYa"
                    val fotoPrestador = docPrestador.getString("fotoUrl") ?: ""
                    val calificacion = docPrestador.getDouble("calificacion") ?: 5.0
                    val totalResenas = docPrestador.getLong("totalResenas")?.toInt() ?: 0

                    val datosPropuesta = hashMapOf(
                        "prestadorId" to prestadorId,
                        "prestadorNombre" to nombrePrestador,
                        "prestadorFotoUrl" to fotoPrestador,
                        "calificacion" to calificacion,
                        "totalResenas" to totalResenas,
                        "monto" to montoPropuesta,
                        "mensaje" to mensajePresentacion,
                        "fechaPostulacion" to Timestamp.now(),
                        "estado" to "PENDIENTE"
                    )

                    val batch = db.batch()

                    // 2. Guardar postulación en subcolección
                    val propuestaRef = db.collection("solicitudes")
                        .document(solicitudId)
                        .collection("propuestas")
                        .document(prestadorId)

                    batch.set(propuestaRef, datosPropuesta, SetOptions.merge())

                    // 3. Incrementar contador de interesados en la solicitud
                    val solicitudRef = db.collection("solicitudes").document(solicitudId)
                    batch.update(solicitudRef, "numeroInteresados", FieldValue.increment(1))
                    batch.update(solicitudRef, "cantidadPropuestas", FieldValue.increment(1))

                    // 4. Crear primer mensaje en el chat del servicio
                    val primerMensajeRef = solicitudRef.collection("mensajes").document()
                    val primerMensaje = hashMapOf(
                        "id" to primerMensajeRef.id,
                        "emisorId" to prestadorId,
                        "texto" to "Hola, me he postulado a tu solicitud. $mensajePresentacion",
                        "fechaEnvio" to Timestamp.now(),
                        "esOferta" to true,
                        "montoOferta" to montoPropuesta,
                        "estadoOferta" to "PENDIENTE"
                    )
                    batch.set(primerMensajeRef, primerMensaje)

                    batch.commit()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error al enviar la postulación") }
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Error al consultar perfil del prestador")
                }
        }
    }
}