package com.servixyabogota.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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
    var messageText by remember { mutableStateOf("") }

    // Color primario según rol: Azul para Cliente, Naranja para Prestador
    val colorTema = if (esCliente) Color(0xFF2563EB) else Color(0xFFF97316)

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

                    IconButton(onClick = { /* Lógica para llamadas */ }) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = { /* Lógica para adjuntar */ }) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Adjuntar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = {
                            Text(
                                text = "Escribe un mensaje...",
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
                            .background(colorTema) // BOTÓN DE ENVIAR EN NARANJA SI ES PRESTADOR
                            .clickable {
                                if (messageText.isNotBlank()) {
                                    val texto = messageText.trim()
                                    messageText = ""

                                    viewModel.enviarMensaje(texto)

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
                            colorTema = colorTema
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
    colorTema: Color = Color(0xFF2563EB) // TEMA POR DEFECTO
) {
    val horaFormateada = remember(mensaje.fechaEnvio) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        formatter.format(mensaje.fechaEnvio)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isFromMe) colorTema else Color(0xFFE2E8F0), // BURBUJA NARANJA SI ES PRESTADOR
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = mensaje.texto,
                fontSize = 14.sp,
                color = if (isFromMe) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                lineHeight = 20.sp
            )
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
                    tint = colorTema, // CHECKMARKS EN NARANJA SI ES PRESTADOR
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}