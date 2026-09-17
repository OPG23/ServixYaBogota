package com.servixyabogota.ui.client

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servixyabogota.ui.provider.ProviderViewModel

@Composable
fun ProviderSecuritySettingsScreen(
    viewModel: ProviderViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // Estados de las contraseñas
    var contrasenaActual by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }

    // Estados para ver/ocultar texto (ojo)
    var verActual by remember { mutableStateOf(false) }
    var verNueva by remember { mutableStateOf(false) }
    var verConfirmar by remember { mutableStateOf(false) }

    // Estado de carga para contraseña
    var estaCargando by remember { mutableStateOf(false) }

    // Estados para Preferencias de Notificación (sincronizados con Firestore)
    var notificacionesPushChat by remember { mutableStateOf(true) }
    var alertasCorreo by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Seguridad y Ajustes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // 2. PREFERENCIAS DE NOTIFICACIÓN (Diseño de la imagen)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PREFERENCIAS DE NOTIFICACIÓN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Opción 1: Notificaciones Push de Chat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notificaciones Push de Chat",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A)
                            )

                            Switch(
                                checked = notificacionesPushChat,
                                onCheckedChange = { nuevoEstado ->
                                    notificacionesPushChat = nuevoEstado
                                    viewModel.actualizarPreferenciasNotificaciones(
                                        notificacionesPushChat = nuevoEstado,
                                        alertasCorreo = alertasCorreo,
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF8F00),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp
                        )

                        // Opción 2: Alertas por Correo Electrónico
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alertas por Correo Electrónico",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A)
                            )

                            Switch(
                                checked = alertasCorreo,
                                onCheckedChange = { nuevoEstado ->
                                    alertasCorreo = nuevoEstado
                                    viewModel.actualizarPreferenciasNotificaciones(
                                        notificacionesPushChat = notificacionesPushChat,
                                        alertasCorreo = nuevoEstado,
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF8F00),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            // 3. SECCIÓN CAMBIAR CONTRASEÑA
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFF8F00),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Cambiar Contraseña",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Text(
                        text = "Ingresa tu contraseña actual y la nueva contraseña para actualizar la seguridad de tu cuenta.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // FIELD 1: CONTRASEÑA ACTUAL
                    OutlinedTextField(
                        value = contrasenaActual,
                        onValueChange = { contrasenaActual = it },
                        label = { Text("Contraseña Actual") },
                        singleLine = true,
                        visualTransformation = if (verActual) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verActual = !verActual }) {
                                Icon(
                                    imageVector = if (verActual) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (verActual) "Ocultar" else "Mostrar",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF8F00),
                            focusedLabelColor = Color(0xFFFF8F00)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // FIELD 2: NUEVA CONTRASEÑA
                    OutlinedTextField(
                        value = nuevaContrasena,
                        onValueChange = { nuevaContrasena = it },
                        label = { Text("Nueva Contraseña") },
                        singleLine = true,
                        visualTransformation = if (verNueva) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verNueva = !verNueva }) {
                                Icon(
                                    imageVector = if (verNueva) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (verNueva) "Ocultar" else "Mostrar",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF8F00),
                            focusedLabelColor = Color(0xFFFF8F00)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // FIELD 3: CONFIRMAR NUEVA CONTRASEÑA
                    OutlinedTextField(
                        value = confirmarContrasena,
                        onValueChange = { confirmarContrasena = it },
                        label = { Text("Confirmar Nueva Contraseña") },
                        singleLine = true,
                        visualTransformation = if (verConfirmar) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verConfirmar = !verConfirmar }) {
                                Icon(
                                    imageVector = if (verConfirmar) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (verConfirmar) "Ocultar" else "Mostrar",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF8F00),
                            focusedLabelColor = Color(0xFFFF8F00)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // BOTÓN ACTUALIZAR
                    Button(
                        onClick = {
                            when {
                                contrasenaActual.isBlank() -> {
                                    Toast.makeText(context, "Ingresa tu contraseña actual", Toast.LENGTH_SHORT).show()
                                }
                                nuevaContrasena.length < 6 -> {
                                    Toast.makeText(context, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                                }
                                nuevaContrasena != confirmarContrasena -> {
                                    Toast.makeText(context, "Las nuevas contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    estaCargando = true
                                    viewModel.cambiarContrasena(
                                        contrasenaActual = contrasenaActual,
                                        nuevaContrasena = nuevaContrasena,
                                        onSuccess = {
                                            estaCargando = false
                                            contrasenaActual = ""
                                            nuevaContrasena = ""
                                            confirmarContrasena = ""
                                            Toast.makeText(context, "¡Contraseña actualizada exitosamente!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { errorMsg ->
                                            estaCargando = false
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !estaCargando,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (estaCargando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Actualizar Contraseña",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}