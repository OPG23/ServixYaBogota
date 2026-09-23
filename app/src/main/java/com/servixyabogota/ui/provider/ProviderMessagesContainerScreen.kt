package com.servixyabogota.ui.provider

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ChatItemUi(
    val id: String,
    val nombreCliente: String,
    val ultimoMensaje: String,
    val hora: String,
    val noLeidos: Int = 0,
    val tituloSolicitud: String? = null
)

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

        // 1. Escuchar Chats por Solicitudes
        val listenerSolicitudes = db.collection("solicitudes")
            .whereEqualTo("prestadorId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val listaTemp = mutableListOf<ChatItemUi>()
                    val totalDocs = snapshot.documents.size

                    if (totalDocs == 0) {
                        chatsSolicitudes = emptyList()
                        isLoading = false
                    } else {
                        var procesados = 0
                        for (doc in snapshot.documents) {
                            val chatId = doc.id
                            val clienteId = doc.getString("clienteId") ?: ""
                            val ultimoMsg = doc.getString("ultimoMensaje") ?: "Solicitud iniciada"
                            val timestamp = doc.getTimestamp("fechaUltimoMensaje")
                            val horaFormateada = formatearFecha(timestamp)
                            val tituloServicio = doc.getString("titulo")
                                ?: doc.getString("categoria")
                                ?: doc.getString("servicio")
                                ?: "Solicitud de Servicio"

                            if (clienteId.isNotBlank()) {
                                db.collection("usuarios").document(clienteId).get()
                                    .addOnSuccessListener { clientDoc ->
                                        val nombreCliente = clientDoc.getString("nombreCompleto")
                                            ?: clientDoc.getString("nombre")
                                            ?: "Cliente"

                                        listaTemp.add(
                                            ChatItemUi(
                                                id = chatId,
                                                nombreCliente = nombreCliente,
                                                ultimoMensaje = ultimoMsg,
                                                hora = horaFormateada,
                                                noLeidos = 0,
                                                tituloSolicitud = tituloServicio
                                            )
                                        )
                                        procesados++
                                        if (procesados == totalDocs) {
                                            chatsSolicitudes = listaTemp
                                            isLoading = false
                                        }
                                    }
                                    .addOnFailureListener {
                                        procesados++
                                        if (procesados == totalDocs) {
                                            chatsSolicitudes = listaTemp
                                            isLoading = false
                                        }
                                    }
                            } else {
                                procesados++
                                if (procesados == totalDocs) {
                                    chatsSolicitudes = listaTemp
                                    isLoading = false
                                }
                            }
                        }
                    }
                } else {
                    isLoading = false
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

                            if (clienteId.isNotBlank()) {
                                db.collection("usuarios").document(clienteId).get()
                                    .addOnSuccessListener { clientDoc ->
                                        val nombreCliente = clientDoc.getString("nombreCompleto")
                                            ?: clientDoc.getString("nombre")
                                            ?: "Cliente Directo"

                                        listaTemp.add(
                                            ChatItemUi(
                                                id = chatId,
                                                nombreCliente = nombreCliente,
                                                ultimoMensaje = ultimoMsg,
                                                hora = horaFormateada,
                                                noLeidos = 0,
                                                tituloSolicitud = null
                                            )
                                        )
                                        procesados++
                                        if (procesados == totalDocs) {
                                            chatsDirectos = listaTemp
                                            isLoading = false
                                        }
                                    }
                                    .addOnFailureListener {
                                        procesados++
                                        if (procesados == totalDocs) {
                                            chatsDirectos = listaTemp
                                            isLoading = false
                                        }
                                    }
                            } else {
                                procesados++
                                if (procesados == totalDocs) {
                                    chatsDirectos = listaTemp
                                    isLoading = false
                                }
                            }
                        }
                    }
                } else {
                    isLoading = false
                }
            }

        onDispose {
            listenerSolicitudes.remove()
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (chat.tituloSolicitud != null) Color(0xFFFFF3E0) else Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (chat.tituloSolicitud != null) Icons.Default.Assignment else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (chat.tituloSolicitud != null) Color(0xFFFF8F00) else Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (chat.tituloSolicitud != null) {
                    Text(
                        text = chat.tituloSolicitud,
                        fontSize = 11.sp,
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
                        color = Color.Gray
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
                        fontWeight = if (chat.noLeidos > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.noLeidos > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF8F00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.noLeidos.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
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