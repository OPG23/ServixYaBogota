
package com.servixyabogota.ui.client
/*
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
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class ChatMessageItem(
    val id: String,
    val text: String,
    val time: String,
    val isFromMe: Boolean
)

@Composable
fun ClientChatScreen(
    prestadorNombre: String = "Carlos Pérez",
    solicitudInfo: String = "Solicitud #1024 - Plomería",
    onBack: () -> Unit = {},
    onConfirmarServicio: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }

    val messages = remember {
        mutableStateListOf(
            ChatMessageItem("1", "Hola, vi tu solicitud sobre la fuga en el lavamanos. ¿Puedes enviarme una foto?", "10:30 AM", isFromMe = false),
            ChatMessageItem("2", "Claro, aquí te mando. Lleva goteando 2 días.", "10:32 AM", isFromMe = true),
            ChatMessageItem("3", "Perfecto, puedo ir hoy a las 3pm. ¿Te sirve?", "10:33 AM", isFromMe = false),
            ChatMessageItem("4", "¡Sí, perfecto! Te espero.", "10:34 AM", isFromMe = true)
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                // 1. TOP BAR DE CONTACTO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Avatar con indicador En Línea
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCBD5E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Punto verde "En Línea"
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prestadorNombre,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "En línea",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF16A34A)
                        )
                    }

                    // Botón de llamada
                    IconButton(onClick = { /* Llamar */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Llamar",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 2. BANNER DE CONTEXTO DE SOLICITUD
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Build,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = solicitudInfo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Button(
                            onClick = onConfirmarServicio,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Confirmar Servicio",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // 3. BARRA INFERIOR DE ENVÍO DE MENSAJE
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
                    // Botón Adjuntar
                    IconButton(onClick = { /* Adjuntar archivo */ }) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Adjuntar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Campo de texto
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

                    // Botón Enviar Circular
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                            .clickable {
                                if (messageText.isNotBlank()) {
                                    messages.add(
                                        ChatMessageItem(
                                            id = System
                                                .currentTimeMillis()
                                                .toString(),
                                            text = messageText.trim(),
                                            time = "10:35 AM",
                                            isFromMe = true
                                        )
                                    )
                                    messageText = ""
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
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
        // 4. LISTA DE MENSAJES
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isFromMe) Color(0xFF2563EB) else Color(0xFFE2E8F0),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                color = if (message.isFromMe) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Hora y Double Check para mensajes propios
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.time,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            if (message.isFromMe) {
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = "Leído",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
*/
