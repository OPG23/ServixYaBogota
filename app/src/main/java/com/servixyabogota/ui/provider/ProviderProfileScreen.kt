package com.servixyabogota.ui.provider

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun PerfilProfesionalScreen(
    nombre: String = "Carlos Pérez",
    esVerificado: Boolean = true,
    rating: String = "4.8",
    resenasCount: Int = 15,
    telefonoInicial: String = "+57 310 987 6543",
    correoInicial: String = "carlos.perez@servix.com",
    fotoUrl: String? = null,
    onIrAZonaCobertura: () -> Unit = {},
    onIrASeguridad: () -> Unit = {},
    onIrAResenas: () -> Unit = {},
    onGuardarCambios: (telefono: String, correo: String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var telefono by remember(telefonoInicial) { mutableStateOf(telefonoInicial) }
    var correo by remember(correoInicial) { mutableStateOf(correoInicial) }

    var showLogoutDialog by remember { mutableStateOf(false) }

    val orangeColor = Color(0xFFFF8F00)

    // Diálogo de confirmación para cerrar sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Estás seguro de que deseas salir de tu cuenta profesional?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                        onLogout()
                    }
                ) {
                    Text(text = "Sí, cerrar sesión", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // ENCABEZADO Y AVATAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Perfil Profesional",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color(0xFF111827)
                )

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                ) {
                    if (!fotoUrl.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(fotoUrl),
                            contentDescription = "Foto Perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TARJETA NOMBRE Y VERIFICACIÓN
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = nombre.ifBlank { "Carlos Pérez" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = orangeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = rating,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = orangeColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "($resenasCount reseñas)",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (esVerificado) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "VERIFICADO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN CONTACTO PROFESIONAL
            Text(
                text = "CONTACTO PROFESIONAL",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = Color(0xFF1F2937),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Teléfono de contacto profesional",
                fontSize = 12.sp,
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFE5E7EB),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Correo electrónico profesional",
                fontSize = 12.sp,
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFE5E7EB),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // MENÚ DE OPCIONES
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    PerfilMenuOption(
                        icon = Icons.Outlined.Lock,
                        title = "Seguridad y Notificaciones",
                        onClick = onIrASeguridad
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    PerfilMenuOption(
                        icon = Icons.Outlined.StarBorder,
                        title = "Mis Calificaciones y Reseñas",
                        onClick = onIrAResenas
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    PerfilMenuOption(
                        icon = Icons.Outlined.WorkOutline,
                        title = "Zona de Cobertura y Portafolio",
                        onClick = onIrAZonaCobertura
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OPCIÓN CERRAR SESIÓN
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFEE2E8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = "Cerrar Sesión",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Cerrar Sesión",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // BOTÓN DE GUARDAR
        Button(
            onClick = {
                onGuardarCambios(telefono, correo)
                Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = orangeColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Guardar Cambios de Perfil",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PerfilMenuOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF374151),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
        )
    }
}