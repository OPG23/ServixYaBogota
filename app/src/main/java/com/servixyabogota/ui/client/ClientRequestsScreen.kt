package com.servixyabogota.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

// ==========================================
// MODELO DE DATOS Y ENUMS
// ==========================================

enum class EstadoSolicitud {
    ABIERTA,
    EN_PROCESO,
    COMPLETADA
}

data class SolicitudClienteModel(
    val id: String,
    val titulo: String,
    val categoria: String,
    val direccion: String,
    val tiempoCreada: String,
    val estado: EstadoSolicitud,
    val iconoCategoria: ImageVector
)

// ==========================================
// SCREEN PRINCIPAL: MIS SOLICITUDES
// ==========================================

@Composable
fun ClientRequestsScreen(
    onNuevaSolicitudClick: () -> Unit = {},
    onEditarClick: (SolicitudClienteModel) -> Unit = {},
    onCancelarClick: (SolicitudClienteModel) -> Unit = {},
    onVerChatClick: (SolicitudClienteModel) -> Unit = {},
    onNavigateTab: (String) -> Unit = {}
) {
    var filtroSeleccionado by remember { mutableStateOf("Abiertas") }

    val listaSolicitudes = remember {
        listOf(
            SolicitudClienteModel(
                id = "1",
                titulo = "Fuga en lavamanos principal",
                categoria = "Plomería",
                direccion = "Calle 127 #15-45",
                tiempoCreada = "Creada hace 2 horas",
                estado = EstadoSolicitud.ABIERTA,
                iconoCategoria = Icons.Outlined.WaterDrop
            ),
            SolicitudClienteModel(
                id = "2",
                titulo = "Cambio de tomacorrientes",
                categoria = "Electricidad",
                direccion = "Carrera 9 #72-10",
                tiempoCreada = "Creada hace 5 horas",
                estado = EstadoSolicitud.ABIERTA,
                iconoCategoria = Icons.Outlined.ElectricBolt
            ),
            SolicitudClienteModel(
                id = "3",
                titulo = "Cambio de cerradura puerta principal",
                categoria = "Cerrajería",
                direccion = "Av. Boyacá #64-20",
                tiempoCreada = "Creada hace 1 día",
                estado = EstadoSolicitud.EN_PROCESO,
                iconoCategoria = Icons.Outlined.Build
            )
        )
    }

    // Filtrado según el tab seleccionado
    val solicitudesFiltradas = remember(filtroSeleccionado, listaSolicitudes) {
        when (filtroSeleccionado) {
            "Abiertas" -> listaSolicitudes.filter { it.estado == EstadoSolicitud.ABIERTA }
            "En Proceso" -> listaSolicitudes.filter { it.estado == EstadoSolicitud.EN_PROCESO }
            else -> listaSolicitudes.filter { it.estado == EstadoSolicitud.COMPLETADA }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ENCABEZADO (Título + Botón Nueva Solicitud)
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

            // 2. TABS DE FILTRO (Abiertas, En Proceso, Historial)
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
                            "Abiertas" to "Abiertas (2)",
                            "En Proceso" to "En Proceso (1)",
                            "Historial" to "Historial"
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

            // 3. TARJETAS DE SOLICITUDES
            items(solicitudesFiltradas, key = { it.id }) { solicitud ->
                SolicitudCardItem(
                    solicitud = solicitud,
                    onEditarClick = { onEditarClick(solicitud) },
                    onCancelarClick = { onCancelarClick(solicitud) },
                    onVerChatClick = { onVerChatClick(solicitud) }
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
    solicitud: SolicitudClienteModel,
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
            // Fila superior: Badge Categoría + Badge Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge Categoría (con icono)
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
                            imageVector = solicitud.iconoCategoria,
                            contentDescription = null,
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = solicitud.categoria,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                // Badge Estado (ABIERTA / EN PROCESO)
                val (estadoBg, estadoTextColor, estadoTexto) = when (solicitud.estado) {
                    EstadoSolicitud.ABIERTA -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "ABIERTA")
                    EstadoSolicitud.EN_PROCESO -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "EN PROCESO")
                    EstadoSolicitud.COMPLETADA -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "COMPLETADA")
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

            // Título de la Solicitud
            Text(
                text = solicitud.titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Ubicación
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
                    text = solicitud.direccion,
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            }

            // Tiempo transcurrido
            Text(
                text = solicitud.tiempoCreada,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )

            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Botones de Acción dinámicos según el estado
            if (solicitud.estado == EstadoSolicitud.ABIERTA) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Editar
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

                    // Botón Cancelar
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
            } else if (solicitud.estado == EstadoSolicitud.EN_PROCESO) {
                // Botón Ancho Ver Chat / Prestador
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
