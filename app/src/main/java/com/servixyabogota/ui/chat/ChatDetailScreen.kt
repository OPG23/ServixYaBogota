package com.servixyabogota.ui.chat

import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ChatDetailScreen(
    chatId: String,
    currentUserId: String = "usuario_demo",
    esCliente: Boolean = true,
    interlocutorNombre: String = "",
    interlocutorFotoUrl: String? = null,
    subtituloOnline: String = "En línea",
    solicitudInfo: String? = null,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null,
    viewModel: ChatViewModel = viewModel(),
    onBack: () -> Unit = {},
    onVerPerfilPrestador: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var showOfertaDialog by remember { mutableStateOf(false) }

    var previewMediaUrl by remember { mutableStateOf<String?>(null) }
    var previewIsVideo by remember { mutableStateOf(false) }

    val colorTema = if (esCliente) Color(0xFF2563EB) else Color(0xFFF97316)

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val tamanoEnBytes = context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L

            val maximoPermitido = 5 * 1024 * 1024 // 5 MB

            if (tamanoEnBytes > maximoPermitido) {
                Toast.makeText(context, "El archivo supera el límite de 5 MB.", Toast.LENGTH_LONG).show()
            } else {
                selectedMediaUri = uri
            }
        }
    }

    LaunchedEffect(chatId, currentUserId, esCliente) {
        viewModel.inicializarChat(
            chatId = chatId,
            currentUserId = currentUserId,
            esCliente = esCliente
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val nombreMostrar = interlocutorNombre.ifBlank { uiState.nombreContraparte.ifEmpty { "Usuario" } }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.mensajes.size) {
        if (uiState.mensajes.isNotEmpty()) {
            listState.animateScrollToItem(uiState.mensajes.size - 1)
        }
    }

    if (showOfertaDialog) {
        CrearOfertaDialog(
            onDismiss = { showOfertaDialog = false },
            onEnviar = { monto, desc ->
                viewModel.enviarOferta(monto, desc)
                showOfertaDialog = false
            }
        )
    }

    if (previewMediaUrl != null) {
        if (previewIsVideo) {
            VideoPlayerDialog(
                videoUrl = previewMediaUrl!!,
                onDismiss = { previewMediaUrl = null }
            )
        } else {
            ImageViewerDialog(
                imageUrl = previewMediaUrl!!,
                onDismiss = { previewMediaUrl = null }
            )
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = esCliente && onVerPerfilPrestador != null) {
                                onVerPerfilPrestador?.invoke()
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFCBD5E1)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!interlocutorFotoUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = interlocutorFotoUrl,
                                        contentDescription = "Foto de perfil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = nombreMostrar,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (esCliente && onVerPerfilPrestador != null) "Ver perfil" else subtituloOnline,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (esCliente && onVerPerfilPrestador != null) Color(0xFF2563EB) else Color(0xFF16A34A)
                            )
                        }
                    }

                    IconButton(onClick = { /* Llamada */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Llamar",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                if (!solicitudInfo.isNullOrEmpty()) {
                    Surface(
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Build,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = solicitudInfo,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            if (!actionButtonText.isNullOrEmpty() && onActionButtonClick != null) {
                                Button(
                                    onClick = onActionButtonClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = actionButtonText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    selectedMediaUri?.let { uri ->
                        val esVideo = remember(uri) {
                            context.contentResolver.getType(uri)?.startsWith("video") == true
                        }

                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                                .size(72.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Adjunto seleccionado",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (esVideo) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .clickable { selectedMediaUri = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "Adjuntar Foto o Video",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Botón especial para que el Prestador Cotice/Ofrezca tarifa
                        if (!esCliente) {
                            IconButton(onClick = { showOfertaDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = "Ofrecer Tarifa",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text(
                                    text = if (selectedMediaUri != null) "Añade un comentario..." else "Escribe un mensaje...",
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colorTema)
                                .clickable {
                                    if (messageText.isNotBlank() || selectedMediaUri != null) {
                                        val texto = messageText.trim()
                                        val mediaAdjunto = selectedMediaUri

                                        messageText = ""
                                        selectedMediaUri = null

                                        if (mediaAdjunto != null) {
                                            viewModel.enviarMensajeConMedia(texto, mediaAdjunto, context)
                                        } else {
                                            viewModel.enviarMensaje(texto)
                                        }

                                        coroutineScope.launch {
                                            if (uiState.mensajes.isNotEmpty()) {
                                                listState.animateScrollToItem(uiState.mensajes.size - 1)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(18.dp)
                                    .offset(x = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.mensajes.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorTema
                )
            } else if (uiState.mensajes.isEmpty()) {
                Text(
                    text = "No hay mensajes aún. ¡Inicia la conversación!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(uiState.mensajes, key = { it.id }) { msg ->
                        ChatBubbleFirebase(
                            mensaje = msg,
                            isFromMe = msg.emisorId == currentUserId,
                            esCliente = esCliente,
                            colorTema = colorTema,
                            onMediaClick = { url, esVideo ->
                                previewMediaUrl = url
                                previewIsVideo = esVideo
                            },
                            onResponderOferta = { aceptada ->
                                viewModel.responderOferta(msg.id, aceptada)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleFirebase(
    mensaje: MensajeChat,
    isFromMe: Boolean,
    esCliente: Boolean,
    colorTema: Color = Color(0xFF2563EB),
    onMediaClick: (url: String, esVideo: Boolean) -> Unit = { _, _ -> },
    onResponderOferta: (aceptada: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val horaFormateada = remember(mensaje.fechaEnvio) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        formatter.format(mensaje.fechaEnvio)
    }

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        if (mensaje.esOferta) {
            // TARJETA DE OFERTA/COTIZACIÓN
            OfertaCard(
                mensaje = mensaje,
                isFromMe = isFromMe,
                esCliente = esCliente,
                colorTema = colorTema,
                onResponderOferta = onResponderOferta
            )
        } else {
            // MENSAJE DE TEXTO O MULTIMEDIA ESTÁNDAR
            Surface(
                color = if (isFromMe) colorTema else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (isFromMe) 4.dp else 16.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(all = 6.dp)) {
                    val mediaUrl = mensaje.mediaUrl.takeIf { !it.isNullOrEmpty() } ?: mensaje.imagenUrl.takeIf { it.isNotEmpty() }

                    if (!mediaUrl.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .clickable {
                                    onMediaClick(mediaUrl, mensaje.esVideo)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(mediaUrl)
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = "Multimedia adjunta",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (mensaje.esVideo) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Reproducir Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (mensaje.texto.isNotBlank()) {
                        Text(
                            text = mensaje.texto,
                            fontSize = 14.sp,
                            color = if (isFromMe) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = horaFormateada,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            if (isFromMe) {
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = "Enviado",
                    tint = colorTema,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// TARJETA DE OFERTA DENTRO DEL CHAT
// =======================================================
// 1. TARJETA DE OFERTA MEJORADA (DENTRO DEL CHAT)
// =======================================================
@Composable
fun OfertaCard(
    mensaje: MensajeChat,
    isFromMe: Boolean,
    esCliente: Boolean,
    colorTema: Color,
    onResponderOferta: (aceptada: Boolean) -> Unit
) {
    val formatoMoneda = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val montoFormateado = formatoMoneda.format(mensaje.montoOferta)

    // Configuración según el estado de la oferta
    val (estadoTexto, estadoBg, estadoFg, estadoIcono) = when (mensaje.estadoOferta) {
        "ACEPTADA" -> Quadruple("Aceptada", Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.CheckCircle)
        "RECHAZADA" -> Quadruple("Rechazada", Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.Cancel)
        else -> Quadruple("Pendiente", Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.HourglassTop)
    }

    val borderColor = when (mensaje.estadoOferta) {
        "ACEPTADA" -> Color(0xFF22C55E).copy(alpha = 0.4f)
        "RECHAZADA" -> Color(0xFFEF4444).copy(alpha = 0.3f)
        else -> Color(0xFFE2E8F0)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .width(285.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabecera estilizada tipo recibo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (mensaje.estadoOferta) {
                            "ACEPTADA" -> Color(0xFFF0FDF4)
                            "RECHAZADA" -> Color(0xFFFFF1F2)
                            else -> Color(0xFFF8FAFC)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colorTema.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = colorTema,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "COTIZACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Badge de Estado
                    Surface(
                        color = estadoBg,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = estadoIcono,
                                contentDescription = null,
                                tint = estadoFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = estadoTexto,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = estadoFg
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Cuerpo principal: Monto y Detalles
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "Valor de la propuesta",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = montoFormateado,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                if (mensaje.texto.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp).offset(y = 1.dp)
                            )
                            Text(
                                text = mensaje.texto,
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Acciones y estados de respuesta
                when {
                    esCliente && mensaje.estadoOferta == "PENDIENTE" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onResponderOferta(false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFFFF1F2),
                                    contentColor = Color(0xFFE11D48)
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Rechazar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onResponderOferta(true) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aceptar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    mensaje.estadoOferta == "ACEPTADA" -> {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (esCliente) "Has aceptado esta tarifa" else "El cliente aceptó la oferta",
                                    fontSize = 12.sp,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    mensaje.estadoOferta == "RECHAZADA" -> {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (esCliente) "Has rechazado esta tarifa" else "El cliente rechazó la oferta",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Esperando respuesta...",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// DIÁLOGO PARA CREAR OFERTA (PRESTADOR)
@Composable
fun CrearOfertaDialog(
    onDismiss: () -> Unit,
    onEnviar: (monto: Double, descripcion: String) -> Unit
) {
    var montoTexto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícono circular decorativo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF7ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEDD5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ofrecer Tarifa",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Define el costo del servicio para enviárselo al cliente.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Campo Monto
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            montoTexto = input
                        }
                    },
                    label = { Text("Valor / Tarifa (COP)") },
                    placeholder = { Text("Ej: 80000") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFF16A34A)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFFF97316)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Detalles adicionales (Opcional)") },
                    placeholder = { Text("Ej: Incluye repuestos y mano de obra...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFFF97316)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de Acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }

                    val esValido = (montoTexto.toDoubleOrNull() ?: 0.0) > 0

                    Button(
                        onClick = {
                            val monto = montoTexto.toDoubleOrNull() ?: 0.0
                            if (monto > 0) {
                                onEnviar(monto, descripcion.trim())
                            }
                        },
                        enabled = esValido,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF97316),
                            disabledContainerColor = Color(0xFFFED7AA)
                        )
                    ) {
                        Text(
                            text = "Enviar Oferta",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// VISOR DE IMÁGENES A PANTALLA COMPLETA
@Composable
fun ImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .zIndex(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen ampliada",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// REPRODUCTOR DE VIDEO A PANTALLA COMPLETA
@Composable
fun VideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .zIndex(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(videoUrl))
                        val mediaController = MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}