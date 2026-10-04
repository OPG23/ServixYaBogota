package com.servixyabogota.ui.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ==========================================
// MODELOS DE DATOS Y ESTADOS
// ==========================================

enum class RolUsuario(val label: String) {
    TODOS("Todos"),
    CLIENTE("Cliente"),
    PRESTADOR("Prestador")
}

enum class EstadoVerificacion(val label: String, val badgeColor: Color, val textColor: Color) {
    TODOS("Todos", Color.Transparent, Color.Unspecified),
    VERIFICADO("VERIFICADO", Color(0xFFDCFCE7), Color(0xFF15803D)),
    NO_VERIFICADO("NO VERIFICADO", Color(0xFFF3F4F6), Color(0xFF4B5563)),
    DESHABILITADO("DESHABILITADO", Color(0xFFFEE2E2), Color(0xFFB91C1C)),
    PENDIENTE("PENDIENTE", Color(0xFFFEF3C7), Color(0xFFB45309))
}

data class UsuarioAdmin(
    val id: String,
    val nombre: String,
    val correo: String,
    val cedula: String,
    val fotoUrl: String?,
    val rol: RolUsuario,
    val estado: EstadoVerificacion,
    val calificacion: Double?, // Permite ser nulo si no tiene calificaciones
    val fechaRegistroMs: Long = 0L
)

// ==========================================
// CONTENEDOR PRINCIPAL (NAVEGACIÓN INTERNA)
// ==========================================

@Composable
fun AdminUsersContainer(
    viewModel: AdminViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    // Estado para controlar qué usuario se seleccionó para ver su detalle
    var usuarioSeleccionado by remember { mutableStateOf<UsuarioAdmin?>(null) }

    if (usuarioSeleccionado != null) {
        // 1. PANTALLA DE DETALLE DEL USUARIO
        UserDetailScreen(
            usuario = usuarioSeleccionado!!,
            onBackClick = {
                usuarioSeleccionado = null // Volver a la lista
            },
            onToggleDeshabilitar = { deshabilitar ->
                val nuevoEstado = if (deshabilitar) {
                    EstadoVerificacion.DESHABILITADO
                } else {
                    EstadoVerificacion.VERIFICADO
                }

                // Actualizar en Firebase/BD
                viewModel.cambiarEstadoUsuario(usuarioSeleccionado!!.id, nuevoEstado)

                // Actualizar estado local para reflejar el cambio inmediato en pantalla
                usuarioSeleccionado = usuarioSeleccionado!!.copy(estado = nuevoEstado)
            },
            onEliminarPermanente = {
                viewModel.eliminarUsuario(usuarioSeleccionado!!.id)
                usuarioSeleccionado = null // Volver a la lista tras eliminar
            }
        )
    } else {
        // 2. PANTALLA DE LISTA DE USUARIOS
        AdminUsersScreen(
            viewModel = viewModel,
            onNavigateToHome = onNavigateToHome,
            onVerDetalleUsuario = { usuario ->
                usuarioSeleccionado = usuario // Abrir detalle
            }
        )
    }
}

