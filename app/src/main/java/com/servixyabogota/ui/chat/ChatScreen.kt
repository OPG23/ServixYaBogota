package com.servixyabogota.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AttachFile
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
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    solicitudId: String,
    currentUserId: String,
    esCliente: Boolean,
    categoriaSolicitud: String = "Plomería",
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onLlamarClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var mensajeTexto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // CORRECCIÓN: Se pasó 'chatId = solicitudId' a la función del ViewModel
    LaunchedEffect(solicitudId, currentUserId, esCliente) {
        viewModel.inicializarChat(
            chatId = solicitudId,
            currentUserId = currentUserId,
            esCliente = esCliente
        )
    }

    LaunchedEffect(uiState.mensajes.size) {
        if (uiState.mensajes.isNotEmpty()) {
            listState.animateScrollToItem(uiState.mensajes.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column {
                Surface(
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(60.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = Color.Black
                                )
                            }

                            Box {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFE2E8F0)
                                ) {
                                    if (uiState.fotoContraparte.isNotBlank()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = uiState.fotoContraparte),
                                            contentDescription = "Foto usuario",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            val inicial = uiState.nombreContraparte.trim().take(1).uppercase().ifBlank { "C" }
                                            Text(
                                                text = inicial,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color(0xFF1E88E5)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .border(1.5.dp, Color.White, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            Column {
                                Text(
                                    text = uiState.nombreContraparte.ifBlank { "Carlos Pérez" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "En línea",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = onLlamarClick) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Llamar",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Solicitud - ${categoriaSolicitud.ifBlank { "Plomería" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0D47A1)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { /* TODO: Adjuntar foto */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Adjuntar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedTextField(
                        value = mensajeTexto,
                        onValueChange = { mensajeTexto = it },
                        placeholder = {
                            Text(
                                text = "Escribe un mensaje...",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9)
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (mensajeTexto.isNotBlank()) {
                                viewModel.enviarMensaje(mensajeTexto)
                                mensajeTexto = ""
                            }
                        },
                        enabled = mensajeTexto.isNotBlank(),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1E88E5),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF1E88E5)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(uiState.mensajes, key = { it.id }) { mensaje ->
                        val esMio = mensaje.emisorId == currentUserId

                        BurbujaMensajeExacta(
                            mensaje = mensaje,
                            esMio = esMio,
                            esCliente = esCliente,
                            estadoPropuesta = uiState.estadoPropuesta,
                            onConfirmarServicio = { viewModel.aceptarPropuesta() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BurbujaMensajeExacta(
    mensaje: MensajeChat,
    esMio: Boolean,
    esCliente: Boolean,
    estadoPropuesta: String,
    onConfirmarServicio: () -> Unit
) {
    val horaFormateada = remember(mensaje.fechaEnvio) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(mensaje.fechaEnvio)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (esMio) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (esMio) Color(0xFF1E88E5) else Color(0xFFE5E5E5),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(
                text = mensaje.texto,
                fontSize = 15.sp,
                color = if (esMio) Color.White else Color(0xFF1F2937),
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = horaFormateada,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            if (esMio) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        if (!esMio && esCliente && (mensaje.esPropuesta || mensaje.montoPropuesta > 0) && estadoPropuesta == "PENDIENTE") {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onConfirmarServicio,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Confirmar Servicio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}