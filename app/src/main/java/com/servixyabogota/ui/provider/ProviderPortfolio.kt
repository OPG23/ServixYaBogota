package com.servixyabogota.ui.provider

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState

data class ItemPortafolio(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val titulo: String = "",
    val esVideo: Boolean = false
)

@Composable
fun ZonaCoberturaPortafolioContainer(
    viewModel: ProviderViewModel,
    onVolver: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.observeAsState(EstadoProveedorUiState())

    val itemsPortafolioCargados = remember(uiState.portafolioUrls) {
        uiState.portafolioUrls.map { url ->
            val uri = Uri.parse(url)
            val esVideo = esVideoUri(context, uri)
            ItemPortafolio(
                uri = uri,
                titulo = if (esVideo) "Video de trabajo" else "Trabajo cargado",
                esVideo = esVideo
            )
        }
    }

    ZonaCoberturaPortafolioScreen(
        initialLocalidades = uiState.localidades,
        initialCategorias = uiState.categorias,
        initialDescripcion = uiState.descripcion,
        initialPortafolio = itemsPortafolioCargados,
        onVolver = onVolver,
        onGuardar = { localidades, categorias, portafolio, desc ->
            viewModel.guardarCoberturaYPortafolio(
                localidades = localidades,
                categorias = categorias,
                itemsPortafolio = portafolio,
                descripcion = desc,
                onSuccess = {
                    Toast.makeText(context, "Perfil profesional actualizado con éxito", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZonaCoberturaPortafolioScreen(
    initialLocalidades: List<String> = emptyList(),
    initialCategorias: List<String> = emptyList(),
    initialDescripcion: String = "",
    initialPortafolio: List<ItemPortafolio> = emptyList(),
    onVolver: () -> Unit = {},
    onGuardar: (
        localidades: List<String>,
        categorias: List<String>,
        portafolio: List<ItemPortafolio>,
        descripcion: String
    ) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current

    // Las 20 localidades oficiales de Bogotá
    val todasLasLocalidades = listOf(
        "Usaquén",
        "Chapinero",
        "Santa Fe",
        "San Cristóbal",
        "Usme",
        "Tunjuelito",
        "Bosa",
        "Kennedy",
        "Fontibón",
        "Engativá",
        "Suba",
        "Barrios Unidos",
        "Teusaquillo",
        "Los Mártires",
        "Antonio Nariño",
        "Puente Aranda",
        "La Candelaria",
        "Rafael Uribe Uribe",
        "Ciudad Bolívar",
        "Sumapaz"
    )

    val todasLasCategorias = listOf(
        "Plomería",
        "Electricidad",
        "Cerrajería",
        "Pintura",
        "Aseo y Limpieza",
        "Reparación de Electrodomésticos",
        "Carpintería"
    )

    val mapaEmojis = mapOf(
        "Plomería" to "🪠",
        "Electricidad" to "⚡",
        "Cerrajería" to "🔑",
        "Pintura" to "🎨",
        "Aseo y Limpieza" to "🧹",
        "Reparación de Electrodomésticos" to "🔌",
        "Carpintería" to "🪚"
    )

    var localidadesSeleccionadas by remember(initialLocalidades) {
        mutableStateOf(initialLocalidades.ifEmpty { listOf("Usaquén", "Chapinero", "Suba", "Teusaquillo") }.toSet())
    }

    var categoriasSeleccionadas by remember(initialCategorias) {
        mutableStateOf(
            initialCategorias
                .map { it.replace(Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]"), "").trim() }
                .ifEmpty { listOf("Plomería") }
                .toSet()
        )
    }

    var descripcion by remember(initialDescripcion) {
        mutableStateOf(initialDescripcion)
    }

    var listaPortafolio by remember(initialPortafolio) {
        mutableStateOf(initialPortafolio)
    }

    var videoParaReproducir by remember { mutableStateOf<Uri?>(null) }

    val maxBytesPermitidos = 5 * 1024 * 1024L

    val launcherMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            if (listaPortafolio.size >= 6) {
                Toast.makeText(context, "Has alcanzado el límite máximo de 6 archivos", Toast.LENGTH_SHORT).show()
                return@let
            }

            val pesoArchivo = obtenerTamanoArchivoBytes(context, selectedUri)

            if (pesoArchivo > maxBytesPermitidos) {
                Toast.makeText(
                    context,
                    "El archivo supera el límite de 5 MB. Por favor elige uno más liviano.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val esVideo = esVideoUri(context, selectedUri)
                listaPortafolio = listaPortafolio + ItemPortafolio(
                    uri = selectedUri,
                    titulo = if (esVideo) "Nuevo video" else "Nuevo trabajo",
                    esVideo = esVideo
                )
            }
        }
    }

    val orangeColor = Color(0xFFFF8F00)
    val maxCaracteres = 500

    videoParaReproducir?.let { uri ->
        VideoPlayerDialog(
            uri = uri,
            onDismiss = { videoParaReproducir = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFF111827)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Zona de Cobertura y Portafolio",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF111827)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "COBERTURA GEOGRÁFICA",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color(0xFF1F2937),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            todasLasLocalidades.forEach { localidad ->
                val selected = localidadesSeleccionadas.contains(localidad)

                SelectableChip(
                    text = localidad,
                    isSelected = selected,
                    accentColor = orangeColor,
                    onClick = {
                        localidadesSeleccionadas = if (selected) {
                            localidadesSeleccionadas - localidad
                        } else {
                            localidadesSeleccionadas + localidad
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PORTAFOLIO DE TRABAJOS REALIZADOS",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color(0xFF1F2937),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        DashedUploadButton(
            text = "+ Subir foto/video de trabajo previo (Máx. 6 y 5 MB c/u)",
            onClick = {
                if (listaPortafolio.size < 6) {
                    launcherMedia.launch("*/*")
                } else {
                    Toast.makeText(context, "Máximo 6 elementos permitidos", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0..1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val item = listaPortafolio.getOrNull(index)

                        Box(modifier = Modifier.weight(1f)) {
                            if (item != null) {
                                PortfolioItemCard(
                                    item = item,
                                    onClick = {
                                        if (item.esVideo && item.uri != null) {
                                            videoParaReproducir = item.uri
                                        }
                                    },
                                    onDelete = {
                                        listaPortafolio = listaPortafolio.filter { it.id != item.id }
                                    }
                                )
                            } else {
                                EmptyPortfolioCard()
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CATEGORÍAS DE SERVICIO",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color(0xFF1F2937),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            todasLasCategorias.forEach { categoria ->
                val selected = categoriasSeleccionadas.contains(categoria)
                val emoji = mapaEmojis[categoria] ?: ""

                SelectableChip(
                    text = "$emoji $categoria".trim(),
                    isSelected = selected,
                    accentColor = orangeColor,
                    onClick = {
                        categoriasSeleccionadas = if (selected) {
                            categoriasSeleccionadas - categoria
                        } else {
                            categoriasSeleccionadas + categoria
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DESCRIPCIÓN PROFESIONAL",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color(0xFF1F2937),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = {
                if (it.length <= maxCaracteres) descripcion = it
            },
            placeholder = {
                Text(
                    text = "Ej: Técnico certificado SENA con más de 8 años de experiencia en reparación de fugas, mantenimiento de calentadores y plomería general en Bogotá...",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFE5E7EB),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = Color(0xFF374151),
                lineHeight = 18.sp
            ),
            supportingText = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${descripcion.length} / $maxCaracteres",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    localidadesSeleccionadas.isEmpty() -> {
                        Toast.makeText(context, "Debes seleccionar al menos una localidad de cobertura", Toast.LENGTH_SHORT).show()
                    }
                    categoriasSeleccionadas.isEmpty() -> {
                        Toast.makeText(context, "Debes seleccionar al menos una categoría de servicio", Toast.LENGTH_SHORT).show()
                    }
                    listaPortafolio.isEmpty() -> {
                        Toast.makeText(context, "Debes agregar al menos una foto o video a tu portafolio", Toast.LENGTH_SHORT).show()
                    }
                    descripcion.trim().isEmpty() -> {
                        Toast.makeText(context, "Debes ingresar una descripción profesional", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        onGuardar(
                            localidadesSeleccionadas.toList(),
                            categoriasSeleccionadas.toList(),
                            listaPortafolio,
                            descripcion.trim()
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = orangeColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Actualizar Cobertura y Portafolio",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
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
private fun SelectableChip(
    text: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) accentColor else Color.White,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
        shadowElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF4B5563)
            )
        }
    }
}

@Composable
private fun DashedUploadButton(
    text: String,
    onClick: () -> Unit
) {
    val borderColor = Color(0xFF2196F3)
    val stroke = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F7FF))
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
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PortfolioItemCard(
    item: ItemPortafolio,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE0E0E0))
                .clickable { onClick() }
        ) {
            if (item.uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(item.uri),
                    contentDescription = item.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFC4C4C4))
                )
            }

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
                    .background(Color(0xFFEF5350))
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
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.titulo,
            fontSize = 10.sp,
            color = Color(0xFF4B5563),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyPortfolioCard() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF3F4F6),
                modifier = Modifier
                    .width(44.dp)
                    .height(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Vacío",
            fontSize = 10.sp,
            color = Color(0xFF9CA3AF)
        )
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