// ==========================================
// PANTALLA DE LISTA DE USUARIOS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {},
    onVerDetalleUsuario: (UsuarioAdmin) -> Unit = {}
) {
    // Cargar datos al entrar a la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarUsuarios()
    }

    val listaUsuariosReal by viewModel.usuarios.observeAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)

    // Estados de Filtros
    var searchQuery by remember { mutableStateOf("") }
    var tabSeleccionado by remember { mutableStateOf(RolUsuario.TODOS) }
    var filtrosAvanzadosExpandidos by remember { mutableStateOf(false) }

    var rolFiltro by remember { mutableStateOf(RolUsuario.TODOS) }
    var estadoVerificacionFiltro by remember { mutableStateOf(EstadoVerificacion.TODOS) }
    var rangoCalificacion by remember { mutableStateOf(1.0f..5.0f) }
    var fechaDesde by remember { mutableStateOf("") }
    var fechaHasta by remember { mutableStateOf("") }

    // Conteo para las pestañas superiores
    val totalTodos = listaUsuariosReal.size
    val totalClientes = listaUsuariosReal.count { it.rol == RolUsuario.CLIENTE }
    val totalPrestadores = listaUsuariosReal.count { it.rol == RolUsuario.PRESTADOR }

    val simpleDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Filtrado en tiempo real sobre los datos de Firestore
    val usuariosFiltrados = listaUsuariosReal.filter { user ->
        val coincideBusqueda = user.nombre.contains(searchQuery, ignoreCase = true) ||
                user.correo.contains(searchQuery, ignoreCase = true) ||
                user.cedula.contains(searchQuery)

        val coincideTab = when (tabSeleccionado) {
            RolUsuario.TODOS -> true
            RolUsuario.CLIENTE -> user.rol == RolUsuario.CLIENTE
            RolUsuario.PRESTADOR -> user.rol == RolUsuario.PRESTADOR
        }

        val coincideRolAvanzado = if (rolFiltro == RolUsuario.TODOS) true else user.rol == rolFiltro

        val coincideEstado = if (estadoVerificacionFiltro == EstadoVerificacion.TODOS) true else user.estado == estadoVerificacionFiltro

        val coincideCalificacion = user.calificacion == null ||
                (user.calificacion >= rangoCalificacion.start && user.calificacion <= rangoCalificacion.endInclusive)

        val coincideFecha = try {
            var valid = true
            if (fechaDesde.isNotBlank() && user.fechaRegistroMs > 0) {
                val desdeTime = simpleDateFormat.parse(fechaDesde)?.time ?: 0L
                if (user.fechaRegistroMs < desdeTime) valid = false
            }
            if (fechaHasta.isNotBlank() && user.fechaRegistroMs > 0) {
                val hastaTime = (simpleDateFormat.parse(fechaHasta)?.time ?: Long.MAX_VALUE) + 86400000L
                if (user.fechaRegistroMs > hastaTime) valid = false
            }
            valid
        } catch (e: Exception) {
            true
        }

        coincideBusqueda && coincideTab && coincideRolAvanzado && coincideEstado && coincideCalificacion && coincideFecha
    }

    Scaffold(
        bottomBar = {
            AdminBottomNavigation(
                selectedTab = "Usuarios",
                onInicioClick = onNavigateToHome,
                onUsuariosClick = {}
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Título Principal
            Text(
                text = "Gestión de Cuentas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )

            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre, correo o cédula...", color = Color(0xFF9CA3AF), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Chips Superiores (Todos, Clientes, Prestadores)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabChip(
                    text = "Todos ($totalTodos)",
                    isSelected = tabSeleccionado == RolUsuario.TODOS,
                    onClick = { tabSeleccionado = RolUsuario.TODOS }
                )
                FilterTabChip(
                    text = "Clientes ($totalClientes)",
                    isSelected = tabSeleccionado == RolUsuario.CLIENTE,
                    onClick = { tabSeleccionado = RolUsuario.CLIENTE }
                )
                FilterTabChip(
                    text = "Prestadores ($totalPrestadores)",
                    isSelected = tabSeleccionado == RolUsuario.PRESTADOR,
                    onClick = { tabSeleccionado = RolUsuario.PRESTADOR }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && listaUsuariosReal.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1976D2))
                }
            } else {
                // Lista con Filtros y Usuarios Reales
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Card de Filtros Avanzados
                    item {
                        FiltrosAvanzadosCard(
                            isExpanded = filtrosAvanzadosExpandidos,
                            onToggleExpand = { filtrosAvanzadosExpandidos = !filtrosAvanzadosExpandidos },
                            rolSeleccionado = rolFiltro,
                            onRolSelected = { rolFiltro = it },
                            estadoSeleccionado = estadoVerificacionFiltro,
                            onEstadoSelected = { estadoVerificacionFiltro = it },
                            rangoCalificacion = rangoCalificacion,
                            onRangoCalificacionChanged = { rangoCalificacion = it },
                            fechaDesde = fechaDesde,
                            onFechaDesdeChanged = { fechaDesde = it },
                            fechaHasta = fechaHasta,
                            onFechaHastaChanged = { fechaHasta = it }
                        )
                    }

                    if (usuariosFiltrados.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No se encontraron usuarios", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }

                    // Elementos reales de la lista
                    items(usuariosFiltrados, key = { it.id }) { usuario ->
                        UsuarioItemCard(
                            usuario = usuario,
                            onClick = { onVerDetalleUsuario(usuario) },
                            onToggleEstado = {
                                val nuevoEstado = if (usuario.estado == EstadoVerificacion.DESHABILITADO) {
                                    EstadoVerificacion.VERIFICADO
                                } else {
                                    EstadoVerificacion.DESHABILITADO
                                }
                                viewModel.cambiarEstadoUsuario(usuario.id, nuevoEstado)
                            },
                            onEliminar = {
                                viewModel.eliminarUsuario(usuario.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENTES AUXILIARES
// ==========================================

@Composable
private fun FilterTabChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF1976D2) else Color.White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF374151)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosAvanzadosCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    rolSeleccionado: RolUsuario,
    onRolSelected: (RolUsuario) -> Unit,
    estadoSeleccionado: EstadoVerificacion,
    onEstadoSelected: (EstadoVerificacion) -> Unit,
    rangoCalificacion: ClosedFloatingPointRange<Float>,
    onRangoCalificacionChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    fechaDesde: String,
    onFechaDesdeChanged: (String) -> Unit,
    fechaHasta: String,
    onFechaHastaChanged: (String) -> Unit
) {
    var expandedEstadoDropdown by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filtros Avanzados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF111827)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF6B7280)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. FILTRO DE ROL
                Text(
                    text = "ROL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RolUsuario.values().forEach { rol ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onRolSelected(rol) }
                        ) {
                            RadioButton(
                                selected = rolSeleccionado == rol,
                                onClick = { onRolSelected(rol) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1976D2))
                            )
                            Text(
                                text = rol.label,
                                fontSize = 13.sp,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. FILTRO DE ESTADO
                Text(
                    text = "ESTADO DE VERIFICACIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedEstadoDropdown,
                    onExpandedChange = { expandedEstadoDropdown = !expandedEstadoDropdown }
                ) {
                    OutlinedTextField(
                        value = estadoSeleccionado.label.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEstadoDropdown) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE5E5E5),
                            focusedBorderColor = Color(0xFF1976D2)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedEstadoDropdown,
                        onDismissRequest = { expandedEstadoDropdown = false }
                    ) {
                        EstadoVerificacion.values().forEach { estado ->
                            DropdownMenuItem(
                                text = { Text(estado.label) },
                                onClick = {
                                    onEstadoSelected(estado)
                                    expandedEstadoDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. RANGOS DE CALIFICACIÓN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CALIFICACIÓN PROMEDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "%.1f - %.1f".format(rangoCalificacion.start, rangoCalificacion.endInclusive),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }

                RangeSlider(
                    value = rangoCalificacion,
                    onValueChange = onRangoCalificacionChanged,
                    valueRange = 1.0f..5.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF1976D2),
                        activeTrackColor = Color(0xFF1976D2),
                        inactiveTrackColor = Color(0xFFE5E7EB)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. FECHA DE REGISTRO
                Text(
                    text = "FECHA DE REGISTRO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FechaSelectorField(
                        label = "Desde:",
                        valor = fechaDesde,
                        placeholder = "DD/MM/AAAA",
                        modifier = Modifier.weight(1f),
                        onDateSelected = onFechaDesdeChanged
                    )

                    FechaSelectorField(
                        label = "Hasta:",
                        valor = fechaHasta,
                        placeholder = "DD/MM/AAAA",
                        modifier = Modifier.weight(1f),
                        onDateSelected = onFechaHastaChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun FechaSelectorField(
    label: String,
    valor: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val fechaFormateada = "%02d/%02d/%d".format(dayOfMonth, month + 1, year)
            onDateSelected(fechaFormateada)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = valor,
        onValueChange = {},
        readOnly = true,
        placeholder = {
            Text(
                text = "$label $placeholder",
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF)
            )
        },
        trailingIcon = {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { datePickerDialog.show() },
                tint = Color(0xFF9CA3AF)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE5E7EB),
            focusedBorderColor = Color(0xFF1976D2)
        ),
        modifier = modifier.clickable { datePickerDialog.show() }
    )
}

@Composable
private fun UsuarioItemCard(
    usuario: UsuarioAdmin,
    onClick: () -> Unit,
    onToggleEstado: () -> Unit,
    onEliminar: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = if (!usuario.fotoUrl.isNullOrEmpty()) usuario.fotoUrl else "https://via.placeholder.com/150",
                contentDescription = usuario.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (rolBg, rolColor) = if (usuario.rol == RolUsuario.CLIENTE) {
                        Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
                    } else {
                        Color(0xFFF3E8FF) to Color(0xFF6B21A8)
                    }

                    // Rol (CLIENTE / PRESTADOR)
                    Surface(
                        color = rolBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = usuario.rol.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = rolColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Estado de Verificación
                    Surface(
                        color = usuario.estado.badgeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = usuario.estado.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = usuario.estado.textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Calificación (-.- si no tiene)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = usuario.calificacion?.let { "%.1f".format(it) } ?: "-.-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color(0xFF6B7280)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver Detalle") },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (usuario.estado == EstadoVerificacion.DESHABILITADO) "Habilitar Cuenta" else "Deshabilitar Cuenta"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (usuario.estado == EstadoVerificacion.DESHABILITADO) Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleEstado()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar Usuario", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        onClick = {
                            menuExpanded = false
                            onEliminar()
                        }
                    )
                }
            }
        }
    }
}