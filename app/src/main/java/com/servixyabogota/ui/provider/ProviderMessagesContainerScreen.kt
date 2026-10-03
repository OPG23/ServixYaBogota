package com.servixyabogota.ui.provider

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ChatItemUi(
    val id: String,
    val nombreCliente: String,
    val fotoCliente: String = "",
    val ultimoMensaje: String,
    val hora: String,
    val noLeidos: Int = 0,
    val tituloSolicitud: String? = null
)

// Helper para asignar emojis por categoría de servicio
private fun obtenerEmojiCategoria(categoria: String?): String {
    if (categoria.isNullOrBlank()) return "🛠️"
    val cat = categoria.lowercase()
    return when {
        cat.contains("plomer") || cat.contains("tuber") -> "🚰"
        cat.contains("pintur") -> "🎨"
        cat.contains("carpint") -> "🪵"
        cat.contains("electr") -> "⚡"
        cat.contains("limpiez") || cat.contains("aseo") -> "🧹"
        cat.contains("jardin") -> "🌱"
        cat.contains("cerraj") -> "🔑"
        cat.contains("mecanic") || cat.contains("auto") -> "🚗"
        cat.contains("flete") || cat.contains("mudanz") -> "🚚"
        cat.contains("aire") || cat.contains("acondic") -> "❄️"
        cat.contains("techo") || cat.contains("imperm") -> "🏠"
        cat.contains("gas") -> "🔥"
        cat.contains("electrodom") || cat.contains("lavadora") -> "🧺"
        cat.contains("remodel") || cat.contains("construc") -> "🏗️"
        else -> "🛠️"
    }
}

