package com.servixyabogota.ui.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Modelo de datos para Chats Directos
data class DirectChatUi(
    val id: String,
    val providerName: String,
    val providerPhoto: String,
    val category: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val providerId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDirectChatsScreen(
    onBack: () -> Unit,
    onOpenChat: (chatId: String, providerName: String, providerId: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var realChats by remember { mutableStateOf<List<DirectChatUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }

    // Escuchar chats directos del cliente en tiempo real
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            isLoading = false
            onDispose { }
        } else {
            val listener = db.collection("chats")
                .whereEqualTo("clienteId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val documents = snapshot.documents
                        if (documents.isEmpty()) {
                            realChats = emptyList()
                            isLoading = false
                        } else {
                            val tempList = mutableListOf<DirectChatUi>()
                            var processedCount = 0

                            for (doc in documents) {
                                val chatId = doc.id
                                val prestadorId = doc.getString("prestadorId") ?: ""
                                val ultimoMsg = doc.getString("ultimoMensaje") ?: "Conversación iniciada"
                                val timestamp = doc.getTimestamp("fechaUltimoMensaje")
                                val horaFormateada = formatearFecha(timestamp)

                                if (prestadorId.isNotBlank()) {
                                    // Obtener la información del prestador
                                    db.collection("usuarios").document(prestadorId).get()
                                        .addOnSuccessListener { providerDoc ->
                                            val nombrePrestador = providerDoc.getString("nombreCompleto")
                                                ?: providerDoc.getString("nombre")
                                                ?: "Prestador"
                                            val fotoPrestador = providerDoc.getString("fotoUrl")
                                                ?: providerDoc.getString("foto")
                                                ?: ""
                                            val categoriaPrestador = providerDoc.getString("profesion")
                                                ?: providerDoc.getString("categoria")
                                                ?: providerDoc.getString("especialidad")
                                                ?: "Servicio"

                                            tempList.add(
                                                DirectChatUi(
                                                    id = chatId,
                                                    providerName = nombrePrestador,
                                                    providerPhoto = fotoPrestador,
                                                    category = categoriaPrestador,
                                                    lastMessage = ultimoMsg,
                                                    time = horaFormateada,
                                                    unreadCount = 0,
                                                    isOnline = false,
                                                    providerId = prestadorId
                                                )
                                            )
                                            processedCount++
                                            if (processedCount == documents.size) {
                                                realChats = tempList
                                                isLoading = false
                                            }
                                        }
                                        .addOnFailureListener {
                                            processedCount++
                                            if (processedCount == documents.size) {
                                                realChats = tempList
                                                isLoading = false
                                            }
                                        }
                                } else {
                                    processedCount++
                                    if (processedCount == documents.size) {
                                        realChats = tempList
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
                listener.remove()
            }
        }
    }

    // Filtrado en tiempo real según el buscador
    val chatsFiltrados = remember(searchQuery, realChats) {
        if (searchQuery.isBlank()) realChats
        else realChats.filter {
            it.providerName.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chats Directos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${realChats.size} conversaciones",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar conversación o prestador...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (chatsFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No tienes conversaciones directas activas." else "No se encontraron conversaciones.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatsFiltrados, key = { it.id }) { chat ->
                        DirectChatItem(
                            chat = chat,
                            onClick = { onOpenChat(chat.id, chat.providerName, chat.providerId) }
                        )
                    }
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
private fun DirectChatItem(
    chat: DirectChatUi,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de perfil
            Box(modifier = Modifier.size(52.dp)) {
                if (chat.providerPhoto.isNotBlank()) {
                    AsyncImage(
                        model = chat.providerPhoto,
                        contentDescription = chat.providerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del chat
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.providerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = chat.time,
                        fontSize = 12.sp,
                        color = if (chat.unreadCount > 0) Color(0xFF2563EB) else Color(0xFF94A3B8),
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Categoría
                Text(
                    text = chat.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Último mensaje y contador de no leídos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        fontSize = 13.sp,
                        color = if (chat.unreadCount > 0) Color(0xFF1E293B) else Color(0xFF64748B),
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}