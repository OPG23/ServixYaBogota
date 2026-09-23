package com.servixyabogota.ui.client

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

// Helper para vincular el emoji correcto según la categoría
fun obtenerEmojiCategoria(categoria: String): String {
    return when (categoria.trim().lowercase()) {
        "plomería", "plomeria" -> "🪠"
        "electricidad" -> "⚡"
        "cerrajería", "cerrajeria" -> "🔑"
        "pintura" -> "🎨"
        "aseo y limpieza", "aseo" -> "🧹"
        "reparación de electrodomésticos", "reparacion de electrodomesticos" -> "🔌"
        "carpintería", "carpinteria" -> "🪚"
        else -> "🛠️"
    }
}

// Helper para detectar si la URL corresponde a un video
fun esVideoUrl(url: String): Boolean {
    val urlMin = url.lowercase()
    return urlMin.contains(".mp4") || urlMin.contains(".mov") || urlMin.contains(".webm") || urlMin.contains("video")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderDetailProfileScreen(
    prestador: ClientProviderModel,
    onBack: () -> Unit,
    onIniciarChat: () -> Unit
) {
    // Estado para controlar la foto/video que se está viendo en pantalla completa
    var selectedMediaUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil del Profesional", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = onIniciarChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Chat Directo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. TARJETA PRINCIPAL DEL PRESTADOR
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Foto con Badge Verificado
                    Box(modifier = Modifier.size(90.dp)) {
                        AsyncImage(
                            model = prestador.fotoUrl.ifBlank { "https://i.pravatar.cc/150?img=11" },
                            contentDescription = prestador.nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        if (prestador.verificado) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verificado",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = prestador.nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categorías con Emojis
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        prestador.categorias.forEach { cat ->
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = obtenerEmojiCategoria(cat),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Calificación y Experiencia
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem(
                            icon = Icons.Default.Star,
                            iconColor = Color(0xFFF59E0B),
                            valor = "${prestador.calificacion}",
                            etiqueta = "${prestador.totalResenas} reseñas"
                        )
                        MetricItem(
                            icon = Icons.Default.Work,
                            iconColor = Color(0xFF2563EB),
                            valor = "${prestador.experienciaAnos} años",
                            etiqueta = "Experiencia"
                        )
                    }
                }
            }

            // 2. SECCIÓN DE LOCALIDADES DE ATENCIÓN
            if (prestador.localidades.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Localidades de atención",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        prestador.localidades.forEach { localidad ->
                            Surface(
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = localidad,
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. SOBRE EL PROFESIONAL
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sobre el profesional",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prestador.descripcion.ifBlank { "Este profesional aún no ha añadido una descripción detallada." },
                        fontSize = 14.sp,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 4. PORTAFOLIO DE TRABAJOS (AMPLIABLE CON CLIC)
            if (prestador.portafolioUrls.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Portafolio de trabajos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(prestador.portafolioUrls) { mediaUrl ->
                            val esVideo = esVideoUrl(mediaUrl)

                            Box(
                                modifier = Modifier
                                    .size(140.dp, 100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .clickable { selectedMediaUrl = mediaUrl },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Trabajo portafolio",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Si es video, superponemos un ícono de reproducción (Play)
                                if (esVideo) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Reproducir Video",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. MODAL / DIALOG FULLSCREEN PARA MULTIMEDIA DETALLADA
    if (selectedMediaUrl != null) {
        FullscreenMediaDialog(
            mediaUrl = selectedMediaUrl!!,
            onDismiss = { selectedMediaUrl = null }
        )
    }
}

// ==========================================
// COMPOSABLE VISUALIZADOR PANTALLA COMPLETA
// ==========================================

@Composable
fun FullscreenMediaDialog(
    mediaUrl: String,
    onDismiss: () -> Unit
) {
    val esVideo = esVideoUrl(mediaUrl)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (esVideo) {
                // Reproductor nativo para videos
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.parse(mediaUrl))
                            val controller = MediaController(context)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            start()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.Center)
                )
            } else {
                // Visualizador de foto amplia
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = "Imagen ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }

            // Botón superior de Cierre (X)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
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

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    valor: String,
    etiqueta: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = valor, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
        }
        Text(text = etiqueta, fontSize = 12.sp, color = Color(0xFF64748B))
    }
}