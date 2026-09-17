package com.servixyabogota.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    onNavigateToUsuarios: () -> Unit = {}
) {
    val context = LocalContext.current
    val solicitudes by viewModel.solicitudes.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    var prestadorAProcesar by remember { mutableStateOf<SolicitudPrestador?>(null) }
    var mostrarBottomSheetRechazo by remember { mutableStateOf(false) }
    var tabSeleccionado by remember { mutableStateOf(0) }

    // Filtrar para mostrar ÚNICAMENTE los pendientes en el Inicio
    val solicitudesPendientes = remember(solicitudes) {
        solicitudes.filter { it.estadoVerificacion == "PENDIENTE_VERIFICACION" }
    }

    val pendientesCount = solicitudesPendientes.size

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Panel Administrador",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // Badge "X Pendientes"
                        Surface(
                            color = Color(0xFFFF9800),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$pendientesCount Pendientes",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Botones de Actualizar y Cerrar Sesión
                    Row {
                        IconButton(onClick = { viewModel.cargarSolicitudes() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color(0xFF1976D2))
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = Color(0xFFD32F2F))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SOLICITUDES DE VERIFICACIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.height(65.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationBarItem(
                        selected = tabSeleccionado == 0,
                        onClick = { tabSeleccionado = 0 },
                        icon = { Icon(Icons.Default.GridView, contentDescription = "Inicio") },
                        label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2),
                            selectedTextColor = Color(0xFF1976D2),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFFE3F2FD)
                        )
                    )
                    NavigationBarItem(
                        selected = tabSeleccionado == 1,
                        onClick = { onNavigateToUsuarios() },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Usuarios") },
                        label = { Text("Usuarios", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2),
                            selectedTextColor = Color(0xFF1976D2),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFFE3F2FD)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F5F7))
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF1976D2),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (solicitudesPendientes.isEmpty()) {
                Text(
                    text = "No hay solicitudes pendientes",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(solicitudesPendientes, key = { it.id }) { prestador ->
                        TarjetaPrestadorMockup(
                            prestador = prestador,
                            onAbrirDocumento = { url ->
                                if (url.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Documento no disponible", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAprobar = {
                                viewModel.aprobarPrestador(
                                    uid = prestador.id,
                                    onSuccess = { Toast.makeText(context, "Usuario Aprobado", Toast.LENGTH_SHORT).show() },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                )
                            },
                            onRechazar = {
                                prestadorAProcesar = prestador
                                mostrarBottomSheetRechazo = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal de Rechazo
    if (mostrarBottomSheetRechazo && prestadorAProcesar != null) {
        ModalBottomSheetRechazo(
            prestador = prestadorAProcesar!!,
            onDismiss = { mostrarBottomSheetRechazo = false },
            onConfirmar = { motivo, justificacion, documentosARechazar ->
                viewModel.rechazarPrestador(
                    uid = prestadorAProcesar!!.id,
                    motivo = motivo,
                    justificacion = justificacion,
                    documentosRechazados = documentosARechazar,
                    onSuccess = {
                        mostrarBottomSheetRechazo = false
                        Toast.makeText(context, "Verificación rechazada", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
            }
        )
    }
} // <-- AQUÍ SE CERRÓ LA FUNCIÓN PRINCIPAL

@Composable
private fun TarjetaPrestadorMockup(
    prestador: SolicitudPrestador,
    onAbrirDocumento: (String) -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    val fechaFormateada = remember(prestador.fechaEnvioDocumentos) {
        if (prestador.fechaEnvioDocumentos > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))
            sdf.format(Date(prestador.fechaEnvioDocumentos))
        } else {
            "Fecha no disp."
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prestador.fotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = prestador.fotoUrl,
                        contentDescription = "Foto Perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onAbrirDocumento(prestador.fotoUrl) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1976D2))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = prestador.nombreCompleto.ifBlank { "Sin Nombre" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827)
                        )

                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = prestador.profesion,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Solicitud recibida: $fechaFormateada",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "C.C.: ${prestador.cedula}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                    Text(text = "Correo: ${prestador.email}", fontSize = 12.sp, color = Color.Gray)
                    if (prestador.telefono.isNotBlank()) {
                        Text(text = "Teléfono: ${prestador.telefono}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Documentos Adjuntos", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF111827))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CajaDocumento(
                    titulo = "Cedula Frente",
                    url = prestador.documentos["cedulaFrente"] ?: "",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirDocumento
                )
                CajaDocumento(
                    titulo = "Cedula Reverso",
                    url = prestador.documentos["cedulaAtras"] ?: "",
                    modifier = Modifier.weight(1f),
                    onClick = onAbrirDocumento
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CajaDocumento(
                titulo = "Antecedentes",
                url = prestador.documentos["antecedentes"] ?: "",
                modifier = Modifier.fillMaxWidth(),
                onClick = onAbrirDocumento
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRechazar,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onAprobar,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aprobar Usuario",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CajaDocumento(
    titulo: String,
    url: String,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    val disponible = url.isNotBlank()
    Column(
        modifier = modifier.clickable(enabled = disponible) { onClick(url) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (disponible) Color(0xFFEEEEEE) else Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = if (disponible) Color(0xFF616161) else Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = titulo,
            fontSize = 11.sp,
            color = if (disponible) Color.Gray else Color.LightGray,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalBottomSheetRechazo(
    prestador: SolicitudPrestador,
    onDismiss: () -> Unit,
    onConfirmar: (motivo: String, justificacion: String, documentosARechazar: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val motivos = listOf(
        "Documento ilegible / borroso",
        "Documento vencido o no válido",
        "Foto de perfil no cumple los criterios",
        "Inconsistencia en los antecedentes"
    )

    var motivoSeleccionado by remember { mutableStateOf(motivos[0]) }
    var justificacion by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    var rechazarFoto by remember { mutableStateOf(false) }
    var rechazarCedulaFrente by remember { mutableStateOf(false) }
    var rechazarCedulaAtras by remember { mutableStateOf(false) }
    var rechazarAntecedentes by remember { mutableStateOf(false) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rechazar Verificación", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Cancel, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFFF8F9FA),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (prestador.fotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = prestador.fotoUrl,
                            contentDescription = "Foto Perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prestador.nombreCompleto, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("C.C. ${prestador.cedula}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Documentos a solicitar nuevamente", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD32F2F))
            Spacer(modifier = Modifier.height(4.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rechazarFoto, onCheckedChange = { rechazarFoto = it })
                    Text("Foto de Perfil (Rostro)", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rechazarCedulaFrente, onCheckedChange = { rechazarCedulaFrente = it })
                    Text("Cédula (Frente)", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rechazarCedulaAtras, onCheckedChange = { rechazarCedulaAtras = it })
                    Text("Cédula (Anverso / Reverso)", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rechazarAntecedentes, onCheckedChange = { rechazarAntecedentes = it })
                    Text("Antecedentes Judiciales", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Motivo principal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = motivoSeleccionado,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1976D2),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    motivos.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item, fontSize = 13.sp) },
                            onClick = {
                                motivoSeleccionado = item
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Justificación detallada", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = justificacion,
                onValueChange = { justificacion = it },
                placeholder = { Text("Escribe la justificación detallada...", fontSize = 12.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Cancelar", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val seleccionados = mutableListOf<String>()
                        if (rechazarFoto) seleccionados.add("fotoPerfil")
                        if (rechazarCedulaFrente) seleccionados.add("cedulaFrente")
                        if (rechazarCedulaAtras) seleccionados.add("cedulaAtras")
                        if (rechazarAntecedentes) seleccionados.add("antecedentes")

                        if (seleccionados.isEmpty()) {
                            Toast.makeText(context, "Selecciona al menos un documento a corregir", Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirmar(motivoSeleccionado, justificacion, seleccionados)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.weight(1.2f).height(46.dp)
                ) {
                    Text("Confirmar Rechazo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}