package com.servixyabogota.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servixyabogota.ui.client.NotificacionInApp
import com.servixyabogota.ui.client.TipoNotificacion
import kotlinx.coroutines.delay

@Composable
fun InAppNotificationBanner(
    notificacion: NotificacionInApp?,
    onDismiss: () -> Unit,
    onClick: () -> Unit = {}
) {
    // Autocerrar la alerta después de 4.5 segundos
    LaunchedEffect(notificacion) {
        if (notificacion != null) {
            delay(4500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notificacion != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (notificacion != null) {
            Surface(
                color = if (notificacion.tipo == TipoNotificacion.NUEVO_MENSAJE) Color(0xFF0F172A) else Color(0xFF1E3A8A),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable {
                        onClick()
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (notificacion.tipo == TipoNotificacion.NUEVO_MENSAJE) Icons.Default.ChatBubble else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notificacion.titulo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = notificacion.mensaje,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            maxLines = 2
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}