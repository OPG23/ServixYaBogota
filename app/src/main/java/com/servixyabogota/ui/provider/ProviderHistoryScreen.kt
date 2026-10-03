package com.servixyabogota.ui.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
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

data class TrabajoItemUI(
    val id: String,
    val clienteId: String = "",
    val clienteNombre: String,
    val titulo: String,
    val localidad: String,
    val direccion: String = "",
    val estado: String,
    val fecha: String,
    val calificacion: Float? = null,
    val subtituloEstado: String? = null
)

@Composable
fun ProviderHistoryScreen(
    trabajosActivos: List<TrabajoItemUI>,
    historialReciente: List<TrabajoItemUI>,
    estaCargando: Boolean = false,
    onVerDetalleSolicitud: (String) -> Unit = {},
    onAbrirChatCliente: (solicitudId: String, clienteId: String) -> Unit = { _, _ -> }
) {
    var tabSeleccionada by remember { mutableIntStateOf(0) }

    if (estaCargando && trabajosActivos.isEmpty() && historialReciente.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF8F00))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // --- CABECERA Y TABS ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis Trabajos",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color(0xFF111827)
                    )

                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "${trabajosActivos.size} Activo",
                            color = Color(0xFF15803D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { tabSeleccionada = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Trabajos Aceptados",
                                fontSize = 14.sp,
                                fontWeight = if (tabSeleccionada == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (tabSeleccionada == 0) Color(0xFFFF8F00) else Color(0xFF6B7280)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (tabSeleccionada == 0) Color(0xFFFF8F00) else Color.Transparent)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { tabSeleccionada = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Historial Completo",
                                fontSize = 14.sp,
                                fontWeight = if (tabSeleccionada == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (tabSeleccionada == 1) Color(0xFFFF8F00) else Color(0xFF6B7280)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (tabSeleccionada == 1) Color(0xFFFF8F00) else Color.Transparent)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
        }

        // --- TRABAJOS ACTUALES ---
        item {
            Text(
                text = "TRABAJOS ACTUALES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        if (trabajosActivos.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No tienes trabajos activos en este momento.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(
                items = trabajosActivos,
                key = { it.id }
            ) { trabajo ->
                TrabajoActualCard(
                    trabajo = trabajo,
                    onAbrirChat = { onAbrirChatCliente(trabajo.id, trabajo.clienteId) },
                    onVerDetalle = { onVerDetalleSolicitud(trabajo.id) }
                )
            }
        }

        // --- HISTORIAL RECIENTE ---
        item {
            Text(
                text = "HISTORIAL RECIENTE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        if (historialReciente.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Aún no tienes historial de postulaciones o trabajos.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(
                items = historialReciente,
                key = { it.id }
            ) { trabajo ->
                TrabajoHistorialCard(
                    trabajo = trabajo,
                    onClick = { onVerDetalleSolicitud(trabajo.id) }
                )
            }
        }
    }
}

@Composable
private fun TrabajoActualCard(
    trabajo: TrabajoItemUI,
    onAbrirChat: () -> Unit,
    onVerDetalle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onVerDetalle() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF16A34A))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC7D2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = trabajo.clienteNombre.take(1),
                                color = Color(0xFF3730A3),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = trabajo.clienteNombre,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF111827)
                        )
                    }

                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = trabajo.estado,
                            color = Color(0xFF15803D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = trabajo.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF111827),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${trabajo.localidad}${if (trabajo.direccion.isNotBlank()) " - ${trabajo.direccion}" else ""}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onAbrirChat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Abrir Chat Directo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TrabajoHistorialCard(
    trabajo: TrabajoItemUI,
    onClick: () -> Unit
) {
    val (badgeBg, badgeText) = when (trabajo.estado) {
        "COMPLETADO" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "POSTULADO" -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
        else -> Color(0xFFF3F4F6) to Color(0xFF4B5563)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = trabajo.estado,
                            color = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trabajo.fecha,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = trabajo.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!trabajo.subtituloEstado.isNullOrBlank()) {
                    Text(
                        text = trabajo.subtituloEstado,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (trabajo.calificacion != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = trabajo.calificacion.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF111827)
                    )
                }
            }
        }
    }
}