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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale

// Alias por si lo utilizas con el nombre ProviderProfileScreen en ProviderMainContainer
@Composable
fun ProviderProfileScreen(
    nombre: String = "Carlos Pérez",
    esVerificado: Boolean = true,
    rating: String = "0.0",
    resenasCount: Int = 0,
    telefonoInicial: String = "",
    correoInicial: String = "",
    fotoUrl: String? = null,
    onIrAZonaCobertura: () -> Unit = {},
    onIrASeguridad: () -> Unit = {},
    onIrAResenas: () -> Unit = {},
    onGuardarCambios: (telefono: String, correo: String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {}
) {
    PerfilProfesionalScreen(
        nombre = nombre,
        esVerificado = esVerificado,
        rating = rating,
        resenasCount = resenasCount,
        telefonoInicial = telefonoInicial,
        correoInicial = correoInicial,
        fotoUrl = fotoUrl,
        onIrAZonaCobertura = onIrAZonaCobertura,
        onIrASeguridad = onIrASeguridad,
        onIrAResenas = onIrAResenas,
        onGuardarCambios = onGuardarCambios,
        onLogout = onLogout
    )
}

@Composable
fun PerfilProfesionalScreen(
    nombre: String = "Carlos Pérez",
    esVerificado: Boolean = true,
    rating: String = "0.0",
    resenasCount: Int = 0,
    telefonoInicial: String = "",
    correoInicial: String = "",
    fotoUrl: String? = null,
    onIrAZonaCobertura: () -> Unit = {},
    onIrASeguridad: () -> Unit = {},
    onIrAResenas: () -> Unit = {},
    onGuardarCambios: (telefono: String, correo: String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val orangeColor = Color(0xFFFF8F00)

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }

    // Estados reactivos dinámicos
    var nombreState by remember { mutableStateOf(nombre) }
    var fotoUrlState by remember { mutableStateOf(fotoUrl) }
    var esVerificadoState by remember { mutableStateOf(esVerificado) }
    var ratingState by remember { mutableStateOf(rating) }
    var resenasCountState by remember { mutableIntStateOf(resenasCount) }

    var telefono by remember { mutableStateOf(telefonoInicial) }
    var correo by remember { mutableStateOf(correoInicial) }
    var isSaving by remember { mutableStateOf(false) }

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Cargar y escuchar datos reales del prestador y sus reseñas en Firestore
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            onDispose { }
        } else {
            // 1. Escuchar perfil del usuario en Firestore
            val listenerUsuario = db.collection("usuarios").document(currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        nombreState = snapshot.getString("nombreCompleto")
                            ?: snapshot.getString("nombre")
                                    ?: nombreState

                        fotoUrlState = snapshot.getString("fotoUrl")
                            ?: snapshot.getString("fotoPerfilUrl")
                                    ?: snapshot.getString("foto")
                                    ?: fotoUrlState

                        val telDoc = snapshot.getString("telefono") ?: ""
                        if (telDoc.isNotBlank()) telefono = telDoc

                        val mailDoc = snapshot.getString("correo") ?: snapshot.getString("email") ?: ""
                        if (mailDoc.isNotBlank()) correo = mailDoc

                        esVerificadoState = snapshot.getBoolean("esVerificado")
                            ?: snapshot.getBoolean("verificado")
                                    ?: true

                        // Leer calificación previa si existe acumulada
                        val califNum = (snapshot.get("calificacion") as? Number)?.toDouble()
                            ?: (snapshot.get("rating") as? Number)?.toDouble()
                        if (califNum != null && califNum > 0) {
                            ratingState = String.format(Locale.US, "%.1f", califNum)
                        }

                        val countNum = (snapshot.get("totalResenas") as? Number)?.toInt()
                            ?: (snapshot.get("resenasCount") as? Number)?.toInt()
                        if (countNum != null) {
                            resenasCountState = countNum
                        }
                    }
                }

            // 2. Escuchar colección de reseñas reales para recalcular el promedio dinámico
            val listenerResenas = db.collection("resenas")
                .whereEqualTo("prestadorId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        val total = snapshot.documents.size
                        var suma = 0.0
                        var validos = 0
                        for (doc in snapshot.documents) {
                            val calif = (doc.get("calificacion") as? Number)?.toDouble()
                                ?: (doc.get("puntuacion") as? Number)?.toDouble()
                                ?: (doc.get("estrellas") as? Number)?.toDouble()
                            if (calif != null) {
                                suma += calif
                                validos++
                            }
                        }
                        if (validos > 0) {
                            val promedio = suma / validos
                            ratingState = String.format(Locale.US, "%.1f", promedio)
                            resenasCountState = total
                        }
                    }
                }

            onDispose {
                listenerUsuario.remove()
                listenerResenas.remove()
            }
        }
    }

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
                    if (!fotoUrlState.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(fotoUrlState),
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
                            text = nombreState.ifBlank { "Prestador de Servicios" },
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
                                text = ratingState,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = orangeColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "($resenasCountState reseñas)",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (esVerificadoState) {
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
            enabled = !isSaving,
            onClick = {
                if (currentUserId.isNotBlank()) {
                    isSaving = true
                    val datosActualizar = mapOf(
                        "telefono" to telefono,
                        "correo" to correo,
                        "email" to correo
                    )
                    db.collection("usuarios").document(currentUserId)
                        .set(datosActualizar, SetOptions.merge())
                        .addOnSuccessListener {
                            isSaving = false
                            onGuardarCambios(telefono, correo)
                            Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            isSaving = false
                            Toast.makeText(context, "Error al guardar en servidor", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    onGuardarCambios(telefono, correo)
                    Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = orangeColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = "Guardar Cambios de Perfil",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
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