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

    val chatsSolicitudes = remember {
        listOf(
            ChatItemUi(
                id = "sol_101",
                nombreCliente = "Carlos Mendoza",
                ultimoMensaje = "¿A qué hora podrías venir a revisar la fuga?",
                hora = "10:30 AM",
                noLeidos = 2,
                tituloSolicitud = "Plomería · Reparación Tubo PVC"
            ),
            ChatItemUi(
                id = "sol_102",
                nombreCliente = "Beatriz Gómez",
                ultimoMensaje = "Perfecto, acepto el presupuesto.",
                hora = "Ayer",
                noLeidos = 0,
                tituloSolicitud = "Electricidad · Instalación Lámparas"
            )
        )
    }

    val chatsDirectos = remember {
        listOf(
            ChatItemUi(
                id = "dir_201",
                nombreCliente = "Andrés López",
                ultimoMensaje = "Hola, vi tu perfil y me interesa una cotización.",
                hora = "09:15 AM",
                noLeidos = 1
            )
        )
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

        if (listaActual.isEmpty()) {
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