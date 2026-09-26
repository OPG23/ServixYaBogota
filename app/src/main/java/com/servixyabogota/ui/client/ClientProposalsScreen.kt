package com.servixyabogota.ui.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.servixyabogota.data.model.Solicitud

@Composable
fun ClientProposalsScreen(
    clientViewModel: ClientViewModel,
    onNavigateTab: (String) -> Unit = {},
    onVerPropuestasClick: (String) -> Unit = {},
    onVerChatClick: (String) -> Unit = {},
    onVerChatsDirectosClick: () -> Unit = {}
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val misSolicitudesState = remember(currentUserId) { clientViewModel.getMisSolicitudes(currentUserId) }
    val listaSolicitudes by misSolicitudesState.collectAsState()

    // Filtro para incluir únicamente solicitudes en estado activo/pendiente/en proceso
    val solicitudesActivas = remember(listaSolicitudes) {
        val estadosActivos = setOf("PENDIENTE", "PUBLICADA", "EN_PROCESO")
        listaSolicitudes.filter { solicitud ->
            solicitud.estado.uppercase() in estadosActivos
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            ClientBottomNavigation(
                selectedTab = "Propuestas",
                onTabSelected = onNavigateTab
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Propuestas y Chats",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Acceso a Chats Directos
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVerChatsDirectosClick() }
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Chat,
                                contentDescription = "Chats Directos",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Chats Directos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Conversaciones iniciadas desde perfiles",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "Solicitudes en curso",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )

            if (solicitudesActivas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes solicitudes activas publicadas.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(solicitudesActivas, key = { it.id }) { solicitud ->
                        TarjetaSolicitudCliente(
                            solicitud = solicitud,
                            onVerPropuestasClick = { onVerPropuestasClick(solicitud.id) },
                            onVerChatClick = { onVerChatClick(solicitud.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitudCliente(
    solicitud: Solicitud,
    onVerPropuestasClick: () -> Unit,
    onVerChatClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = solicitud.categoria,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                val esProceso = solicitud.estado.equals("EN_PROCESO", ignoreCase = true)
                Surface(
                    color = if (esProceso) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = solicitud.estado.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esProceso) Color(0xFF16A34A) else Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = solicitud.detalleProblema,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 2
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${solicitud.localidad}, Bogotá",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                }

                // Muestra la cantidad acumulada actual
                val conteoPropuestas = solicitud.cantidadPropuestas
                Text(
                    text = "$conteoPropuestas ${if (conteoPropuestas == 1) "Prestador interesado" else "Prestadores interesados"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2563EB)
                )
            }

            Button(
                onClick = {
                    if (solicitud.estado.equals("EN_PROCESO", ignoreCase = true)) {
                        onVerChatClick()
                    } else {
                        onVerPropuestasClick()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (solicitud.estado.equals("EN_PROCESO", ignoreCase = true)) "Abrir Chat de Servicio" else "Ver Propuestas Recibidas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}