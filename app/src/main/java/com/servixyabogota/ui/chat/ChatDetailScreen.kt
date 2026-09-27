package com.servixyabogota.ui.chat

import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Build
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatDetailScreen(
    chatId: String,
    currentUserId: String,
    esCliente: Boolean = true,
    interlocutorNombre: String = "",
    interlocutorApellido: String = "",
    interlocutorFotoUrl: String? = null,
    subtituloOnline: String = "En línea",
    solicitudInfo: String? = null,
    onVerSolicitudClick: (() -> Unit)? = null,
    viewModel: ChatViewModel = viewModel(),
    onBack: () -> Unit = {},
    onVerPerfilPrestador: ((idPrestador: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var showOfertaDialog by remember { mutableStateOf(false) }
    var showConfirmCompletarDialog by remember { mutableStateOf(false) }

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

            val maximoPermitido = 5 * 1024 * 1024

            if (tamanoEnBytes > maximoPermitido) {
                Toast.makeText(context, "El archivo supera el límite de 5 MB.", Toast.LENGTH_LONG).show()
            } else {
                selectedMediaUri = uri
            }
        }
    }

    LaunchedEffect(chatId, currentUserId, esCliente) {
        if (currentUserId.isNotBlank()) {
            viewModel.inicializarChat(
                chatId = chatId,
                currentUserId = currentUserId,
                esCliente = esCliente
            )
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    val nombreApellidoParametro = remember(interlocutorNombre, interlocutorApellido) {
        "$interlocutorNombre $interlocutorApellido".trim()
    }

    val fallbackRol = if (esCliente) "Prestador" else "Cliente"
    val nombreMostrar = remember(uiState.nombreContraparte, nombreApellidoParametro, esCliente) {
        when {
            uiState.nombreContraparte.isNotBlank() -> uiState.nombreContraparte
            nombreApellidoParametro.isNotBlank() && !nombreApellidoParametro.contains("Usuario") && !nombreApellidoParametro.contains("Prestador") && !nombreApellidoParametro.contains("Cliente") -> nombreApellidoParametro
            else -> fallbackRol
        }
    }

    val inicialesAvatar = remember(nombreMostrar) {
        val partes = nombreMostrar.trim().split("\\s+".toRegex())
        if (partes.size >= 2 && partes[0].isNotBlank() && partes[1].isNotBlank()) {
            "${partes[0].take(1)}${partes[1].take(1)}".uppercase()
        } else if (partes.isNotEmpty() && partes[0].isNotBlank()) {
            partes[0].take(1).uppercase()
        } else {
            "P"
        }
    }

    val fotoMostrar = interlocutorFotoUrl.takeIf { !it.isNullOrBlank() } ?: uiState.fotoContraparte

    val cotizacionAceptada = uiState.estadoPropuesta == "ACEPTADA" || uiState.mensajes.any { it.esOferta && it.estadoOferta == "ACEPTADA" }
    val servicioCompletado = uiState.estadoSolicitud == "COMPLETADO" || uiState.estadoSolicitud == "FINALIZADO"

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isPrimeraCarga by remember(chatId, currentUserId) { mutableStateOf(true) }
    var mensajesConocidosIds by remember(chatId, currentUserId) { mutableStateOf(setOf<String>()) }

    // Control de desplazamiento automático sin banner
    LaunchedEffect(uiState.mensajes, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect

        val mensajes = uiState.mensajes
        if (mensajes.isNotEmpty()) {
            val idsActuales = mensajes.map { it.id }.toSet()

            if (isPrimeraCarga) {
                mensajesConocidosIds = idsActuales
                listState.scrollToItem(mensajes.size - 1)
                isPrimeraCarga = false
            } else {
                val nuevosIds = idsActuales - mensajesConocidosIds
                if (nuevosIds.isNotEmpty()) {
                    listState.animateScrollToItem(mensajes.size - 1)
                    mensajesConocidosIds = idsActuales
                }
            }
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

    if (showConfirmCompletarDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCompletarDialog = false },
            title = { Text("Completar Servicio", fontWeight = FontWeight.Bold) },
            text = { Text("¿Confirmas que el servicio ha sido realizado satisfactoriamente?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCompletarDialog = false
                        viewModel.completarServicio(
                            onSuccess = {
                                Toast.makeText(context, "¡Servicio completado!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Sí, Completar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCompletarDialog = false }) {
                    Text("Cancelar")
                }
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
                            .clickable(enabled = esCliente && onVerPerfilPrestador != null && uiState.idContraparte.isNotBlank()) {
                                onVerPerfilPrestador?.invoke(uiState.idContraparte)
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (fotoMostrar.isNotBlank()) {
                                    AsyncImage(
                                        model = fotoMostrar,
                                        contentDescription = "Foto de perfil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = inicialesAvatar,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colorTema
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
                                fontSize = 16.sp,
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

                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Llamar",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                if (cotizacionAceptada && !servicioCompletado) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cotización aceptada • En proceso",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = if (esCliente) "Al finalizar el trabajo, marca el servicio como completado." else "Trata los detalles finales con el cliente.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534)
                                )
                            }

                            if (esCliente) {
                                Button(
                                    onClick = { showConfirmCompletarDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Completar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (servicioCompletado) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                text = "Servicio Completado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }

                if (!esCliente && !solicitudInfo.isNullOrEmpty()) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onVerSolicitudClick != null) {
                                onVerSolicitudClick?.invoke()
                            }
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Build,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = solicitudInfo,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (onVerSolicitudClick != null) {
                                        Text(
                                            text = "Toca para ver el detalle de la solicitud",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            if (onVerSolicitudClick != null) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Ver solicitud",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(22.dp)
                                )
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

                        if (!esCliente && !cotizacionAceptada && !servicioCompletado) {
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
            OfertaCard(
                mensaje = mensaje,
                isFromMe = isFromMe,
                esCliente = esCliente,
                colorTema = colorTema,
                onResponderOferta = onResponderOferta
            )
        } else {
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
                val colorCheck = if (mensaje.leido) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = if (mensaje.leido) "Leído" else "Enviado",
                    tint = colorCheck,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun OfertaCard(
    mensaje: MensajeChat,
    isFromMe: Boolean,
    esCliente: Boolean,
    colorTema: Color,
    onResponderOferta: (aceptada: Boolean) -> Unit
) {
    val formatoMoneda = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val montoFormateado = remember(mensaje.montoOferta) {
        try {
            formatoMoneda.format(mensaje.montoOferta)
        } catch (e: Exception) {
            "$${mensaje.montoOferta}"
        }
    }

    val estado = mensaje.estadoOferta

    Surface(
        color = when (estado) {
            "ACEPTADA" -> Color(0xFFF0FDF4)
            "RECHAZADA" -> Color(0xFFFEF2F2)
            else -> Color(0xFFFFFBEB)
        },
        border = BorderStroke(
            1.dp,
            when (estado) {
                "ACEPTADA" -> Color(0xFF86EFAC)
                "RECHAZADA" -> Color(0xFFFCA5A5)
                else -> Color(0xFDFCD34D)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = when (estado) {
                        "ACEPTADA" -> Color(0xFF16A34A)
                        "RECHAZADA" -> Color(0xFFDC2626)
                        else -> Color(0xFFD97706)
                    },
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Oferta de servicio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }

            Text(
                text = montoFormateado,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

            if (mensaje.texto.isNotBlank() && mensaje.texto != "Oferta de servicio enviada") {
                Text(
                    text = mensaje.texto,
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            }

            when (estado) {
                "ACEPTADA" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Oferta Aceptada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
                "RECHAZADA" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Oferta Rechazada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
                else -> {
                    if (!isFromMe && esCliente) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onResponderOferta(false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text("Rechazar", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onResponderOferta(true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text("Aceptar", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Pendiente de respuesta",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CrearOfertaDialog(
    onDismiss: () -> Unit,
    onEnviar: (monto: Double, descripcion: String) -> Unit
) {
    var montoTexto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enviar Oferta de Servicio", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it },
                    label = { Text("Monto ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción / Detalle (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoTexto.toDoubleOrNull() ?: 0.0
                    if (monto > 0) {
                        onEnviar(monto, descripcion)
                    }
                },
                enabled = (montoTexto.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("Enviar Oferta", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun VideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoPath(videoUrl)
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        start()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
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
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen ampliada",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
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