@Composable
fun ProviderMessagesContainerScreen(
    onOpenChat: (chat: ChatItemUi) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Solicitudes, 1: Contacto Directo

    var chatsSolicitudes by remember { mutableStateOf<List<ChatItemUi>>(emptyList()) }
    var chatsDirectos by remember { mutableStateOf<List<ChatItemUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }

    // Escuchador en tiempo real de Firestore para el prestador
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            isLoading = false
            onDispose { }
        }

        // 1. Escuchar Chats por Solicitudes (vía subcolección 'propuestas' y asignaciones directas)
        val listenerPropuestas = db.collectionGroup("propuestas")
            .whereEqualTo("prestadorId", currentUserId)
            .addSnapshotListener { snapshotPropuestas, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshotPropuestas == null || snapshotPropuestas.isEmpty) {
                    db.collection("solicitudes")
                        .whereEqualTo("prestadorId", currentUserId)
                        .get()
                        .addOnSuccessListener { snapshotSolicitudes ->
                            if (snapshotSolicitudes.isEmpty) {
                                chatsSolicitudes = emptyList()
                                isLoading = false
                            } else {
                                cargarDetallesSolicitudes(
                                    solicitudDocs = snapshotSolicitudes.documents,
                                    db = db,
                                    currentUserId = currentUserId
                                ) { lista ->
                                    chatsSolicitudes = lista
                                    isLoading = false
                                }
                            }
                        }
                        .addOnFailureListener {
                            chatsSolicitudes = emptyList()
                            isLoading = false
                        }
                    return@addSnapshotListener
                }

                val propuestasDocs = snapshotPropuestas.documents
                val solicitudesMap = mutableMapOf<String, DocumentSnapshot>()
                val totalPropuestas = propuestasDocs.size
                var procesadasPropuestas = 0

                for (propDoc in propuestasDocs) {
                    val solicitudRef = propDoc.reference.parent.parent
                    if (solicitudRef != null) {
                        solicitudRef.get().addOnSuccessListener { solDoc ->
                            if (solDoc.exists()) {
                                solicitudesMap[solDoc.id] = solDoc
                            }
                            procesadasPropuestas++
                            if (procesadasPropuestas == totalPropuestas) {
                                cargarDetallesSolicitudes(
                                    solicitudDocs = solicitudesMap.values.toList(),
                                    db = db,
                                    currentUserId = currentUserId
                                ) { lista ->
                                    chatsSolicitudes = lista
                                    isLoading = false
                                }
                            }
                        }.addOnFailureListener {
                            procesadasPropuestas++
                            if (procesadasPropuestas == totalPropuestas) {
                                cargarDetallesSolicitudes(
                                    solicitudDocs = solicitudesMap.values.toList(),
                                    db = db,
                                    currentUserId = currentUserId
                                ) { lista ->
                                    chatsSolicitudes = lista
                                    isLoading = false
                                }
                            }
                        }
                    } else {
                        procesadasPropuestas++
                        if (procesadasPropuestas == totalPropuestas) {
                            cargarDetallesSolicitudes(
                                solicitudDocs = solicitudesMap.values.toList(),
                                db = db,
                                currentUserId = currentUserId
                            ) { lista ->
                                chatsSolicitudes = lista
                                isLoading = false
                            }
                        }
                    }
                }
            }

        // 2. Escuchar Chats Directos
        val listenerDirectos = db.collection("chats")
            .whereEqualTo("prestadorId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val listaTemp = mutableListOf<ChatItemUi>()
                    val totalDocs = snapshot.documents.size

                    if (totalDocs == 0) {
                        chatsDirectos = emptyList()
                        isLoading = false
                    } else {
                        var procesados = 0
                        for (doc in snapshot.documents) {
                            val chatId = doc.id
                            val clienteId = doc.getString("clienteId") ?: ""
                            val ultimoMsg = doc.getString("ultimoMensaje") ?: "Contacto directo"
                            val timestamp = doc.getTimestamp("fechaUltimoMensaje")
                            val horaFormateada = formatearFecha(timestamp)

                            val noLeidosDirecto = (doc.getLong("noLeidosPrestador")
                                ?: doc.getLong("noLeidos_$currentUserId")
                                ?: doc.getLong("noLeidos") ?: 0L).toInt()

                            fun agregarChatDirecto(nombreCliente: String, fotoCliente: String, noLeidosCount: Int) {
                                listaTemp.add(
                                    ChatItemUi(
                                        id = chatId,
                                        nombreCliente = nombreCliente,
                                        fotoCliente = fotoCliente,
                                        ultimoMensaje = ultimoMsg,
                                        hora = horaFormateada,
                                        noLeidos = noLeidosCount,
                                        tituloSolicitud = null
                                    )
                                )
                                procesados++
                                if (procesados == totalDocs) {
                                    chatsDirectos = listaTemp.distinctBy { it.id }
                                    isLoading = false
                                }
                            }

                            // Consultar subcolección de mensajes no leídos para chats directos
                            db.collection("chats").document(chatId)
                                .collection("mensajes")
                                .whereEqualTo("leido", false)
                                .get()
                                .addOnSuccessListener { msgSnap ->
                                    val noLeidosSubcoleccion = msgSnap.documents.count { msgDoc ->
                                        val emisorId = msgDoc.getString("emisorId") ?: msgDoc.getString("remitenteId") ?: ""
                                        val receptorId = msgDoc.getString("receptorId") ?: ""
                                        receptorId == currentUserId || (emisorId.isNotBlank() && emisorId != currentUserId)
                                    }

                                    val noLeidosFinal = maxOf(noLeidosDirecto, noLeidosSubcoleccion)

                                    if (clienteId.isNotBlank()) {
                                        db.collection("usuarios").document(clienteId).get()
                                            .addOnSuccessListener { clientDoc ->
                                                val nombreCliente = clientDoc.getString("nombreCompleto")
                                                    ?: clientDoc.getString("nombre")
                                                    ?: "Cliente Directo"

                                                val fotoCliente = clientDoc.getString("fotoUrl")
                                                    ?: clientDoc.getString("fotoPerfilUrl")
                                                    ?: clientDoc.getString("foto")
                                                    ?: ""

                                                agregarChatDirecto(nombreCliente, fotoCliente, noLeidosFinal)
                                            }
                                            .addOnFailureListener {
                                                agregarChatDirecto("Cliente Directo", "", noLeidosFinal)
                                            }
                                    } else {
                                        agregarChatDirecto("Cliente Directo", "", noLeidosFinal)
                                    }
                                }
                                .addOnFailureListener {
                                    if (clienteId.isNotBlank()) {
                                        db.collection("usuarios").document(clienteId).get()
                                            .addOnSuccessListener { clientDoc ->
                                                val nombreCliente = clientDoc.getString("nombreCompleto")
                                                    ?: clientDoc.getString("nombre")
                                                    ?: "Cliente Directo"

                                                val fotoCliente = clientDoc.getString("fotoUrl")
                                                    ?: clientDoc.getString("fotoPerfilUrl")
                                                    ?: clientDoc.getString("foto")
                                                    ?: ""

                                                agregarChatDirecto(nombreCliente, fotoCliente, noLeidosDirecto)
                                            }
                                            .addOnFailureListener {
                                                agregarChatDirecto("Cliente Directo", "", noLeidosDirecto)
                                            }
                                    } else {
                                        agregarChatDirecto("Cliente Directo", "", noLeidosDirecto)
                                    }
                                }
                        }
                    }
                } else {
                    isLoading = false
                }
            }

        onDispose {
            listenerPropuestas.remove()
            listenerDirectos.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
    ) {
        Text(
            text = "Mensajes",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = Color(0xFF111827),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.White,
            contentColor = Color(0xFFFF8F00),
            divider = { HorizontalDivider(color = Color(0xFFE5E7EB)) }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Text(
                        text = "Por Solicitudes (${chatsSolicitudes.size})",
                        fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Text(
                        text = "Contacto Directo (${chatsDirectos.size})",
                        fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            )
        }

        val listaActual = if (selectedSubTab == 0) chatsSolicitudes else chatsDirectos

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF8F00))
            }
        } else if (listaActual.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedSubTab == 0) "No tienes chats sobre solicitudes activas." else "No tienes mensajes directos.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listaActual, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { onOpenChat(chat) }
                    )
                }
            }
        }
    }
}

