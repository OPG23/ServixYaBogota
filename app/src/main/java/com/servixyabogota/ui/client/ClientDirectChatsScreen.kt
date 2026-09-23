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

// Modelo de datos Mock para Chats Directos
data class DirectChatMock(
    val id: String,
    val providerName: String,
    val providerPhoto: String,
    val category: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDirectChatsScreen(
    onBack: () -> Unit,
    onOpenChat: (chatId: String, providerName: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Mockup de chats directos iniciados desde el perfil de prestadores
    val mockChats = remember {
        listOf(
            DirectChatMock(
                id = "chat_1",
                providerName = "Carlos Mendoza",
                providerPhoto = "https://i.pravatar.cc/150?img=12",
                category = "Plomería",
                lastMessage = "Hola, claro que sí. Puedo pasar hoy a las 3:00 pm a revisar la fuga.",
                time = "10:42 AM",
                unreadCount = 2,
                isOnline = true
            ),
            DirectChatMock(
                id = "chat_2",
                providerName = "Ana María Gómez",
                providerPhoto = "https://i.pravatar.cc/150?img=47",
                category = "Electricidad",
                lastMessage = "¿A qué hora te quedaría bien que revise el tablero eléctrico?",
                time = "Ayer",
                unreadCount = 0,
                isOnline = false
            ),
            DirectChatMock(
                id = "chat_3",
                providerName = "Jorge Ramírez",
                providerPhoto = "https://i.pravatar.cc/150?img=33",
                category = "Cerrajería",
                lastMessage = "El costo del cambio de clave de la cerradura es de $80.000.",
                time = "18 Sep",
                unreadCount = 0,
                isOnline = true
            ),
            DirectChatMock(
                id = "chat_4",
                providerName = "Laura Restrepo",
                providerPhoto = "https://i.pravatar.cc/150?img=25",
                category = "Pintura",
                lastMessage = "Perfecto, te envío la cotización con los materiales incluidos.",
                time = "15 Sep",
                unreadCount = 0,
                isOnline = false
            )
        )
    }

    // Filtrado en tiempo real según el buscador
    val chatsFiltrados = remember(searchQuery) {
        if (searchQuery.isBlank()) mockChats
        else mockChats.filter {
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
                            text = "${mockChats.size} conversaciones",
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

            if (chatsFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron conversaciones.",
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
                            onClick = { onOpenChat(chat.id, chat.providerName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectChatItem(
    chat: DirectChatMock,
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
            // Foto de perfil con indicador de línea
            Box(modifier = Modifier.size(52.dp)) {
                AsyncImage(
                    model = chat.providerPhoto,
                    contentDescription = chat.providerName,
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

                // Último mensaje y badge de mensajes no leídos
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