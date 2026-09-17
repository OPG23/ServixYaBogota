package com.servixyabogota.ui.provider

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servixyabogota.ui.components.TerminosYCondicionesDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargarDocumentosPrestadorScreen(
    viewModel: ProviderViewModel,
    onDocumentosEnviados: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.observeAsState(EstadoProveedorUiState())
    val isUploading by viewModel.isUploading.observeAsState(false)

    var profesionSeleccionada by remember { mutableStateOf(uiState.profesion.ifBlank { "Plomería" }) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var cedulaFrontalUri by remember { mutableStateOf<Uri?>(null) }
    var cedulaPosteriorUri by remember { mutableStateOf<Uri?>(null) }
    var antecedentesUri by remember { mutableStateOf<Uri?>(null) }
    var fotoPerfilUri by remember { mutableStateOf<Uri?>(null) }

    var mostrarTerminos by remember { mutableStateOf(false) }

    val launcherFotoPerfil = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { if (validarArchivo(context, it, permitirPdf = false)) fotoPerfilUri = it }
    }
    val launcherCedulaFrontal = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { if (validarArchivo(context, it, permitirPdf = false)) cedulaFrontalUri = it }
    }
    val launcherCedulaPosterior = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { if (validarArchivo(context, it, permitirPdf = false)) cedulaPosteriorUri = it }
    }
    val launcherAntecedentes = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { if (validarArchivo(context, it, permitirPdf = true)) antecedentesUri = it }
    }

    // Identificación de correcciones enviadas por el Administrador
    val rechazados = uiState.documentosRechazados
    val esModoCorreccion = rechazados.isNotEmpty()

    val requiereFoto = !esModoCorreccion || rechazados.contains("fotoPerfil")
    val requiereFrente = !esModoCorreccion || rechazados.contains("cedulaFrente")
    val requiereAtras = !esModoCorreccion || rechazados.contains("cedulaAtras")
    val requiereAntecedentes = !esModoCorreccion || rechazados.contains("antecedentes")

    val documentosHabilitados = (!requiereFoto || fotoPerfilUri != null) &&
            (!requiereFrente || cedulaFrontalUri != null) &&
            (!requiereAtras || cedulaPosteriorUri != null) &&
            (!requiereAntecedentes || antecedentesUri != null)

    val listaProfesiones = listOf(
        "🪠 Plomería",
        "⚡ Electricidad",
        "🔑 Cerrajería",
        "🎨 Pintura",
        "🧹 Aseo y Limpieza",
        "🔌 Reparación de Electrodomésticos",
        "🪚 Carpintería"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Cabecera
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onVolver) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Verificación de Identidad",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF111827)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mensaje azul/rojo informativo
        Surface(
            color = if (esModoCorreccion) Color(0xFFFFEBEE) else Color(0xFFE8F1FF),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (esModoCorreccion) Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (esModoCorreccion) Color(0xFFD32F2F) else Color(0xFF1976D2),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (esModoCorreccion)
                        "Por favor corrige únicamente los documentos señalados a continuación para completar la verificación."
                    else
                        "Para ofrecer servicios en Bogotá y recibir trabajos, necesitamos validar tu identidad. Revisión en máximo 24 horas.",
                    fontSize = 13.sp,
                    color = if (esModoCorreccion) Color(0xFFC62828) else Color(0xFF1565C0),
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selección de Profesión
        Text(text = "Especialidad o Oficio Principal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = expandedDropdown,
            onExpandedChange = { expandedDropdown = !expandedDropdown }
        ) {
            OutlinedTextField(
                value = profesionSeleccionada,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
            ExposedDropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false }
            ) {
                listaProfesiones.forEach { profesion ->
                    DropdownMenuItem(
                        text = { Text(profesion) },
                        onClick = {
                            profesionSeleccionada = profesion
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "DOCUMENTOS DE IDENTIDAD",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color(0xFF374151),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Tarjetas de subida de archivos
        DocumentUploadCardSelective(
            title = "Foto de Perfil (Rostro)",
            subtitle = "JPG / JPEG (Máx 2 MB)",
            icon = Icons.Default.AccountBox,
            fileUri = fotoPerfilUri,
            requiereCorreccion = requiereFoto,
            esModoCorreccion = esModoCorreccion,
            context = context,
            onClick = { launcherFotoPerfil.launch("image/*") },
            onRemove = { fotoPerfilUri = null }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DocumentUploadCardSelective(
            title = "Documento de Identidad (Frente)",
            subtitle = "JPG / JPEG (Máx 2 MB)",
            icon = Icons.Default.Badge,
            fileUri = cedulaFrontalUri,
            requiereCorreccion = requiereFrente,
            esModoCorreccion = esModoCorreccion,
            context = context,
            onClick = { launcherCedulaFrontal.launch("image/*") },
            onRemove = { cedulaFrontalUri = null }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DocumentUploadCardSelective(
            title = "Documento de Identidad (Anverso)",
            subtitle = "JPG / JPEG (Máx 2 MB)",
            icon = Icons.Default.Badge,
            fileUri = cedulaPosteriorUri,
            requiereCorreccion = requiereAtras,
            esModoCorreccion = esModoCorreccion,
            context = context,
            onClick = { launcherCedulaPosterior.launch("image/*") },
            onRemove = { cedulaPosteriorUri = null }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DocumentUploadCardSelective(
            title = "Antecedentes Judiciales (Policía)",
            subtitle = "PDF (Máx 5 MB) o JPG / JPEG (Máx 2 MB)",
            icon = Icons.Default.Description,
            fileUri = antecedentesUri,
            requiereCorreccion = requiereAntecedentes,
            esModoCorreccion = esModoCorreccion,
            context = context,
            onClick = { launcherAntecedentes.launch("*/*") },
            onRemove = { antecedentesUri = null }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Botón o Barra de Carga
        if (isUploading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1976D2))
            }
        } else {
            Button(
                enabled = documentosHabilitados,
                onClick = {
                    viewModel.subirDocumentosPrestador(
                        profesion = profesionSeleccionada,
                        fotoPerfil = if (requiereFoto) fotoPerfilUri else null,
                        cedulaFrente = if (requiereFrente) cedulaFrontalUri else null,
                        cedulaAtras = if (requiereAtras) cedulaPosteriorUri else null,
                        antecedentes = if (requiereAntecedentes) antecedentesUri else null,
                        onSuccess = {
                            Toast.makeText(context, "Documentos enviados con éxito", Toast.LENGTH_SHORT).show()
                            onDocumentosEnviados()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = if (esModoCorreccion) "Enviar Correcciones" else "Enviar a Verificación",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leyenda de Protección de Datos (Habeas Data) interactiva
        Text(
            text = "Al enviar tu información, autorizas el tratamiento de tus datos personales y aceptas que tus documentos serán procesados bajo la Ley 1581 de 2012 de Habeas Data y nuestros Términos y Condiciones.",
            fontSize = 11.sp,
            color = Color(0xFF1976D2),
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clickable { mostrarTerminos = true } // <-- HACE CLIC
        )

        // Mostrar el Dialog al hacer clic
        if (mostrarTerminos) {
            TerminosYCondicionesDialog(onDismiss = { mostrarTerminos = false })
        }
    }
}

@Composable
private fun DocumentUploadCardSelective(
    title: String,
    subtitle: String,
    icon: ImageVector,
    fileUri: Uri?,
    requiereCorreccion: Boolean,
    esModoCorreccion: Boolean,
    context: Context,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val fileSelected = fileUri != null
    val fileName = remember(fileUri) { fileUri?.let { obtenerNombreArchivo(context, it) } ?: "" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!requiereCorreccion && esModoCorreccion) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F4F8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF455A64), modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (!requiereCorreccion && esModoCorreccion) Color.Gray else Color(0xFF111827)
                    )
                    if (!fileSelected && requiereCorreccion) {
                        Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
                    }
                }

                // Insignias de Estado
                if (!requiereCorreccion && esModoCorreccion) {
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "Aprobado",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (requiereCorreccion && esModoCorreccion) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "Corregir",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!requiereCorreccion && esModoCorreccion) {
                Text(
                    text = "Este documento ya fue revisado y validado.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else if (fileSelected) {
                Surface(
                    color = Color(0xFFEDF7ED),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = fileName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20), maxLines = 1)
                        }
                        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar archivo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun obtenerNombreArchivo(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = c.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "archivo_seleccionado"
}

private fun validarArchivo(context: Context, uri: Uri, permitirPdf: Boolean): Boolean {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: ""
    var tamañoBytes = 0L
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex != -1 && cursor.moveToFirst()) {
            tamañoBytes = cursor.getLong(sizeIndex)
        }
    }

    val maxFotoBytes = 2 * 1024 * 1024
    val maxPdfBytes = 5 * 1024 * 1024
    val esJpg = mimeType.equals("image/jpeg", ignoreCase = true) || mimeType.equals("image/jpg", ignoreCase = true)
    val esPdf = mimeType.equals("application/pdf", ignoreCase = true)

    return when {
        esJpg -> if (tamañoBytes > maxFotoBytes) {
            Toast.makeText(context, "La imagen excede el límite de 2 MB.", Toast.LENGTH_LONG).show(); false
        } else true
        esPdf && permitirPdf -> if (tamañoBytes > maxPdfBytes) {
            Toast.makeText(context, "El PDF excede el límite de 5 MB.", Toast.LENGTH_LONG).show(); false
        } else true
        else -> {
            val msj = if (permitirPdf) "Formato no permitido. Solo JPG/JPEG o PDF." else "Formato no permitido. Solo JPG/JPEG."
            Toast.makeText(context, msj, Toast.LENGTH_LONG).show()
            false
        }
    }
}