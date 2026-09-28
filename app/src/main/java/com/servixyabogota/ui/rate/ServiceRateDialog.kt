package com.servixyabogota.ui.rate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Diálogo / Pantalla modal unificada para calificar servicios.
 * Sirve tanto para que el Cliente califique al Prestador,
 * como para que el Prestador califique al Cliente.
 */
@Composable
fun ServiceRateDialog(
    nombreEvaluado: String,
    fotoEvaluadoUrl: String = "",
    subtituloEvaluado: String = "", // Ej: "Plomería y Gas" o "Cliente"
    tituloSolicitud: String, // Ej: "Solicitud - Arreglo Fuga Lavamanos"
    esClienteEvaluando: Boolean = true,
    estaEnviando: Boolean = false,
    onDismiss: () -> Unit,
    onEnviarCalificacion: (calificacion: Int, comentario: String) -> Unit
) {
    var calificacion by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }

    val maxCaracteres = 280

    // Etiquetas según la puntuación seleccionada
    val textoPuntuacion = remember(calificacion) {
        when (calificacion) {
            1 -> "Muy malo"
            2 -> "Malo"
            3 -> "Aceptable"
            4 -> "¡Muy buen servicio!"
            5 -> "¡Excelente servicio!"
            else -> "Selecciona las estrellas"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. ENCABEZADO Y BOTÓN CERRAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evaluar Servicio",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. TARJETA DE USUARIO A EVALUAR Y DETALLE DE LA SOLICITUD
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar con foto o iniciales
                            if (fotoEvaluadoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = fotoEvaluadoUrl,
                                    contentDescription = nombreEvaluado,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = obtenerIniciales(nombreEvaluado),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = nombreEvaluado,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                if (subtituloEvaluado.isNotBlank()) {
                                    Text(
                                        text = subtituloEvaluado,
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Text(
                            text = tituloSolicitud,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // 3. TARJETA DE CALIFICACIÓN CON ESTRELLAS
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (esClienteEvaluando) "¿CÓMO CALIFICARÍAS EL TRABAJO?" else "¿CÓMO CALIFICARÍAS AL CLIENTE?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )

                        // Fila de 5 Estrellas
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { index ->
                                val estaSeleccionada = index <= calificacion
                                Icon(
                                    imageVector = if (estaSeleccionada) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Estrella $index",
                                    tint = if (estaSeleccionada) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { calificacion = index }
                                )
                            }
                        }

                        // Texto descriptivo dinámico de la calificación
                        Text(
                            text = textoPuntuacion,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (calificacion > 0) Color(0xFFF97316) else Color(0xFF94A3B8)
                        )
                    }
                }

                // 4. CAMPO DE COMENTARIOS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Comentarios adicionales",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = comentario,
                                onValueChange = {
                                    if (it.length <= maxCaracteres) {
                                        comentario = it
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = if (esClienteEvaluando) {
                                            "Escribe tu experiencia con el prestador (opcional)..."
                                        } else {
                                            "Escribe tu experiencia con el cliente (opcional)..."
                                        },
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp
                                    )
                                },
                                minLines = 3,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Contador de caracteres
                            Text(
                                text = "${comentario.length} / $maxCaracteres",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                // 5. BOTÓN DE ACCIÓN
                Button(
                    onClick = {
                        if (calificacion > 0) {
                            onEnviarCalificacion(calificacion, comentario.trim())
                        }
                    },
                    enabled = calificacion > 0 && !estaEnviando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        disabledContainerColor = Color(0xFF93C5FD)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (estaEnviando) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Enviar Calificación",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun obtenerIniciales(nombre: String): String {
    val palabras = nombre.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        palabras.isEmpty() -> "U"
        palabras.size == 1 -> palabras[0].take(2).uppercase()
        else -> "${palabras[0].first()}${palabras[1].first()}".uppercase()
    }
}