package com.servixyabogota.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    usuario: UsuarioAdmin,
    telefono: String = "+57 310 456 7890", // O pásalo desde tu modelo/mapa
    serviciosTotales: Int = 24,
    reportesRecibidos: Int = 0,
    onBackClick: () -> Unit,
    onToggleDeshabilitar: (Boolean) -> Unit,
    onEliminarPermanente: () -> Unit
) {
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var estaDeshabilitado by remember {
        mutableStateOf(usuario.estado == EstadoVerificacion.DESHABILITADO)
    }

    // Formateo de fecha de registro
    val fechaFormateada = remember(usuario.fechaRegistroMs) {
        if (usuario.fechaRegistroMs > 0) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(usuario.fechaRegistroMs))
        } else {
            "10/08/2026"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle de Usuario",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF111827)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF111827)
                        )
                    }
                },
                actions = {
                    // Badge del Rol (Top Right)
                    val (rolBg, rolColor) = if (usuario.rol == RolUsuario.CLIENTE) {
                        Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
                    } else {
                        Color(0xFFF3E8FF) to Color(0xFF6B21A8)
                    }

                    Surface(
                        color = rolBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = usuario.rol.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = rolColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 1. TARJETA PRINCIPAL DEL PERFIL
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Foto
                    AsyncImage(
                        model = usuario.fotoUrl.takeIf { !it.isNullOrBlank() }
                            ?: "https://via.placeholder.com/150",
                        contentDescription = usuario.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nombre
                    Text(
                        text = usuario.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Badge Estado de Cuenta
                    val (estadoBg, estadoColor, estadoText) = if (estaDeshabilitado) {
                        Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Cuenta Suspendida")
                    } else {
                        Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Cuenta Activa")
                    }

                    Surface(
                        color = estadoBg,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = estadoText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = estadoColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = usuario.correo.ifBlank { "Sin correo registrado" },
                            fontSize = 14.sp,
                            color = Color(0xFF374151)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Teléfono
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = telefono,
                            fontSize = 14.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }

            // 2. MÉTRICAS DE DESEMPEÑO
            Text(
                text = "Métricas de Desempeño",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1F2937)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricaCard(
                        titulo = "Servicios Totales",
                        valor = serviciosTotales.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricaCard(
                        titulo = "Calificación",
                        valor = usuario.calificacion?.let { "%.1f".format(it) } ?: "-.-",
                        conEstrella = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricaCard(
                        titulo = "Fecha Registro",
                        valor = fechaFormateada,
                        modifier = Modifier.weight(1f)
                    )
                    MetricaCard(
                        titulo = "Reportes Recibidos",
                        valor = reportesRecibidos.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. CONTROL ADMINISTRATIVO
            Text(
                text = "Control Administrativo",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1F2937)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B)), // Borde ámbar/naranja
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Switch Deshabilitar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deshabilitar Usuario Temporalmente",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Suspende el acceso sin eliminar datos",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }

                        Switch(
                            checked = estaDeshabilitado,
                            onCheckedChange = { checked ->
                                estaDeshabilitado = checked
                                onToggleDeshabilitar(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Eliminar Permanentemente
                    OutlinedButton(
                        onClick = { mostrarDialogoEliminar = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eliminar Usuario Permanentemente",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Acción irreversible — requiere doble validación",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Diálogo de Confirmación para Eliminar
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = {
                Text(
                    text = "Confirmar eliminación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar permanentemente a ${usuario.nombre}? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoEliminar = false
                        onEliminarPermanente()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MetricaCard(
    titulo: String,
    valor: String,
    conEstrella: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conEstrella) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = valor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
        }
    }
}