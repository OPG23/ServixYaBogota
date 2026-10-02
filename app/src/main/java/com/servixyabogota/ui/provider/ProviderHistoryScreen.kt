package com.servixyabogota.ui.provider

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.layout.IntrinsicSize

// Modelos de datos para UI
data class TrabajoActivoUi(
    val id: String = "1",
    val clienteId: String = "",
    val nombreCliente: String = "Laura Gómez",
    val fotoCliente: String = "",
    val titulo: String = "Arreglo Fuga Lavamanos",
    val ubicacion: String = "Usaquén - Sector Calle 127",
    val estado: String = "ACEPTADO",
    val chatId: String = ""
)

data class TrabajoHistorialUi(
    val id: String,
    val titulo: String,
    val fecha: String,
    val estado: String, // "COMPLETADO", "POSTULADO", "CANCELADO"
    val calificacion: Double? = null,
    val detalleEstado: String? = null
)

@Composable
fun ProviderHistoryScreen(
    onOpenChat: (trabajoId: String, chatId: String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Trabajos Aceptados, 1: Historial Completo

    // Datos por defecto (basados en el diseño exacto recibido)
    var trabajosActuales by remember {
        mutableStateOf<List<TrabajoActivoUi>>(
            listOf(
                TrabajoActivoUi(
                    id = "act_1",
                    nombreCliente = "Laura Gómez",
                    titulo = "Arreglo Fuga Lavamanos",
                    ubicacion = "Usaquén - Sector Calle 127",
                    estado = "ACEPTADO",
                    chatId = "act_1"
                )
            )
        )
    }

    var historialReciente by remember {
        mutableStateOf<List<TrabajoHistorialUi>>(
            listOf(
                TrabajoHistorialUi(
                    id = "hist_1",
                    titulo = "Cambio de Grifería",
                    fecha = "12/08/2026",
                    estado = "COMPLETADO",
                    calificacion = 5.0
                ),
                TrabajoHistorialUi(
                    id = "hist_2",
                    titulo = "Mantenimiento Calentador",
                    fecha = "10/08/2026",
                    estado = "POSTULADO",
                    detalleEstado = "En espera de selección"
                ),
                TrabajoHistorialUi(
                    id = "hist_3",
                    titulo = "Instalación Lavadora",
                    fecha = "05/08/2026",
                    estado = "COMPLETADO",
                    calificacion = 4.8
                )
            )
        )
    }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }

    // Manejador del ciclo de vida de los listeners de Firestore
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            onDispose { }
        } else {
            // 1. Cargar solicitudes asignadas/aceptadas
            val listenerSolicitudes = db.collection("solicitudes")
                .whereEqualTo("prestadorId", currentUserId)
                .whereIn("estado", listOf("ACEPTADO", "ACEPTADA", "EN_PROCESO"))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && !snapshot.isEmpty) {
                        val listaTemp = snapshot.documents.map { doc ->
                            TrabajoActivoUi(
                                id = doc.id,
                                clienteId = doc.getString("clienteId") ?: "",
                                nombreCliente = doc.getString("nombreCliente") ?: doc.getString("clienteNombre") ?: "Cliente",
                                fotoCliente = doc.getString("fotoCliente") ?: doc.getString("clienteFoto") ?: "",
                                titulo = doc.getString("titulo") ?: doc.getString("servicio") ?: doc.getString("categoria") ?: "Servicio Aceptado",
                                ubicacion = doc.getString("ubicacion") ?: doc.getString("barrio") ?: "Bogotá",
                                estado = doc.getString("estado") ?: "ACEPTADO",
                                chatId = doc.getString("chatId") ?: doc.id
                            )
                        }.distinctBy { it.id }
                        trabajosActuales = listaTemp
                    }
                }

            // 2. Cargar solicitudes finalizadas o postulaciones
            val listenerPropuestas = db.collectionGroup("propuestas")
                .whereEqualTo("prestadorId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && !snapshot.isEmpty) {
                        val listaHist = snapshot.documents.map { doc ->
                            val timestamp = try {
                                doc.getTimestamp("fecha") ?: doc.getTimestamp("fechaCreacion")
                            } catch (e: Exception) {
                                null
                            }

                            val fechaStr = if (timestamp != null) {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp.toDate())
                            } else "Reciente"

                            // Conversión segura de número para evitar ClassCastException
                            val calif = (doc.get("calificacion") as? Number)?.toDouble()
                                ?: (doc.get("rating") as? Number)?.toDouble()

                            TrabajoHistorialUi(
                                id = doc.id,
                                titulo = doc.getString("tituloSolicitud") ?: doc.getString("servicio") ?: "Servicio de Solicitud",
                                fecha = fechaStr,
                                estado = doc.getString("estado") ?: "POSTULADO",
                                calificacion = calif,
                                detalleEstado = if (doc.getString("estado") == "COMPLETADO") null else "En espera de selección"
                            )
                        }.distinctBy { it.id }

                        if (listaHist.isNotEmpty()) {
                            historialReciente = listaHist
                        }
                    }
                }

            onDispose {
                listenerSolicitudes.remove()
                listenerPropuestas.remove()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
    ) {
        // Encabezado superior con contador de activos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis Trabajos",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color(0xFF111827)
            )

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${trabajosActuales.size} Activo",
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Selector de Pestañas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .border(
                        width = if (selectedTab == 0) 1.5.dp else 1.dp,
                        color = if (selectedTab == 0) Color(0xFFFF8F00) else Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .clickable { selectedTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Trabajos Aceptados",
                    color = if (selectedTab == 0) Color(0xFFFF8F00) else Color.Gray,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .border(
                        width = if (selectedTab == 1) 1.5.dp else 1.dp,
                        color = if (selectedTab == 1) Color(0xFFFF8F00) else Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .clickable { selectedTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Historial Completo",
                    color = if (selectedTab == 1) Color(0xFFFF8F00) else Color.Gray,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contenido según pestaña
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedTab == 0) {
                // Sección: TRABAJOS ACTUALES
                item {
                    Text(
                        text = "TRABAJOS ACTUALES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (trabajosActuales.isEmpty()) {
                    item {
                        Text(
                            text = "No tienes trabajos activos en este momento.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(trabajosActuales, key = { "act_${it.id}" }) { trabajo ->
                        TrabajoActualCard(
                            job = trabajo,
                            onOpenChat = {
                                val chatIdToOpen = trabajo.chatId.ifBlank { trabajo.id }
                                onOpenChat(trabajo.id, chatIdToOpen)
                            }
                        )
                    }
                }

                // Sección: HISTORIAL RECIENTE
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HISTORIAL RECIENTE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(historialReciente, key = { "hist_rec_${it.id}" }) { itemHist ->
                    HistorialItemCard(item = itemHist)
                }
            } else {
                // Modo: Historial Completo
                item {
                    Text(
                        text = "TODOS LOS TRABAJOS Y POSTULACIONES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(historialReciente, key = { "hist_comp_${it.id}" }) { itemHist ->
                    HistorialItemCard(item = itemHist)
                }
            }
        }
    }
}

@Composable
private fun TrabajoActualCard(
    job: TrabajoActivoUi,
    onOpenChat: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Borde lateral verde
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2E7D32))
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC5CAE9)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (job.fotoCliente.isNotBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = job.fotoCliente),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = job.nombreCliente,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF111827)
                        )
                    }

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = job.estado.uppercase(),
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = job.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.ubicacion,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
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
private fun HistorialItemCard(
    item: TrabajoHistorialUi
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(115.dp)
            ) {
                val isCompletado = item.estado.uppercase() == "COMPLETADO"
                val badgeBg = if (isCompletado) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                val badgeTextColor = if (isCompletado) Color(0xFF2E7D32) else Color(0xFFE65100)

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.estado.uppercase(),
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.fecha,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (item.calificacion != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.calificacion.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF111827)
                        )
                    }
                } else if (!item.detalleEstado.isNullOrBlank()) {
                    Text(
                        text = item.detalleEstado,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}