package com.servixyabogota.ui.provider

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

// Modelo Mock para el listado de chats recibidos por el prestador
data class ProviderDirectChatMock(
    val id: String,
    val clientName: String,
    val clientPhoto: String,
    val serviceRequested: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDirectChatsScreen(
    onBack: (() -> Unit)? = null, // Opcional si es un tab principal
    onOpenChat: (chatId: String, clientName: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Mockup de mensajes recibidos de clientes
    val mockChats = remember {
        listOf(
            ProviderDirectChatMock(
                id = "chat_directo_1",
                clientName = "María Fernanda",
                clientPhoto = "https://i.pravatar.cc/150?img=5",
                serviceRequested = "Consulta Plomería",
                lastMessage = "¿Hola! Quería saber si tienes disponibilidad para mañana en la mañana?",
                time = "10:45 AM",
                unreadCount = 1,
                isOnline = true
            ),
            ProviderDirectChatMock(
                id = "chat_directo_2",
                clientName = "Andrés Cepeda",
                clientPhoto = "https://i.pravatar.cc/150?img=8",
                serviceRequested = "Mantenimiento General",
                lastMessage = "Muchas gracias por la información, te confirmo más tarde.",
                time = "Ayer",
                unreadCount = 0,
                isOnline = false
            ),
            ProviderDirectChatMock(
                id = "chat_directo_3",
                clientName = "Camila Ruiz",
                clientPhoto = "https://i.pravatar.cc/150?img=9",
                serviceRequested = "Reparación Fuga",
                lastMessage = "¿Podrías enviarme una estimación de precio?",
                time = "20 Sep",
                unreadCount = 0,
                isOnline = true
            )
        )
    }

    val chatsFiltrados = remember(searchQuery) {
        if (searchQuery.isBlank()) mockChats
        else mockChats.filter {
            it.clientName.contains(searchQuery, ignoreCase = true) ||
                    it.serviceRequested.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mensajes de Clientes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${mockChats.size} conversaciones activas",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color(0xFF0F172A)
                            )
                        }
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
                placeholder = { Text("Buscar cliente o mensaje...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
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

            if (chatsFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tienes mensajes directos por el momento.", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatsFiltrados, key = { it.id }) { chat ->
                        ProviderChatItem(
                            chat = chat,
                            onClick = { onOpenChat(chat.id, chat.clientName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderChatItem(
    chat: ProviderDirectChatMock,
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
            // Foto de perfil del Cliente
            Box(modifier = Modifier.size(52.dp)) {
                AsyncImage(
                    model = chat.clientPhoto,
                    contentDescription = chat.clientName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                )
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

            // Información del mensaje
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.clientName,
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

                Text(
                    text = "Cliente • ${chat.serviceRequested}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(4.dp))

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