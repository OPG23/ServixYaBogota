package com.servixyabogota.ui.client

import android.content.Context
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.servixyabogota.data.model.Solicitud

data class ItemMediaSolicitud(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val esVideo: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    solicitudToEdit: Solicitud? = null,
    onBack: () -> Unit = {},
    onPublicarSolicitud: (
        categoria: String,
        detalle: String,
        localidad: String,
        direccion: String,
        urgencia: String,
        archivos: List<Uri>
    ) -> Unit = { _, _, _, _, _, _ -> },
    onGuardarEdicion: (
        solicitudId: String,
        categoria: String,
        detalle: String,
        localidad: String,
        direccion: String,
        urgencia: String,
        archivos: List<Uri>
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val esEdicion = solicitudToEdit != null

    val categoriasDisponibles = listOf(
        "🪠 Plomería",
        "⚡ Electricidad",
        "🔑 Cerrajería",
        "🎨 Pintura",
        "🧹 Aseo y Limpieza",
        "🔌 Reparación de Electrodomésticos",
        "🪚 Carpintería"
    )

    val localidadesBogota = listOf(
        "Usaquén", "Chapinero", "Suba", "Teusaquillo", "Kennedy",
        "Engativá", "Fontibón", "Bosa", "Barrios Unidos", "Puente Aranda",
        "Los Mártires", "Santa Fe", "San Cristóbal", "Usme", "Tunjuelito",
        "Antonio Nariño", "Candelaria", "Rafael Uribe Uribe", "Ciudad Bolívar"
    )

    // Precargar datos si viene una solicitud a editar
    var categoriaSeleccionada by remember(solicitudToEdit) {
        mutableStateOf(
            categoriasDisponibles.find { it.contains(solicitudToEdit?.categoria ?: "", ignoreCase = true) }
                ?: solicitudToEdit?.categoria ?: ""
        )
    }
    var dropdownCategoriaExpanded by remember { mutableStateOf(false) }

    var localidadSeleccionada by remember(solicitudToEdit) {
        mutableStateOf(solicitudToEdit?.localidad ?: "")
    }
    var dropdownLocalidadExpanded by remember { mutableStateOf(false) }

    var detalleProblema by remember(solicitudToEdit) {
        mutableStateOf(solicitudToEdit?.detalleProblema ?: "")
    }
    var nivelUrgencia by remember(solicitudToEdit) {
        mutableStateOf(if (solicitudToEdit?.nivelUrgencia.isNullOrBlank()) "Media" else solicitudToEdit!!.nivelUrgencia)
    }
    var direccionBogota by remember(solicitudToEdit) {
        mutableStateOf(solicitudToEdit?.direccion ?: "")
    }

    // Inicializar lista de archivos remotos y locales
    var archivosAdjuntos by remember(solicitudToEdit) {
        val listaInicial: List<ItemMediaSolicitud> = solicitudToEdit?.archivosUrls?.map { url ->
            val uri = Uri.parse(url)
            ItemMediaSolicitud(
                uri = uri,
                esVideo = esVideoUri(context, uri)
            )
        } ?: emptyList()

        mutableStateOf<List<ItemMediaSolicitud>>(listaInicial)
    }
    var videoParaReproducir by remember { mutableStateOf<Uri?>(null) }

    val maxBytesPermitidos = 5 * 1024 * 1024L // 5 MB

    val launcherMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            if (archivosAdjuntos.size >= 3) {
                Toast.makeText(context, "Has alcanzado el límite máximo de 3 archivos", Toast.LENGTH_SHORT).show()
                return@let
            }

            val pesoArchivo = obtenerTamanoArchivoBytes(context, selectedUri)

            if (pesoArchivo > maxBytesPermitidos) {
                Toast.makeText(
                    context,
                    "El archivo supera el límite de 5 MB.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val esVideo = esVideoUri(context, selectedUri)
                archivosAdjuntos = archivosAdjuntos + ItemMediaSolicitud(
                    uri = selectedUri,
                    esVideo = esVideo
                )
            }
        }
    }

    videoParaReproducir?.let { uri ->
        VideoPlayerDialog(
            uri = uri,
            onDismiss = { videoParaReproducir = null }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (categoriaSeleccionada.isEmpty()) {
                                Toast.makeText(context, "Por favor selecciona una categoría", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (localidadSeleccionada.isEmpty()) {
                                Toast.makeText(context, "Por favor selecciona tu localidad", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (direccionBogota.trim().isEmpty()) {
                                Toast.makeText(context, "Por favor ingresa la dirección de atención", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Limpiar icono emoji para guardar solo el texto base en BD
                            val categoriaLimpia = categoriaSeleccionada.replace(Regex("^[^\b\\w]+"), "").trim()

                            if (esEdicion && solicitudToEdit != null) {
                                onGuardarEdicion(
                                    solicitudToEdit.id,
                                    categoriaLimpia,
                                    detalleProblema,
                                    localidadSeleccionada,
                                    direccionBogota,
                                    nivelUrgencia,
                                    archivosAdjuntos.map { it.uri }
                                )
                            } else {
                                onPublicarSolicitud(
                                    categoriaLimpia,
                                    detalleProblema,
                                    localidadSeleccionada,
                                    direccionBogota,
                                    nivelUrgencia,
                                    archivosAdjuntos.map { it.uri }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (esEdicion) "Guardar Cambios" else "Publicar Solicitud",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. ENCABEZADO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (esEdicion) "Editar Solicitud Técnica" else "Nueva Solicitud Técnica",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // 2. CATEGORÍA DE SERVICIO
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Categoría de Servicio",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownCategoriaExpanded,
                    onExpandedChange = { dropdownCategoriaExpanded = !dropdownCategoriaExpanded }
                ) {
                    OutlinedTextField(
                        value = if (categoriaSeleccionada.isEmpty()) "Selecciona Categoría Técnica" else categoriaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = if (categoriaSeleccionada.isEmpty()) Color(0xFF94A3B8) else Color(0xFF0F172A),
                            unfocusedTextColor = if (categoriaSeleccionada.isEmpty()) Color(0xFF94A3B8) else Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownCategoriaExpanded,
                        onDismissRequest = { dropdownCategoriaExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        categoriasDisponibles.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, fontSize = 14.sp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    categoriaSeleccionada = cat
                                    dropdownCategoriaExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. DETALLE DEL PROBLEMA
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Detalle del problema",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = detalleProblema,
                    onValueChange = { detalleProblema = it },
                    placeholder = {
                        Text(
                            text = "Describe la falla o servicio requerido...",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    },
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. FOTOS / VIDEOS (MÁX. 3)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "FOTOS / VIDEOS DEL PROBLEMA (OPCIONAL)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp
                )

                DashedUploadButton(
                    text = "+ Subir foto/video del problema (Máx. 3 y 5 MB c/u)",
                    onClick = {
                        if (archivosAdjuntos.size < 3) {
                            launcherMedia.launch("*/*")
                        } else {
                            Toast.makeText(context, "Máximo 3 elementos permitidos", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (index in 0..2) {
                        val item = archivosAdjuntos.getOrNull(index)
                        Box(modifier = Modifier.weight(1f)) {
                            MediaSlotItem(
                                item = item,
                                onClick = {
                                    if (item != null && item.esVideo) {
                                        videoParaReproducir = item.uri
                                    }
                                },
                                onDelete = {
                                    if (item != null) {
                                        archivosAdjuntos = archivosAdjuntos.filter { it.id != item.id }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 5. NIVEL DE URGENCIA
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nivel de Urgencia",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UrgenciaOptionButton(
                        text = "Baja",
                        isSelected = nivelUrgencia.contains("Baja", ignoreCase = true),
                        selectedBorderColor = Color(0xFF94A3B8),
                        selectedTextColor = Color(0xFF475569),
                        selectedBgColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { nivelUrgencia = "Baja" }
                    )

                    UrgenciaOptionButton(
                        text = "Media",
                        isSelected = nivelUrgencia.contains("Media", ignoreCase = true),
                        selectedBorderColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFD97706),
                        selectedBgColor = Color(0xFFFFFBEB),
                        modifier = Modifier.weight(1f),
                        onClick = { nivelUrgencia = "Media" }
                    )

                    UrgenciaOptionButton(
                        text = "Urgente 🚨",
                        isSelected = nivelUrgencia.contains("Urgente", ignoreCase = true),
                        selectedBorderColor = Color(0xFFEF4444),
                        selectedTextColor = Color(0xFFDC2626),
                        selectedBgColor = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1.1f),
                        onClick = { nivelUrgencia = "Urgente" }
                    )
                }
            }

            // 6. UBICACIÓN EN BOGOTÁ (LOCALIDAD + DIRECCIÓN)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ubicación en Bogotá",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                // Dropdown Localidad
                ExposedDropdownMenuBox(
                    expanded = dropdownLocalidadExpanded,
                    onExpandedChange = { dropdownLocalidadExpanded = !dropdownLocalidadExpanded }
                ) {
                    OutlinedTextField(
                        value = if (localidadSeleccionada.isEmpty()) "Selecciona tu Localidad en Bogotá" else localidadSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = if (localidadSeleccionada.isEmpty()) Color(0xFF94A3B8) else Color(0xFF0F172A),
                            unfocusedTextColor = if (localidadSeleccionada.isEmpty()) Color(0xFF94A3B8) else Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownLocalidadExpanded,
                        onDismissRequest = { dropdownLocalidadExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        localidadesBogota.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc, fontSize = 14.sp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    localidadSeleccionada = loc
                                    dropdownLocalidadExpanded = false
                                }
                            )
                        }
                    }
                }

                // Campo Dirección Exacta
                OutlinedTextField(
                    value = direccionBogota,
                    onValueChange = { direccionBogota = it },
                    placeholder = {
                        Text(
                            text = "Dirección exacta (ej. Calle 127 #15-45 Apt 302)",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MediaSlotItem(
    item: ItemMediaSolicitud?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (item != null) Color(0xFFE0E0E0) else Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (item != null) {
                Image(
                    painter = rememberAsyncImagePainter(item.uri),
                    contentDescription = if (item.esVideo) "Video cargado" else "Foto cargada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (item.esVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir Video",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .width(44.dp)
                        .height(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (item != null) (if (item.esVideo) "Video cargado" else "Foto cargada") else "Vacío",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DashedUploadButton(
    text: String,
    onClick: () -> Unit
) {
    val borderColor = Color(0xFF2563EB)
    val stroke = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFF6FF))
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = borderColor,
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = borderColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VideoPlayerDialog(
    uri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(uri)
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            start()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun UrgenciaOptionButton(
    text: String,
    isSelected: Boolean,
    selectedBorderColor: Color,
    selectedTextColor: Color,
    selectedBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectedBgColor else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) selectedBorderColor else Color(0xFFE2E8F0)
        ),
        modifier = modifier.height(44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedTextColor else Color(0xFF64748B)
            )
        }
    }
}

private fun obtenerTamanoArchivoBytes(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}

private fun esVideoUri(context: Context, uri: Uri?): Boolean {
    if (uri == null) return false
    val uriStr = uri.toString().lowercase()
    if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
        return uriStr.contains(".mp4") || uriStr.contains(".mkv") || uriStr.contains(".3gp") || uriStr.contains(".webm") || uriStr.contains("video")
    }
    val mimeType = context.contentResolver.getType(uri)
    return mimeType?.startsWith("video/") == true
}