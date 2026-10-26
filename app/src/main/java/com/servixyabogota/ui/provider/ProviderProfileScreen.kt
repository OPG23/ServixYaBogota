package com.servixyabogota.ui.provider

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun PerfilProfesionalScreen(
    viewModel: ProviderViewModel = viewModel(),
    nombre: String = "",
    fotoUrl: String = "",
    telefonoInicial: String = "",
    correoInicial: String = "",
    esVerificado: Boolean = true,
    onIrAZonaCobertura: () -> Unit = {},
    onIrASeguridad: () -> Unit = {},
    onIrAResenas: () -> Unit = {},
    onGuardarCambios: (String, String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val promedio = viewModel.promedioCalificacion
    val totalResenas = viewModel.totalResenasCount

    val tieneCalificaciones = totalResenas > 0 && promedio > 0.0
    val promedioTexto = if (tieneCalificaciones) String.format(Locale.US, "%.1f", promedio) else "-.-"

    var showLogoutDialog by remember { mutableStateOf(false) }

    // 🟢 ESTADOS PARA EDICIÓN DEL TELÉFONO
    var editandoTelefono by remember { mutableStateOf(false) }
    var telefonoInput by remember(telefonoInicial) { mutableStateOf(telefonoInicial) }

    LaunchedEffect(telefonoInicial) {
        telefonoInput = telefonoInicial
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Cerrar sesión 👋",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas salir de tu cuenta? 🚪",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sí, salir 🏃‍♂️", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Perfil Profesional",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // TARJETA PRINCIPAL DEL PRESTADOR
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = nombre.ifBlank { "Prestador ServixYa" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { onIrAResenas() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (tieneCalificaciones) Color(0xFFFF8F00) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = promedioTexto,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tieneCalificaciones) Color(0xFFFF8F00) else Color(0xFF64748B)
                            )
                            Text(
                                text = if (tieneCalificaciones) {
                                    "($totalResenas reseña${if (totalResenas != 1) "s" else ""})"
                                } else {
                                    "(Sin reseñas)"
                                },
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        if (esVerificado) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF166534),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "VERIFICADO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    }

                    if (fotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = fotoUrl,
                            contentDescription = "Foto de perfil",
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
                                .background(Color(0xFFCBD5E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "CONTACTO PROFESIONAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                modifier = Modifier.padding(top = 8.dp)
            )

            // 🟢 CAMPO TELÉFONO EDITABLE
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Teléfono de contacto profesional",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    if (!editandoTelefono) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { editandoTelefono = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar",
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Editar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8F00)
                            )
                        }
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (editandoTelefono) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = telefonoInput,
                                onValueChange = { telefonoInput = it },
                                label = { Text("Número de teléfono", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF8F00),
                                    focusedLabelColor = Color(0xFFFF8F00),
                                    cursorColor = Color(0xFFFF8F00)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        telefonoInput = telefonoInicial
                                        editandoTelefono = false
                                    }
                                ) {
                                    Text("Cancelar", color = Color(0xFF64748B), fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (telefonoInput.isBlank()) {
                                            Toast.makeText(context, "El teléfono no puede estar vacío", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        editandoTelefono = false
                                        onGuardarCambios(telefonoInput, correoInicial)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editandoTelefono = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = telefonoInicial.ifBlank { "Sin registrar (Toca para agregar)" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (telefonoInicial.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // CORREO ELECTRÓNICO (SOLO LECTURA)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Correo electrónico profesional",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = correoInicial.ifBlank { "Sin registrar" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // MENÚ DE OPCIONES DE CONFIGURACIÓN
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    OpcionPerfilItem(
                        icon = Icons.Outlined.Lock,
                        titulo = "Seguridad y Notificaciones",
                        onClick = onIrASeguridad
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    OpcionPerfilItem(
                        icon = Icons.Outlined.StarOutline,
                        titulo = "Mis Calificaciones y Reseñas",
                        onClick = onIrAResenas
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    OpcionPerfilItem(
                        icon = Icons.Outlined.WorkOutline,
                        titulo = "Zona de Cobertura y Portafolio",
                        onClick = onIrAZonaCobertura
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    OpcionPerfilItem(
                        icon = Icons.Outlined.HelpOutline,
                        titulo = "Contactar a Soporte",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:servixyabogota@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Soporte Prestador ServixYa - ${nombre.ifBlank { "Prestador" }}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se encontró una aplicación de correo instalada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // BOTÓN CERRAR SESIÓN
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD6D6)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Cerrar Sesión",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun OpcionPerfilItem(
    icon: ImageVector,
    titulo: String,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = titulo,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}