private fun cargarDetallesSolicitudes(
    solicitudDocs: List<DocumentSnapshot>,
    db: FirebaseFirestore,
    currentUserId: String,
    onResult: (List<ChatItemUi>) -> Unit
) {
    if (solicitudDocs.isEmpty()) {
        onResult(emptyList())
        return
    }

    val listaTemp = mutableListOf<ChatItemUi>()
    var procesados = 0
    val totalDocs = solicitudDocs.size

    for (doc in solicitudDocs) {
        val chatId = doc.id
        val clienteId = doc.getString("clienteId") ?: ""
        val ultimoMsg = doc.getString("ultimoMensaje") ?: "Solicitud iniciada"
        val timestamp = doc.getTimestamp("fechaUltimoMensaje")
        val horaFormateada = formatearFecha(timestamp)
        val categoriaRaw = doc.getString("categoria")
            ?: doc.getString("servicio")
            ?: doc.getString("titulo")
            ?: "Solicitud de Servicio"

        val emoji = obtenerEmojiCategoria(categoriaRaw)
        val tituloServicio = "$emoji $categoriaRaw"

        val noLeidosDirecto = (doc.getLong("noLeidosPrestador")
            ?: doc.getLong("noLeidos_$currentUserId")
            ?: doc.getLong("noLeidos") ?: 0L).toInt()

        fun agregarItem(nombreCliente: String, fotoCliente: String, noLeidosCount: Int) {
            listaTemp.add(
                ChatItemUi(
                    id = chatId,
                    nombreCliente = nombreCliente,
                    fotoCliente = fotoCliente,
                    ultimoMensaje = ultimoMsg,
                    hora = horaFormateada,
                    noLeidos = noLeidosCount,
                    tituloSolicitud = tituloServicio
                )
            )
            procesados++
            if (procesados == totalDocs) {
                onResult(listaTemp.distinctBy { it.id })
            }
        }

        // Consultar mensajes no leídos en la subcolección
        db.collection("solicitudes").document(chatId)
            .collection("mensajes")
            .whereEqualTo("leido", false)
            .get()
            .addOnSuccessListener { msgSnap ->
                val noLeidosSubcoleccion = msgSnap.documents.count { msgDoc ->
                    val emisorId = msgDoc.getString("emisorId") ?: msgDoc.getString("remitenteId") ?: ""
                    val receptorId = msgDoc.getString("receptorId") ?: ""
                    receptorId == currentUserId || (emisorId.isNotBlank() && emisorId != currentUserId)
                }

                val noLeidosFinal = maxOf(noLeidosDirecto, noLeidosSubcoleccion)

                if (clienteId.isNotBlank()) {
                    db.collection("usuarios").document(clienteId).get()
                        .addOnSuccessListener { clientDoc ->
                            val nombreCliente = clientDoc.getString("nombreCompleto")
                                ?: clientDoc.getString("nombre")
                                ?: "Cliente"

                            val fotoCliente = clientDoc.getString("fotoUrl")
                                ?: clientDoc.getString("fotoPerfilUrl")
                                ?: clientDoc.getString("foto")
                                ?: ""

                            agregarItem(nombreCliente, fotoCliente, noLeidosFinal)
                        }
                        .addOnFailureListener {
                            agregarItem("Cliente", "", noLeidosFinal)
                        }
                } else {
                    agregarItem("Cliente", "", noLeidosFinal)
                }
            }
            .addOnFailureListener {
                if (clienteId.isNotBlank()) {
                    db.collection("usuarios").document(clienteId).get()
                        .addOnSuccessListener { clientDoc ->
                            val nombreCliente = clientDoc.getString("nombreCompleto")
                                ?: clientDoc.getString("nombre")
                                ?: "Cliente"

                            val fotoCliente = clientDoc.getString("fotoUrl")
                                ?: clientDoc.getString("fotoPerfilUrl")
                                ?: clientDoc.getString("foto")
                                ?: ""

                            agregarItem(nombreCliente, fotoCliente, noLeidosDirecto)
                        }
                        .addOnFailureListener {
                            agregarItem("Cliente", "", noLeidosDirecto)
                        }
                } else {
                    agregarItem("Cliente", "", noLeidosDirecto)
                }
            }
    }
}

