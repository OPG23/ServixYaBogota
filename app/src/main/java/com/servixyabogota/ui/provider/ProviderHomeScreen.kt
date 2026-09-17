package com.servixyabogota.ui.provider

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servixyabogota.ui.client.ProviderSecuritySettingsScreen

data class Oportunidad(
    val id: String,
    val categoria: String,
    val emojiCategoria: String,
    val titulo: String,
    val ubicacion: String,
    val distancia: String,
    val descripcion: String,
    val tiempoHace: String,
    val esUrgente: Boolean = false
)

@Composable
fun ProviderHomeScreen(
    viewModel: ProviderViewModel,
    onIrACorregirDocumentos: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.observeAsState(EstadoProveedorUiState())
    var mostrarModalRechazo by remember(uiState.estadoVerificacion) {
        mutableStateOf(uiState.estadoVerificacion == "RECHAZADO")
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var subPantallaPerfil by remember { mutableStateOf("PERFIL") } // "PERFIL", "PORTAFOLIO", "SEGURIDAD", "RESENAS"

    // Modal de Rechazo
    if (uiState.estadoVerificacion == "RECHAZADO" && mostrarModalRechazo) {
        AlertDialog(
            onDismissRequest = { },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Observaciones en tu Verificación",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFD32F2F)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Hola, ${uiState.nombreCompleto}. Se requieren correcciones:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Motivo: ${uiState.motivoRechazo}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                            if (uiState.justificacionRechazo.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.justificacionRechazo,
                                    fontSize = 12.sp,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarModalRechazo = false
                        onIrACorregirDocumentos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Corregir Documentos", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalRechazo = false }) {
                    Text("Cerrar", color = Color.Gray)
                }
            }
        )
    }

    // PRESTADOR APROBADO: Navegación Principal
    if (uiState.estadoVerificacion == "APROBADO") {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.BusinessCenter, contentDescription = "Trabajos") },
                        label = { Text("Trabajos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF8F00),
                            selectedTextColor = Color(0xFFFF8F00),
                            indicatorColor = Color(0xFFFFF3E0),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                        label = { Text("Historial", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF8F00),
                            selectedTextColor = Color(0xFFFF8F00),
                            indicatorColor = Color(0xFFFFF3E0),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Mi Perfil") },
                        label = { Text("Mi Perfil", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF8F00),
                            selectedTextColor = Color(0xFFFF8F00),
                            indicatorColor = Color(0xFFFFF3E0),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> OportunidadesScreen(categorias = uiState.categorias)
                    1 -> HistorialTrabajosScreen()
                    2 -> {
                        when (subPantallaPerfil) {
                            "PERFIL" -> {
                                PerfilProfesionalScreen(
                                    nombre = uiState.nombreCompleto,
                                    fotoUrl = uiState.fotoUrl,
                                    telefonoInicial = uiState.telefono,
                                    correoInicial = uiState.correo,
                                    esVerificado = true,
                                    onIrAZonaCobertura = { subPantallaPerfil = "PORTAFOLIO" },
                                    onIrASeguridad = { subPantallaPerfil = "SEGURIDAD" },
                                    onIrAResenas = { subPantallaPerfil = "RESENAS" },
                                    onGuardarCambios = { nuevoTelefono, nuevoCorreo ->
                                        viewModel.actualizarContactoPerfil(
                                            telefono = nuevoTelefono,
                                            correo = nuevoCorreo,
                                            onSuccess = {
                                                Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { error ->
                                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onLogout = onLogout // <-- Se pasa la función onLogout
                                )
                            }
                            "PORTAFOLIO" -> {
                                ZonaCoberturaPortafolioContainer(
                                    viewModel = viewModel,
                                    onVolver = { subPantallaPerfil = "PERFIL" }
                                )
                            }
                            "SEGURIDAD" -> {
                                ProviderSecuritySettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { subPantallaPerfil = "PERFIL" }
                                )
                            }
                            "RESENAS" -> {
                                ProviderReviewsScreen(
                                    onBack = { subPantallaPerfil = "PERFIL" }
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // PANTALLAS PARA ESTADOS PENDIENTE / NO ENVIADO / RECHAZADO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Hola, ${uiState.nombreCompleto.ifBlank { "Prestador" }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (uiState.estadoVerificacion) {
                    "NO_ENVIADO" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusCard(
                            icon = Icons.Default.CloudUpload,
                            iconColor = Color(0xFF1976D2),
                            bgColor = Color(0xFFE3F2FD),
                            title = "Documentación Pendiente",
                            subtitle = "Sube tus documentos (Cédula, Antecedentes) para activar tu cuenta.",
                            onClick = onIrACorregirDocumentos
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onIrACorregirDocumentos,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Cargar Documentos", fontWeight = FontWeight.Bold)
                        }
                    }

                    "PENDIENTE_VERIFICACION" -> StatusCard(
                        icon = Icons.Default.HourglassTop,
                        iconColor = Color(0xFFFF9800),
                        bgColor = Color(0xFFFFF3E0),
                        title = "Verificación en Proceso",
                        subtitle = "Tus documentos están en revisión. Te avisaremos pronto."
                    )

                    "RECHAZADO" -> StatusCard(
                        icon = Icons.Default.Warning,
                        iconColor = Color(0xFFD32F2F),
                        bgColor = Color(0xFFFFEBEE),
                        title = "Solicitud Rechazada",
                        subtitle = "Toca aquí para revisar las observaciones y volver a enviar.",
                        onClick = onIrACorregirDocumentos
                    )
                }
            }

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Cerrar Sesión", color = Color.Gray)
            }
        }
    }
}

// PANTALLA DE OPORTUNIDADES DE TRABAJO
@Composable
fun OportunidadesScreen(categorias: List<String>) {
    val context = LocalContext.current
    var filtroSeleccionado by remember { mutableStateOf("Todas") }

    val listaOportunidades = remember {
        listOf(
            Oportunidad(
                id = "1",
                categoria = "Plomería",
                emojiCategoria = "🪠",
                titulo = "Fuga de agua en lavamanos de baño principal",
                ubicacion = "Usaquén - Cll 127",
                distancia = "3.2 km de tu zona",
                descripcion = "Gotea constantemente la llave de paso de agua caliente. Requiere cambio urgente de empaque o manguera flexible antes de que dañe el mueble.",
                tiempoHace = "Hace 15 min",
                esUrgente = true
            ),
            Oportunidad(
                id = "2",
                categoria = "Gas",
                emojiCategoria = "🔥",
                titulo = "Mantenimiento preventivo e instalación de calentador de paso",
                ubicacion = "Suba - Colina Campestre",
                distancia = "5.7 km de tu zona",
                descripcion = "Se requiere técnico certificado para realizar revisión anual obligatoria e instalación de calentador a gas natural de 5.5 litros.",
                tiempoHace = "Hace 42 min",
                esUrgente = false
            ),
            Oportunidad(
                id = "3",
                categoria = "Plomería",
                emojiCategoria = "🪠",
                titulo = "Destape de tubería de lavaplatos obstruida",
                ubicacion = "Cedritos - Cl. 142",
                distancia = "1.8 km de tu zona",
                descripcion = "El agua no baja y se rebosa en la cocina. Intentamos con productos domésticos pero no funcionó. Se necesita sonda eléctrica.",
                tiempoHace = "Hace 1 hora",
                esUrgente = true
            ),
            Oportunidad(
                id = "4",
                categoria = "Gas",
                emojiCategoria = "🔥",
                titulo = "Adecuación de punto de gas para estufa de empotrar",
                ubicacion = "Chicó Reservado",
                distancia = "6.5 km de tu zona",
                descripcion = "Modificación de tubería de cobre para conectar nueva estufa con encendido electrónico. Trabajo certificado para copropiedad.",
                tiempoHace = "Hace 2 horas",
                esUrgente = false
            )
        )
    }

    val publicacionesFiltradas = remember(filtroSeleccionado) {
        when (filtroSeleccionado) {
            "En mi zona" -> listaOportunidades.filter { it.distancia.contains("1.") || it.distancia.contains("3.") }
            "Urgentes 🚨" -> listaOportunidades.filter { it.esUrgente }
            else -> listaOportunidades
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .statusBarsPadding()
    ) {
        // ENCABEZADO
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
                    text = "Oportunidades",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color(0xFF111827)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Disponible",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFFF8F9FA),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categorías: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = if (categorias.isNotEmpty()) categorias.joinToString(", ") else "Plomería, Gas natural",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8F00),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // FILTROS
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FilterChip(
                    text = "Todas (${listaOportunidades.size})",
                    isSelected = filtroSeleccionado == "Todas",
                    onClick = { filtroSeleccionado = "Todas" }
                )
            }
            item {
                FilterChip(
                    text = "En mi zona",
                    isSelected = filtroSeleccionado == "En mi zona",
                    onClick = { filtroSeleccionado = "En mi zona" }
                )
            }
            item {
                FilterChip(
                    text = "Urgentes 🚨",
                    isSelected = filtroSeleccionado == "Urgentes 🚨",
                    onClick = { filtroSeleccionado = "Urgentes 🚨" }
                )
            }
        }

        // LISTA DE OPORTUNIDADES
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(publicacionesFiltradas) { oportunidad ->
                OportunidadCard(
                    item = oportunidad,
                    onPostularme = {
                        Toast.makeText(context, "Postulación enviada para: ${oportunidad.titulo}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFFFF8F00) else Color.White,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) else null
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF4B5563),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun OportunidadCard(
    item: Oportunidad,
    onPostularme: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${item.emojiCategoria} ${item.categoria}",
                        color = Color(0xFFE65100),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (item.esUrgente) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Urgente 🚨",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.titulo,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.ubicacion} · A ${item.distancia}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.descripcion,
                fontSize = 13.sp,
                color = Color(0xFF4B5563),
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.tiempoHace,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )

                Button(
                    onClick = onPostularme,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Ver Detalle / Postularme",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun HistorialTrabajosScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No tienes trabajos completados aún", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    bgColor: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = iconColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}