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
import com.google.firebase.storage.FirebaseStorage
import com.servixyabogota.data.model.Solicitud
import com.servixyabogota.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modelo UI para las reseñas del cliente
data class ReviewItem(
    val id: String = "",
    val nombre: String = "Prestador",
    val fecha: String = "",
    val calificacion: Int = 5,
    val comentario: String = "",
    val fechaObj: Date? = null,
    val fotoUrl: String? = null
)

class ClientViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
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

    // ESTADOS PARA PRESTADORES REALES
    var listaPrestadores by mutableStateOf<List<ClientProviderModel>>(emptyList())
    var estaCargandoPrestadores by mutableStateOf(false)

    // ESTADOS PARA RESEÑAS REALES DE FIRESTORE
    var listaResenasOtorgadas by mutableStateOf<List<ReviewItem>>(emptyList())
        private set
    var listaResenasRecibidas by mutableStateOf<List<ReviewItem>>(emptyList())
        private set
    var promedioCalificacion by mutableStateOf(5.0)
        private set
    var totalResenasCount by mutableStateOf(0)
        private set
    var estaCargandoResenas by mutableStateOf(false)
        private set

    init {
        cargarPerfilCliente()
        cargarPrestadores()
        escucharResenas()
    }

    /**
     * Escucha en tiempo real la reputación y las reseñas otorgadas/recibidas por el cliente
     */
    fun escucharResenas() {
        val uid = auth.currentUser?.uid ?: return
        estaCargandoResenas = true

        // 1. Escuchar promedio de estrellas y total de reseñas desde el perfil del usuario
        db.collection("usuarios").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    promedioCalificacion = snapshot.getDouble("calificacion")
                        ?: snapshot.getDouble("promedioCalificacion")
                                ?: 5.0
                    totalResenasCount = snapshot.getLong("totalResenas")?.toInt() ?: 0
                }
            }

        // 2. Escuchar reseñas OTORGADAS por el cliente a prestadores (Cliente -> Prestador)
        db.collection("resenas")
            .whereEqualTo("autorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    estaCargandoResenas = false
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.documents.isEmpty()) {
                    listaResenasOtorgadas = emptyList()
                    estaCargandoResenas = false
                    return@addSnapshotListener
                }

                val docsFiltered = snapshot.documents.filter { doc ->
                    doc.getString("tipo") != "PRESTADOR_A_CLIENTE"
                }

                if (docsFiltered.isEmpty()) {
                    listaResenasOtorgadas = emptyList()
                    estaCargandoResenas = false
                    return@addSnapshotListener
                }

                val otorgadasTemp = mutableListOf<ReviewItem>()
                var procesados = 0

                for (doc in docsFiltered) {
                    val reviewId = doc.id
                    val prestadorId = doc.getString("prestadorId") ?: doc.getString("destinatarioId") ?: ""
                    val calificacion = doc.getLong("calificacion")?.toInt() ?: 5
                    val comentario = doc.getString("comentario") ?: ""
                    val timestamp = doc.getTimestamp("fecha")
                    val fechaObj = timestamp?.toDate()

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaFormateada = if (fechaObj != null) sdf.format(fechaObj) else "Reciente"
                    val prestadorNombreDirecto = doc.getString("prestadorNombre") ?: doc.getString("destinatarioNombre")

                    if (!prestadorNombreDirecto.isNullOrBlank()) {
                        otorgadasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = prestadorNombreDirecto,
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj
                            )
                        )
                        procesados++
                        if (procesados == docsFiltered.size) {
                            listaResenasOtorgadas = otorgadasTemp.sortedByDescending { it.fechaObj }
                            estaCargandoResenas = false
                        }
                    } else if (prestadorId.isNotBlank()) {
                        db.collection("usuarios").document(prestadorId).get()
                            .addOnSuccessListener { providerDoc ->
                                val nombreDoc = providerDoc.getString("nombreCompleto")
                                val primerNombre = providerDoc.getString("nombre") ?: ""
                                val apellido = providerDoc.getString("apellido") ?: ""

                                val nombreFinal = when {
                                    !nombreDoc.isNullOrBlank() -> nombreDoc
                                    primerNombre.isNotBlank() -> "$primerNombre $apellido".trim()
                                    else -> "Prestador ServixYa"
                                }

                                otorgadasTemp.add(
                                    ReviewItem(
                                        id = reviewId,
                                        nombre = nombreFinal,
                                        fecha = fechaFormateada,
                                        calificacion = calificacion,
                                        comentario = comentario,
                                        fechaObj = fechaObj
                                    )
                                )
                                procesados++
                                if (procesados == docsFiltered.size) {
                                    listaResenasOtorgadas = otorgadasTemp.sortedByDescending { it.fechaObj }
                                    estaCargandoResenas = false
                                }
                            }
                            .addOnFailureListener {
                                otorgadasTemp.add(
                                    ReviewItem(
                                        id = reviewId,
                                        nombre = "Prestador ServixYa",
                                        fecha = fechaFormateada,
                                        calificacion = calificacion,
                                        comentario = comentario,
                                        fechaObj = fechaObj
                                    )
                                )
                                procesados++
                                if (procesados == docsFiltered.size) {
                                    listaResenasOtorgadas = otorgadasTemp.sortedByDescending { it.fechaObj }
                                    estaCargandoResenas = false
                                }
                            }
                    } else {
                        otorgadasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = "Prestador ServixYa",
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj
                            )
                        )
                        procesados++
                        if (procesados == docsFiltered.size) {
                            listaResenasOtorgadas = otorgadasTemp.sortedByDescending { it.fechaObj }
                            estaCargandoResenas = false
                        }
                    }
                }
            }

        // 3. Escuchar reseñas RECIBIDAS por el cliente (Prestador -> Cliente)
        db.collection("resenas")
            .whereEqualTo("destinatarioId", uid)
            .whereEqualTo("tipo", "PRESTADOR_A_CLIENTE")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.documents.isEmpty()) {
                    listaResenasRecibidas = emptyList()
                    return@addSnapshotListener
                }

                val docs = snapshot.documents
                val recibidasTemp = mutableListOf<ReviewItem>()
                var procesados = 0

                for (doc in docs) {
                    val reviewId = doc.id
                    val prestadorId = doc.getString("autorId") ?: doc.getString("prestadorId") ?: ""
                    val calificacion = doc.getLong("calificacion")?.toInt() ?: 5
                    val comentario = doc.getString("comentario") ?: ""
                    val timestamp = doc.getTimestamp("fecha")
                    val fechaObj = timestamp?.toDate()

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaFormateada = if (fechaObj != null) sdf.format(fechaObj) else "Reciente"
                    val prestadorNombreDirecto = doc.getString("autorNombre") ?: doc.getString("prestadorNombre")

                    if (!prestadorNombreDirecto.isNullOrBlank()) {
                        recibidasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = prestadorNombreDirecto,
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj
                            )
                        )
                        procesados++
                        if (procesados == docs.size) {
                            listaResenasRecibidas = recibidasTemp.sortedByDescending { it.fechaObj }
                        }
                    } else if (prestadorId.isNotBlank()) {
                        db.collection("usuarios").document(prestadorId).get()
                            .addOnSuccessListener { providerDoc ->
                                val nombreDoc = providerDoc.getString("nombreCompleto")
                                val primerNombre = providerDoc.getString("nombre") ?: ""
                                val apellido = providerDoc.getString("apellido") ?: ""

                                val nombreFinal = when {
                                    !nombreDoc.isNullOrBlank() -> nombreDoc
                                    primerNombre.isNotBlank() -> "$primerNombre $apellido".trim()
                                    else -> "Prestador ServixYa"
                                }

                                recibidasTemp.add(
                                    ReviewItem(
                                        id = reviewId,
                                        nombre = nombreFinal,
                                        fecha = fechaFormateada,
                                        calificacion = calificacion,
                                        comentario = comentario,
                                        fechaObj = fechaObj
                                    )
                                )
                                procesados++
                                if (procesados == docs.size) {
                                    listaResenasRecibidas = recibidasTemp.sortedByDescending { it.fechaObj }
                                }
                            }
                            .addOnFailureListener {
                                recibidasTemp.add(
                                    ReviewItem(
                                        id = reviewId,
                                        nombre = "Prestador ServixYa",
                                        fecha = fechaFormateada,
                                        calificacion = calificacion,
                                        comentario = comentario,
                                        fechaObj = fechaObj
                                    )
                                )
                                procesados++
                                if (procesados == docs.size) {
                                    listaResenasRecibidas = recibidasTemp.sortedByDescending { it.fechaObj }
                                }
                            }
                    } else {
                        recibidasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = "Prestador ServixYa",
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj
                            )
                        )
                        procesados++
                        if (procesados == docs.size) {
                            listaResenasRecibidas = recibidasTemp.sortedByDescending { it.fechaObj }
                        }
                    }
                }
            }
    }

    /**
     * Escucha en tiempo real los perfiles de prestadores registrados en Firestore
     */
    fun cargarPrestadores() {
        estaCargandoPrestadores = true

        db.collection("usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    estaCargandoPrestadores = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val prestadores = snapshot.documents.mapNotNull { doc ->
                        val rol = doc.getString("rol") ?: doc.getString("tipoUsuario") ?: ""

                        @Suppress("UNCHECKED_CAST")
                        val categoriasList = (doc.get("categorias") as? List<String>)
                            ?: (doc.get("especialidades") as? List<String>)
                            ?: emptyList()

                        // Es prestador si tiene rol de prestador o si ya definió sus categorías
                        val esPrestador = rol.equals("PRESTADOR", ignoreCase = true) ||
                                rol.equals("prestador", ignoreCase = true) ||
                                categoriasList.isNotEmpty()

                        // VALIDACIÓN ESTRICTA DE VERIFICACIÓN
                        val estadoVerificacion = doc.getString("estadoVerificacion")
                            ?: doc.getString("estado_verificacion")
                            ?: ""
                        val estaAprobado = estadoVerificacion.equals("APROBADO", ignoreCase = true)

                        // Si no es prestador O no está APROBADO, se descarta inmediatamente
                        if (!esPrestador || !estaAprobado) return@mapNotNull null

                        val nombreCompletoDoc = doc.getString("nombreCompleto")
                        val primerNombre = doc.getString("nombre") ?: doc.getString("primerNombre") ?: ""
                        val apellido = doc.getString("apellido") ?: doc.getString("apellidos") ?: ""
                        val nameAttr = doc.getString("name") ?: ""

                        val nombreFinal = when {
                            !nombreCompletoDoc.isNullOrBlank() -> nombreCompletoDoc
                            primerNombre.isNotBlank() && apellido.isNotBlank() -> "$primerNombre $apellido"
                            primerNombre.isNotBlank() -> primerNombre
                            nameAttr.isNotBlank() -> nameAttr
                            else -> "Prestador ServixYa"
                        }

                        val foto = doc.getString("fotoUrl") ?: doc.getString("photoUrl") ?: ""
                        val calificacion = doc.getDouble("calificacion")
                            ?: doc.getDouble("promedioCalificacion")
                            ?: doc.getDouble("rating")
                            ?: 5.0
                        val totalResenas = doc.getLong("totalResenas")?.toInt()
                            ?: doc.getLong("numeroResenas")?.toInt()
                            ?: 0

                        val disponibleHoy = doc.getBoolean("disponibleHoy") ?: doc.getBoolean("disponible") ?: true
                        val descripcion = doc.getString("descripcion") ?: doc.getString("biografia") ?: ""
                        val experienciaAnos = doc.getLong("experienciaAnos")?.toInt()
                            ?: doc.getLong("experiencia")?.toInt()
                            ?: 1

                        @Suppress("UNCHECKED_CAST")
                        val portafolioUrls = (doc.get("portafolioUrls") as? List<String>)
                            ?: (doc.get("portafolio") as? List<String>)
                            ?: emptyList()

                        @Suppress("UNCHECKED_CAST")
                        val localidades = (doc.get("localidades") as? List<String>)
                            ?: (doc.get("localidadesAtencion") as? List<String>)
                            ?: emptyList()

                        ClientProviderModel(
                            id = doc.id,
                            nombre = nombreFinal,
                            fotoUrl = foto,
                            calificacion = calificacion,
                            totalResenas = totalResenas,
                            categorias = categoriasList,
                            disponibleHoy = disponibleHoy,
                            verificado = true, // Al estar APROBADO, garantizamos que sea verificado
                            descripcion = descripcion,
                            experienciaAnos = experienciaAnos,
                            portafolioUrls = portafolioUrls,
                            localidades = localidades
                        )
                    }

                    listaPrestadores = prestadores
                    estaCargandoPrestadores = false
                }
            }
    }

    /**
     * Crea un canal de chat directo en Firestore entre el cliente autenticado y el prestador
     */
    fun obtenerOCrearChatDirecto(
        prestadorId: String,
        onSuccess: (chatId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val clienteId = auth.currentUser?.uid
        if (clienteId.isNullOrEmpty()) {
            onError("Debes iniciar sesión para escribir a un prestador")
            return
        }

        // Crea un ID determinista para evitar duplicar salas entre los dos mismos usuarios
        val chatIdConstruido = if (clienteId < prestadorId) {
            "chat_${clienteId}_$prestadorId"
        } else {
            "chat_${prestadorId}_$clienteId"
        }

        val chatRef = db.collection("chats").document(chatIdConstruido)

        chatRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                onSuccess(chatIdConstruido)
            } else {
                val nuevoChat = hashMapOf(
                    "clienteId" to clienteId,
                    "prestadorId" to prestadorId,
                    "usuarios" to listOf(clienteId, prestadorId),
                    "fechaCreacion" to Timestamp.now(),
                    "ultimoMensaje" to "Conversación iniciada",
                    "fechaUltimoMensaje" to Timestamp.now()
                )

                chatRef.set(nuevoChat, SetOptions.merge())
                    .addOnSuccessListener {
                        onSuccess(chatIdConstruido)
                    }
                    .addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Error al iniciar el chat")
                    }
            }
        }.addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al verificar la conversación")
        }
    }

    /**
     * Carga la información del usuario cliente
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
     * Actualiza el nombre, teléfono y/o la foto de perfil en Firebase Storage y Firestore
     */
    fun guardarCambiosPerfil(
        nuevoNombre: String,
        nuevoTelefono: String,
        nuevaFotoUri: Uri? = null,
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

        val updates = mutableMapOf<String, Any>(
            "nombre" to nuevoNombre,
            "nombreCompleto" to nuevoNombre,
            "primerNombre" to nombrePropio,
            "apellido" to apellidoPropio,
            "telefono" to nuevoTelefono,
            "ultimaActualizacion" to Timestamp.now()
        )

        fun guardarEnFirestore(urlFotoDescargada: String? = null) {
            if (urlFotoDescargada != null) {
                updates["fotoUrl"] = urlFotoDescargada
                updates["photoUrl"] = urlFotoDescargada
            }

            db.collection("usuarios").document(user.uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    nombre = nuevoNombre
                    telefono = nuevoTelefono
                    if (urlFotoDescargada != null) {
                        fotoUrl = urlFotoDescargada
                    }
                    estaGuardando = false
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    estaGuardando = false
                    onError(e.localizedMessage ?: "Error al actualizar la información en Firestore.")
                }
        }

        if (nuevaFotoUri != null) {
            val storageRef = storage.reference.child("profile_images/${user.uid}.jpg")

            storageRef.putFile(nuevaFotoUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            guardarEnFirestore(downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            estaGuardando = false
                            onError(e.localizedMessage ?: "Error al obtener la URL de la imagen.")
                        }
                }
                .addOnFailureListener { e ->
                    estaGuardando = false
                    onError(e.localizedMessage ?: "Error al subir la imagen de perfil.")
                }
        } else {
            guardarEnFirestore()
        }
    }

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

    fun actualizarSolicitud(
        solicitudId: String,
        categoria: String,
        detalle: String,
        urgencia: String,
        direccion: String,
        localidad: String,
        archivos: List<Uri>,
        context: Context,
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
                    context = context
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

    // ==========================================
// ESTADOS Y CÓDIGO PARA VER RESEÑAS PÚBLICAS DEL PRESTADOR
// ==========================================
    var listaResenasDelPrestador by mutableStateOf<List<ReviewItem>>(emptyList())
        private set

    var estaCargandoResenasPrestador by mutableStateOf(false)
        private set

    /**
     * Consulta en Firestore todas las reseñas recibidas por un prestador específico
     */
    /**
     * Consulta en Firestore todas las reseñas recibidas por un prestador específico
     */
    fun cargarResenasDelPrestador(prestadorId: String) {
        if (prestadorId.isBlank()) return
        estaCargandoResenasPrestador = true

        db.collection("resenas")
            .whereEqualTo("prestadorId", prestadorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.documents.isEmpty()) {
                    db.collection("resenas")
                        .whereEqualTo("destinatarioId", prestadorId)
                        .addSnapshotListener { snapshot2, error2 ->
                            if (error2 != null || snapshot2 == null || snapshot2.documents.isEmpty()) {
                                listaResenasDelPrestador = emptyList()
                                estaCargandoResenasPrestador = false
                                return@addSnapshotListener
                            }
                            procesarDocumentosResenasPrestador(snapshot2.documents, prestadorId)
                        }
                    return@addSnapshotListener
                }
                procesarDocumentosResenasPrestador(snapshot.documents, prestadorId)
            }
    }

    private fun procesarDocumentosResenasPrestador(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        prestadorId: String
    ) {
        val resenasTemp = mutableListOf<ReviewItem>()

        // Descartamos las reseñas hechas de Prestador a Cliente o donde el autor sea el prestador
        val docsFiltrados = documents.filter { doc ->
            val tipo = doc.getString("tipo") ?: ""
            val autorId = doc.getString("autorId") ?: ""

            tipo != "PRESTADOR_A_CLIENTE" && autorId != prestadorId
        }

        if (docsFiltrados.isEmpty()) {
            listaResenasDelPrestador = emptyList()
            estaCargandoResenasPrestador = false
            return
        }

        var procesados = 0

        for (doc in docsFiltrados) {
            val reviewId = doc.id

            // Extraemos el ID del cliente (priorizando clienteId)
            val clienteUid = doc.getString("clienteId")
                ?.takeIf { it.isNotBlank() && it != prestadorId }
                ?: doc.getString("autorId")
                    ?.takeIf { it.isNotBlank() && it != prestadorId }
                ?: ""

            val calificacion = doc.getLong("calificacion")?.toInt() ?: 5
            val comentario = doc.getString("comentario") ?: ""
            val timestamp = doc.getTimestamp("fecha")
            val fechaObj = timestamp?.toDate()

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaFormateada = if (fechaObj != null) sdf.format(fechaObj) else "Reciente"

            val autorNombreDirecto = doc.getString("clienteNombre")
                ?: doc.getString("autorNombre")

            val fotoDirecta = doc.getString("clienteFotoUrl")
                ?: doc.getString("clienteFoto")
                ?: doc.getString("autorFoto")
                ?: doc.getString("autorFotoUrl")

            // Si el documento de la reseña ya contiene la URL de la foto
            if (!autorNombreDirecto.isNullOrBlank() && !fotoDirecta.isNullOrBlank()) {
                resenasTemp.add(
                    ReviewItem(
                        id = reviewId,
                        nombre = autorNombreDirecto,
                        fecha = fechaFormateada,
                        calificacion = calificacion,
                        comentario = comentario,
                        fechaObj = fechaObj,
                        fotoUrl = fotoDirecta
                    )
                )
                procesados++
                if (procesados == docsFiltrados.size) {
                    listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                    estaCargandoResenasPrestador = false
                }
            } else if (clienteUid.isNotBlank()) {
                // Consultamos el documento del cliente en 'usuarios' usando su clienteId/fotoUrl
                db.collection("usuarios").document(clienteUid).get()
                    .addOnSuccessListener { userDoc ->
                        val nombreDoc = userDoc.getString("nombreCompleto")
                        val primerNombre = userDoc.getString("nombre") ?: ""
                        val apellido = userDoc.getString("apellido") ?: ""

                        val nombreFinal = when {
                            !autorNombreDirecto.isNullOrBlank() -> autorNombreDirecto
                            !nombreDoc.isNullOrBlank() -> nombreDoc
                            primerNombre.isNotBlank() -> "$primerNombre $apellido".trim()
                            else -> "Cliente ServixYa"
                        }

                        val fotoUrlUser = userDoc.getString("fotoUrl")
                            ?: userDoc.getString("photoUrl")
                            ?: userDoc.getString("fotoPerfil")
                            ?: userDoc.getString("foto")

                        resenasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = nombreFinal,
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj,
                                fotoUrl = fotoUrlUser
                            )
                        )
                        procesados++
                        if (procesados == docsFiltrados.size) {
                            listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                            estaCargandoResenasPrestador = false
                        }
                    }
                    .addOnFailureListener {
                        resenasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = autorNombreDirecto ?: "Cliente ServixYa",
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj,
                                fotoUrl = fotoDirecta
                            )
                        )
                        procesados++
                        if (procesados == docsFiltrados.size) {
                            listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                            estaCargandoResenasPrestador = false
                        }
                    }
            } else {
                resenasTemp.add(
                    ReviewItem(
                        id = reviewId,
                        nombre = autorNombreDirecto ?: "Cliente ServixYa",
                        fecha = fechaFormateada,
                        calificacion = calificacion,
                        comentario = comentario,
                        fechaObj = fechaObj,
                        fotoUrl = fotoDirecta
                    )
                )
                procesados++
                if (procesados == docsFiltrados.size) {
                    listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                    estaCargandoResenasPrestador = false
                }
            }
        }
    }

    private fun procesarDocumentosResenasPrestador(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val resenasTemp = mutableListOf<ReviewItem>()

        // Descartamos las reseñas hechas de Prestador a Cliente
        val docsFiltrados = documents.filter { doc ->
            doc.getString("tipo") != "PRESTADOR_A_CLIENTE"
        }

        if (docsFiltrados.isEmpty()) {
            listaResenasDelPrestador = emptyList()
            estaCargandoResenasPrestador = false
            return
        }

        var procesados = 0

        for (doc in docsFiltrados) {
            val reviewId = doc.id
            val autorId = doc.getString("autorId") ?: ""
            val calificacion = doc.getLong("calificacion")?.toInt() ?: 5
            val comentario = doc.getString("comentario") ?: ""
            val timestamp = doc.getTimestamp("fecha")
            val fechaObj = timestamp?.toDate()

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaFormateada = if (fechaObj != null) sdf.format(fechaObj) else "Reciente"

            val autorNombreDirecto = doc.getString("autorNombre") ?: doc.getString("clienteNombre")
            val fotoDirecta = doc.getString("autorFoto")
                ?: doc.getString("clienteFoto")
                ?: doc.getString("autorFotoUrl")

            if (!autorNombreDirecto.isNullOrBlank() && !fotoDirecta.isNullOrBlank()) {
                resenasTemp.add(
                    ReviewItem(
                        id = reviewId,
                        nombre = autorNombreDirecto,
                        fecha = fechaFormateada,
                        calificacion = calificacion,
                        comentario = comentario,
                        fechaObj = fechaObj,
                        fotoUrl = fotoDirecta
                    )
                )
                procesados++
                if (procesados == docsFiltrados.size) {
                    listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                    estaCargandoResenasPrestador = false
                }
            } else if (autorId.isNotBlank()) {
                db.collection("usuarios").document(autorId).get()
                    .addOnSuccessListener { userDoc ->
                        val nombreDoc = userDoc.getString("nombreCompleto")
                        val primerNombre = userDoc.getString("nombre") ?: ""
                        val apellido = userDoc.getString("apellido") ?: ""

                        val nombreFinal = when {
                            !autorNombreDirecto.isNullOrBlank() -> autorNombreDirecto
                            !nombreDoc.isNullOrBlank() -> nombreDoc
                            primerNombre.isNotBlank() -> "$primerNombre $apellido".trim()
                            else -> "Cliente ServixYa"
                        }

                        val fotoUrlUser = userDoc.getString("fotoUrl")
                            ?: userDoc.getString("fotoPerfil")
                            ?: userDoc.getString("foto")
                            ?: fotoDirecta

                        resenasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = nombreFinal,
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj,
                                fotoUrl = fotoUrlUser
                            )
                        )
                        procesados++
                        if (procesados == docsFiltrados.size) {
                            listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                            estaCargandoResenasPrestador = false
                        }
                    }
                    .addOnFailureListener {
                        resenasTemp.add(
                            ReviewItem(
                                id = reviewId,
                                nombre = autorNombreDirecto ?: "Cliente ServixYa",
                                fecha = fechaFormateada,
                                calificacion = calificacion,
                                comentario = comentario,
                                fechaObj = fechaObj,
                                fotoUrl = fotoDirecta
                            )
                        )
                        procesados++
                        if (procesados == docsFiltrados.size) {
                            listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                            estaCargandoResenasPrestador = false
                        }
                    }
            } else {
                resenasTemp.add(
                    ReviewItem(
                        id = reviewId,
                        nombre = autorNombreDirecto ?: "Cliente ServixYa",
                        fecha = fechaFormateada,
                        calificacion = calificacion,
                        comentario = comentario,
                        fechaObj = fechaObj,
                        fotoUrl = fotoDirecta
                    )
                )
                procesados++
                if (procesados == docsFiltrados.size) {
                    listaResenasDelPrestador = resenasTemp.sortedByDescending { it.fechaObj }
                    estaCargandoResenasPrestador = false
                }
            }
        }
    }
}