private fun formatearFecha(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val fecha = timestamp.toDate()
    val ahora = Calendar.getInstance()
    val calFecha = Calendar.getInstance().apply { time = fecha }

    return if (ahora.get(Calendar.YEAR) == calFecha.get(Calendar.YEAR) &&
        ahora.get(Calendar.DAY_OF_YEAR) == calFecha.get(Calendar.DAY_OF_YEAR)) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(fecha)
    } else if (ahora.get(Calendar.YEAR) == calFecha.get(Calendar.YEAR) &&
        ahora.get(Calendar.DAY_OF_YEAR) - calFecha.get(Calendar.DAY_OF_YEAR) == 1) {
        "Ayer"
    } else {
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(fecha)
    }
}

@Composable
private fun ChatListItem(
    chat: ChatItemUi,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (chat.tituloSolicitud != null) Color(0xFFFFF3E0) else Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                if (chat.fotoCliente.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = chat.fotoCliente),
                        contentDescription = "Foto de ${chat.nombreCliente}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val inicial = chat.nombreCliente.trim().take(1).uppercase()
                    if (inicial.isNotBlank() && inicial[0].isLetter()) {
                        Text(
                            text = inicial,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (chat.tituloSolicitud != null) Color(0xFFFF8F00) else Color(0xFF1976D2)
                        )
                    } else {
                        Icon(
                            imageVector = if (chat.tituloSolicitud != null) Icons.Default.Assignment else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (chat.tituloSolicitud != null) Color(0xFFFF8F00) else Color(0xFF1976D2),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (chat.tituloSolicitud != null) {
                    Text(
                        text = chat.tituloSolicitud,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8F00),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.nombreCliente,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = chat.hora,
                        fontSize = 11.sp,
                        color = if (chat.noLeidos > 0) Color(0xFFFF8F00) else Color.Gray,
                        fontWeight = if (chat.noLeidos > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.ultimoMensaje,
                        fontSize = 13.sp,
                        color = if (chat.noLeidos > 0) Color(0xFF111827) else Color.Gray,
                        fontWeight = if (chat.noLeidos > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.noLeidos > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF8F00))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chat.noLeidos > 99) "99+" else chat.noLeidos.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}