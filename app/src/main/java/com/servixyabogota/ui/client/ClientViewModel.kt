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
import com.google.firebase.firestore.ListenerRegistration
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
import java.util.UUID

data class NotificacionInApp(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val mensaje: String,
    val tipo: TipoNotificacion
)

enum class TipoNotificacion {
    NUEVO_MENSAJE,
    NUEVA_PROPUESTA
}

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

    var nombre by mutableStateOf("")
    var telefono by mutableStateOf("")
    var correo by mutableStateOf("")
    var ciudad by mutableStateOf("Bogotá, D.C.")
    var fotoUrl by mutableStateOf("")
    var tipoCliente by mutableStateOf("Cliente Residencial")

    var estaCargando by mutableStateOf(false)
    var estaGuardando by mutableStateOf(false)

    var listaPrestadores by mutableStateOf<List<ClientProviderModel>>(emptyList())
    var estaCargandoPrestadores by mutableStateOf(false)

    var listaResenasOtorgadas by mutableStateOf<List<ReviewItem>>(emptyList())
        private set
    var listaResenasRecibidas by mutableStateOf<List<ReviewItem>>(emptyList())
        private set
    var promedioCalificacion by mutableStateOf(0.0)
        private set
    var totalResenasCount by mutableStateOf(0)
        private set
    var estaCargandoResenas by mutableStateOf(false)
        private set

    // 🟢 ESTADOS PARA NOTIFICACIONES EN TIEMPO REAL
    var notificacionActiva by mutableStateOf<NotificacionInApp?>(null)
        private set
    var tieneMensajesSinLeer by mutableStateOf(false)
        private set
    var tieneNuevasPropuestas by mutableStateOf(false)
        private set

    private var resenasUserListener: ListenerRegistration? = null
    private var resenasOtorgadasListener: ListenerRegistration? = null
    private var resenasRecibidasListener: ListenerRegistration? = null
    private var prestadoresListener: ListenerRegistration? = null
    private var listenerChatsNotificaciones: ListenerRegistration? = null
    private var listenerSolicitudesNotificaciones: ListenerRegistration? = null

    private val registroConteoPropuestas = mutableMapOf<String, Int>()
    private var esPrimeraCargaSolicitudes = true

    init {
        cargarPerfilCliente()
    }

    override fun onCleared() {
        super.onCleared()
        resenasUserListener?.remove()
        resenasOtorgadasListener?.remove()
        resenasRecibidasListener?.remove()
        prestadoresListener?.remove()
        listenerChatsNotificaciones?.remove()
        listenerSolicitudesNotificaciones?.remove()
    }

    fun cargarPerfilCliente() {
        val user = auth.currentUser
        if (user == null) {
            limpiarEstadoLocal()
            return
        }

        limpiarEstadoLocal()
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
                    fotoUrl = doc.getString("fotoUrl")
                        ?: doc.getString("photoUrl")
                                ?: doc.getString("fotoPerfilUrl")
                                ?: user.photoUrl?.toString()
                                ?: ""
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

        cargarPrestadores()
        escucharResenas()
        iniciarEscuchadoresNotificacionesCliente()
    }

    private fun limpiarEstadoLocal() {
        nombre = ""
        telefono = ""
        correo = ""
        ciudad = "Bogotá, D.C."
        fotoUrl = ""
        tipoCliente = "Cliente Residencial"
        promedioCalificacion = 0.0
        totalResenasCount = 0
        listaResenasOtorgadas = emptyList()
        listaResenasRecibidas = emptyList()
        listaPrestadores = emptyList()
        notificacionActiva = null
        tieneMensajesSinLeer = false
        tieneNuevasPropuestas = false
        registroConteoPropuestas.clear()
        esPrimeraCargaSolicitudes = true
    }

    // 🟢 ESCUCHADORES EN TIEMPO REAL DE MENSAJES Y PROPUESTAS
    fun iniciarEscuchadoresNotificacionesCliente() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Escuchar Nuevos Mensajes
        listenerChatsNotificaciones?.remove()
        listenerChatsNotificaciones = db.collection("chats")
            .whereEqualTo("clienteId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                var haySinLeer = false
                for (doc in snapshot.documents) {
                    val leido = doc.getBoolean("leidoPorCliente") ?: true
                    val ultimoEmisor = doc.getString("ultimoEmisorId") ?: ""

                    if (!leido && ultimoEmisor.isNotBlank() && ultimoEmisor != uid) {
                        haySinLeer = true
                        val prestadorNombre = doc.getString("prestadorNombre") ?: "Un prestador"
                        val texto = doc.getString("ultimoMensaje") ?: "Te ha enviado un mensaje"

                        mostrarNotificacionInApp(
                            titulo = "Nuevo mensaje de $prestadorNombre",
                            mensaje = texto,
                            tipo = TipoNotificacion.NUEVO_MENSAJE
                        )
                        break
                    }
                }
                tieneMensajesSinLeer = haySinLeer
            }

        // 2. Escuchar Nuevas Postulaciones a Solicitudes
        listenerSolicitudesNotificaciones?.remove()
        listenerSolicitudesNotificaciones = db.collection("solicitudes")
            .whereEqualTo("clienteId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val solicitudId = doc.id
                    val numPropuestas = doc.getLong("cantidadPropuestas")?.toInt()
                        ?: doc.getLong("numeroInteresados")?.toInt()
                        ?: 0

                    val conteoAnterior = registroConteoPropuestas[solicitudId]

                    if (!esPrimeraCargaSolicitudes && conteoAnterior != null && numPropuestas > conteoAnterior) {
                        val tituloSolicitud = doc.getString("detalleProblema")
                            ?: doc.getString("categoria")
                            ?: "tu solicitud"

                        tieneNuevasPropuestas = true
                        mostrarNotificacionInApp(
                            titulo = "¡Nueva postulación recibida!",
                            mensaje = "Un profesional se ha postulado a: $tituloSolicitud",
                            tipo = TipoNotificacion.NUEVA_PROPUESTA
                        )
                    }

                    registroConteoPropuestas[solicitudId] = numPropuestas
                }
                esPrimeraCargaSolicitudes = false
            }
    }

    private fun mostrarNotificacionInApp(titulo: String, mensaje: String, tipo: TipoNotificacion) {
        notificacionActiva = NotificacionInApp(
            titulo = titulo,
            mensaje = mensaje,
            tipo = tipo
        )
    }

    fun descartarNotificacion() {
        notificacionActiva = null
    }

    fun limpiarBadgeMensajes() {
        tieneMensajesSinLeer = false
    }

    fun limpiarBadgePropuestas() {
        tieneNuevasPropuestas = false
    }

    fun escucharResenas() {
        val uid = auth.currentUser?.uid ?: return
        estaCargandoResenas = true

        resenasUserListener?.remove()
        resenasOtorgadasListener?.remove()
        resenasRecibidasListener?.remove()

        resenasUserListener = db.collection("usuarios").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    promedioCalificacion = snapshot.getDouble("calificacion")
                        ?: snapshot.getDouble("promedioCalificacion")
                                ?: 0.0
                    totalResenasCount = snapshot.getLong("totalResenas")?.toInt() ?: 0
                }
            }

        resenasOtorgadasListener = db.collection("resenas")
            .whereEqualTo("autorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.documents.isEmpty()) {
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

        resenasRecibidasListener = db.collection("resenas")
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

    fun cargarPrestadores() {
        estaCargandoPrestadores = true
        prestadoresListener?.remove()

        prestadoresListener = db.collection("usuarios")
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

                        val esPrestador = rol.equals("PRESTADOR", ignoreCase = true) ||
                                categoriasList.isNotEmpty()

                        val estadoVerificacion = doc.getString("estadoVerificacion")
                            ?: doc.getString("estado_verificacion")
                            ?: ""
                        val estaAprobado = estadoVerificacion.equals("APROBADO", ignoreCase = true) ||
                                estadoVerificacion.equals("VERIFICADO", ignoreCase = true)

                        val activo = doc.getBoolean("activo") ?: true
                        val estadoCuenta = doc.getString("estadoCuenta") ?: doc.getString("estado") ?: ""
                        val estaDeshabilitado = !activo ||
                                estadoCuenta.equals("DESHABILITADO", ignoreCase = true) ||
                                estadoCuenta.equals("INHABILITADO", ignoreCase = true) ||
                                estadoCuenta.equals("DESACTIVADO", ignoreCase = true) ||
                                estadoCuenta.equals("ELIMINADO", ignoreCase = true) ||
                                estadoVerificacion.equals("DESHABILITADO", ignoreCase = true) ||
                                estadoVerificacion.equals("INHABILITADO", ignoreCase = true) ||
                                estadoVerificacion.equals("DESACTIVADO", ignoreCase = true) ||
                                estadoVerificacion.equals("ELIMINADO", ignoreCase = true)

                        @Suppress("UNCHECKED_CAST")
                        val localidades = (doc.get("localidades") as? List<String>)
                            ?: (doc.get("localidadesAtencion") as? List<String>)
                            ?: emptyList()

                        val descripcion = doc.getString("descripcion") ?: doc.getString("biografia") ?: ""

                        @Suppress("UNCHECKED_CAST")
                        val portafolioUrls = (doc.get("portafolioUrls") as? List<String>)
                            ?: (doc.get("portafolio") as? List<String>)
                            ?: emptyList()

                        val haCompletadoPerfilInicial = localidades.isNotEmpty() && descripcion.isNotBlank()

                        if (!esPrestador || !estaAprobado || estaDeshabilitado || !haCompletadoPerfilInicial) {
                            return@mapNotNull null
                        }

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

                        val foto = doc.getString("fotoUrl")
                            ?: doc.getString("photoUrl")
                            ?: doc.getString("fotoPerfilUrl")
                            ?: doc.getString("fotoPerfil")
                            ?: doc.getString("foto")
                            ?: ""

                        val calificacion = doc.getDouble("calificacion")
                            ?: doc.getDouble("promedioCalificacion")
                            ?: doc.getDouble("rating")
                            ?: 0.0

                        val totalResenas = doc.getLong("totalResenas")?.toInt()
                            ?: doc.getLong("numeroResenas")?.toInt()
                            ?: 0

                        val disponibleHoy = doc.getBoolean("disponibleHoy") ?: doc.getBoolean("disponible") ?: true
                        val experienciaAnos = doc.getLong("experienciaAnos")?.toInt()
                            ?: doc.getLong("experiencia")?.toInt()
                            ?: 1

                        ClientProviderModel(
                            id = doc.id,
                            nombre = nombreFinal,
                            fotoUrl = foto,
                            calificacion = calificacion,
                            totalResenas = totalResenas,
                            categorias = categoriasList,
                            disponibleHoy = disponibleHoy,
                            verificado = true,
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

    fun aceptarPropuesta(
        solicitudId: String,
        propuestaId: String,
        prestadorId: String,
        prestadorNombre: String = "",
        monto: Double = 0.0,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (solicitudId.isBlank() || prestadorId.isBlank()) {
            onError("ID de solicitud o prestador no válido.")
            return
        }

        viewModelScope.launch {
            val solicitudRef = db.collection("solicitudes").document(solicitudId)
            val propuestaRef = solicitudRef.collection("propuestas").document(propuestaId)

            val datosSolicitud = mapOf(
                "estado" to "EN_PROCESO",
                "prestadorIdAsignado" to prestadorId,
                "prestadorId" to prestadorId,
                "prestadorNombre" to prestadorNombre,
                "montoAcordado" to monto,
                "fechaAsignacion" to Timestamp.now()
            )

            val datosPropuesta = mapOf(
                "estado" to "ACEPTADA",
                "fechaAceptacion" to Timestamp.now()
            )

            db.runTransaction { transaction ->
                transaction.update(solicitudRef, datosSolicitud)
                transaction.update(propuestaRef, datosPropuesta)
            }.addOnSuccessListener {
                onSuccess()
            }.addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Error al aceptar la propuesta")
            }
        }
    }

    fun cerrarSesion(onLogout: () -> Unit) {
        auth.signOut()
        limpiarEstadoLocal()
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

    var listaResenasDelPrestador by mutableStateOf<List<ReviewItem>>(emptyList())
        private set

    var estaCargandoResenasPrestador by mutableStateOf(false)
        private set

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
}