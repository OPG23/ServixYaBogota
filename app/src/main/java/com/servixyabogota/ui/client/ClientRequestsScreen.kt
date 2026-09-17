package com.servixyabogota.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.servixyabogota.data.model.Solicitud
import java.util.Date
import java.util.concurrent.TimeUnit

// ==========================================
// SCREEN PRINCIPAL: MIS SOLICITUDES
// ==========================================

@Composable
fun ClientRequestsScreen(
    viewModel: ClientViewModel,
    onNuevaSolicitudClick: () -> Unit = {},
    onEditarClick: (Solicitud) -> Unit = {},
    onCancelarClick: (Solicitud) -> Unit = {},
    onVerChatClick: (Solicitud) -> Unit = {},
    onNavigateTab: (String) -> Unit = {}
) {
    var filtroSeleccionado by remember { mutableStateOf("Abiertas") }
    var solicitudAEditar by remember { mutableStateOf<Solicitud?>(null) }
    var solicitudACancelar by remember { mutableStateOf<Solicitud?>(null) }

    // 1. SI SE HACE CLIC EN EDITAR, MUESTRA CREATEREQUESTSCREEN A PANTALLA COMPLETA
    val solicitudParaEditar = solicitudAEditar
    if (solicitudParaEditar != null) {
        CreateRequestScreen(
            solicitudToEdit = solicitudParaEditar,
            onBack = { solicitudAEditar = null },
            onGuardarEdicion = { id, cat, det, loc, dir, urg, archivos ->
                viewModel.actualizarSolicitud(
                    solicitudId = id,
                    clienteId = solicitudParaEditar.clienteId, // Pasa el clienteId correcto
                    categoria = cat,
                    detalle = det,
                    urgencia = urg,
                    direccion = dir,
                    localidad = loc,
                    archivos = archivos,
                    onSuccess = { solicitudAEditar = null },
                    onError = { /* Manejar error si ocurre */ }
                )
            }
        )
        return
    }

    // Obtener UID del usuario autenticado actual
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // Obtener el Flow/StateFlow desde el ViewModel
    val listaSolicitudes by remember(currentUserId) {
        viewModel.getMisSolicitudes(currentUserId)
    }.collectAsState(initial = emptyList())

    // Clasificación de listas según el estado
    val abiertas = remember(listaSolicitudes) {
        listaSolicitudes.filter { esEstadoPendiente(it.estado) }
    }
    val enProceso = remember(listaSolicitudes) {
        listaSolicitudes.filter { esEstadoEnProceso(it.estado) }
    }
    val historial = remember(listaSolicitudes) {
        listaSolicitudes.filter {
            !esEstadoPendiente(it.estado) && !esEstadoEnProceso(it.estado)
        }
    }

    val solicitudesFiltradas = when (filtroSeleccionado) {
        "Abiertas" -> abiertas
        "En Proceso" -> enProceso
        else -> historial
    }

    // 2. INTERFAZ PRINCIPAL CUANDO NO SE ESTÁ EDITANDO
    Scaffold(
        bottomBar = {
            ClientBottomNavigation(
                selectedTab = "Solicitudes",
                onTabSelected = onNavigateTab
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ENCABEZADO
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mis Solicitudes",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Button(
                            onClick = onNuevaSolicitudClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nueva Solicitud",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // TABS DE FILTRO (Abiertas, En Proceso, Historial)
                item {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val tabs = listOf(
                                "Abiertas" to "Abiertas (${abiertas.size})",
                                "En Proceso" to "En Proceso (${enProceso.size})",
                                "Historial" to "Historial (${historial.size})"
                            )

                            tabs.forEach { (key, label) ->
                                val isSelected = filtroSeleccionado == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                                        .clickable { filtroSeleccionado = key }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }

                // Mensaje estado vacío
                if (solicitudesFiltradas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tienes solicitudes en esta categoría",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // TARJETAS DE SOLICITUDES
                    items(solicitudesFiltradas, key = { it.id }) { solicitud ->
                        SolicitudCardItem(
                            solicitud = solicitud,
                            onEditarClick = { solicitudAEditar = solicitud },
                            onCancelarClick = { solicitudACancelar = solicitud },
                            onVerChatClick = { onVerChatClick(solicitud) }
                        )
                    }
                }
            }

            // DIÁLOGO DE CANCELACIÓN
            solicitudACancelar?.let { solicitud ->
                AlertDialog(
                    onDismissRequest = { solicitudACancelar = null },
                    title = { Text("¿Cancelar solicitud?") },
                    text = { Text("La solicitud pasará al historial como cancelada.") },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            onClick = {
                                viewModel.cancelarSolicitud(
                                    solicitudId = solicitud.id,
                                    onSuccess = { solicitudACancelar = null },
                                    onError = { /* Mostrar error */ }
                                )
                            }
                        ) { Text("Sí, cancelar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { solicitudACancelar = null }) { Text("Volver") }
                    }
                )
            }
        }
    }
}

// ==========================================
// COMPOSABLE: TARJETA DE SOLICITUD
// ==========================================

@Composable
private fun SolicitudCardItem(
    solicitud: Solicitud,
    onEditarClick: () -> Unit,
    onCancelarClick: () -> Unit,
    onVerChatClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = obtenerIconoCategoria(solicitud.categoria),
                            contentDescription = null,
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = solicitud.categoria.ifBlank { "General" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                val estadoTextoLimpio = obtenerEstadoTexto(solicitud.estado)
                val (estadoBg, estadoTextColor, estadoTexto) = when {
                    esEstadoPendiente(solicitud.estado) -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "ABIERTA")
                    esEstadoEnProceso(solicitud.estado) -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "EN PROCESO")
                    estadoTextoLimpio.contains("CANCEL") -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), "CANCELADA")
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "COMPLETADA")
                }

                Surface(
                    color = estadoBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = estadoTexto,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = estadoTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = solicitud.detalleProblema.ifBlank { "Solicitud de ${solicitud.categoria}" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

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
                    text = if (solicitud.direccion.isNotBlank()) solicitud.direccion else solicitud.localidad,
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            }

            Text(
                text = calcularTiempoTranscurrido(solicitud.fechaCreacion),
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )

            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (esEstadoPendiente(solicitud.estado)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditarClick,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Editar",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onCancelarClick,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancelar",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (esEstadoEnProceso(solicitud.estado)) {
                Button(
                    onClick = onVerChatClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver Chat / Prestador",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// FUNCIONES AUXILIARES
// ==========================================

private fun obtenerEstadoTexto(estado: Any?): String {
    return when (estado) {
        is Enum<*> -> estado.name.uppercase()
        is String -> estado.uppercase()
        else -> estado?.toString()?.uppercase() ?: ""
    }
}

private fun esEstadoPendiente(estado: Any?): Boolean {
    val texto = obtenerEstadoTexto(estado)
    return texto.isBlank() || texto == "PENDIENTE" || texto == "ABIERTA"
}

private fun esEstadoEnProceso(estado: Any?): Boolean {
    val texto = obtenerEstadoTexto(estado)
    return texto == "EN_PROCESO" || texto == "ENPROCESO" || texto == "EN PROCESO"
}

private fun obtenerIconoCategoria(categoria: String): ImageVector {
    return when (categoria.lowercase()) {
        "plomería", "plomeria" -> Icons.Outlined.WaterDrop
        "electricidad" -> Icons.Outlined.ElectricBolt
        "cerrajería", "cerrajeria" -> Icons.Outlined.Build
        "pintura" -> Icons.Outlined.FormatPaint
        "limpieza" -> Icons.Outlined.CleaningServices
        else -> Icons.Outlined.Handyman
    }
}

private fun calcularTiempoTranscurrido(fecha: Date?): String {
    if (fecha == null) return "Creada recientemente"
    val diff = System.currentTimeMillis() - fecha.time
    if (diff <= 0) return "Creada hace un momento"

    val minutos = TimeUnit.MILLISECONDS.toMinutes(diff)
    val horas = TimeUnit.MILLISECONDS.toHours(diff)
    val dias = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutos < 1 -> "Creada hace un momento"
        minutos < 60 -> "Creada hace $minutos min"
        horas < 24 -> "Creada hace $horas ${if (horas == 1L) "hora" else "horas"}"
        else -> "Creada hace $dias ${if (dias == 1L) "día" else "días"}"
    }
}