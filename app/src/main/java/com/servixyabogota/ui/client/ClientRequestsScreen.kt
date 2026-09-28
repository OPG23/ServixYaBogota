package com.servixyabogota.ui.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.servixyabogota.data.model.Solicitud
import java.util.Date
import java.util.concurrent.TimeUnit

// ==========================================
// SCREEN PRINCIPAL: MIS SOLICITUDES / PROPUESTAS
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
    val context = LocalContext.current

    var filtroEstadoSeleccionado by remember { mutableStateOf("Abiertas") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }

    var solicitudAEditar by remember { mutableStateOf<Solicitud?>(null) }
    var solicitudACancelar by remember { mutableStateOf<Solicitud?>(null) }

    // SI SE HACE CLIC EN EDITAR, MUESTRA CREATEREQUESTSCREEN
    val solicitudParaEditar = solicitudAEditar
    if (solicitudParaEditar != null) {
        CreateRequestScreen(
            solicitudToEdit = solicitudParaEditar,
            onBack = { solicitudAEditar = null },
            onGuardarEdicion = { id, cat, det, loc, dir, urg, archivos ->
                viewModel.actualizarSolicitud(
                    solicitudId = id,
                    categoria = cat,
                    detalle = det,
                    urgencia = urg,
                    direccion = dir,
                    localidad = loc,
                    archivos = archivos,
                    context = context,
                    onSuccess = { solicitudAEditar = null },
                    onError = { /* Manejar error si ocurre */ }
                )
            }
        )
        return
    }

    // 1. Obtener UID del usuario autenticado actual
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // 2. Obtener la lista original desde el ViewModel
    val listaSolicitudesRaw by remember(currentUserId) {
        viewModel.getMisSolicitudes(currentUserId)
    }.collectAsState(initial = emptyList())

    // 3. Filtrar duplicados por ID
    val listaSolicitudes = remember(listaSolicitudesRaw) {
        listaSolicitudesRaw.distinctBy { it.id }
    }

    // 4. Clasificación de listas según el estado
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

    // 5. Filtrado combinado por Estado + Categoría
    val solicitudesFiltradas = remember(filtroEstadoSeleccionado, categoriaSeleccionada, listaSolicitudes) {
        val porEstado = when (filtroEstadoSeleccionado) {
            "Abiertas" -> abiertas
            "En Proceso" -> enProceso
            else -> historial
        }

        if (categoriaSeleccionada == "Todas") {
            porEstado
        } else {
            porEstado.filter { solicitud ->
                solicitud.categoria.contains(categoriaSeleccionada, ignoreCase = true) ||
                        categoriaSeleccionada.contains(solicitud.categoria, ignoreCase = true)
            }
        }
    }

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
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ENCABEZADO
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
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

                // FILTRO DE CATEGORÍAS (Reutilizado de ClientDirectChatsScreen.kt)
                item {
                    CategoryFilterChips(
                        selectedCategory = categoriaSeleccionada,
                        onCategorySelected = { categoriaSeleccionada = it }
                    )
                }

                // TABS DE ESTADO (Abiertas, En Proceso, Historial)
                item {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
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
                                val isSelected = filtroEstadoSeleccionado == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                                        .clickable { filtroEstadoSeleccionado = key }
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

                // MENSAJE DE ESTADO VACÍO
                if (solicitudesFiltradas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (categoriaSeleccionada == "Todas") {
                                    "No tienes solicitudes en esta categoría de estado"
                                } else {
                                    "No hay solicitudes de '$categoriaSeleccionada' en este filtro"
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // TARJETAS DE SOLICITUDES
                    items(solicitudesFiltradas.size, key = { solicitudesFiltradas[it].id }) { index ->
                        val solicitud = solicitudesFiltradas[index]
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            SolicitudCardItem(
                                solicitud = solicitud,
                                onEditarClick = { solicitudAEditar = solicitud },
                                onCancelarClick = { solicitudACancelar = solicitud },
                                onVerChatClick = { onVerChatClick(solicitud) }
                            )
                        }
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
    val esProceso = esEstadoEnProceso(solicitud.estado)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
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
                    esProceso -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "EN PROCESO")
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

            // Indicador de mensajes no leídos cuando la solicitud está EN_PROCESO
            if (esProceso) {
                val tieneMensajesNuevos = solicitud.noLeidosCliente > 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (tieneMensajesNuevos) Color(0xFF16A34A) else Color(0xFF94A3B8),
                                CircleShape
                            )
                    )
                    Text(
                        text = if (tieneMensajesNuevos) {
                            if (solicitud.noLeidosCliente == 1) "1 mensaje nuevo" else "${solicitud.noLeidosCliente} mensajes nuevos"
                        } else {
                            "Sin mensajes nuevos"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tieneMensajesNuevos) Color(0xFF16A34A) else Color(0xFF64748B)
                    )
                }
            }

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
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
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
            } else if (esProceso) {
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
                        text = "Abrir Chat de Servicio",
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
        "limpieza", "aseo y limpieza" -> Icons.Outlined.CleaningServices
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