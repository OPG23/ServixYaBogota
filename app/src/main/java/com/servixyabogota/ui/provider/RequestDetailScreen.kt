package com.servixyabogota.ui.provider

import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.servixyabogota.data.model.Solicitud
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private fun esVideoUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".mkv") ||
            lower.contains(".webm") || lower.contains(".avi") || lower.contains("video")
}

data class ReviewClienteItem(
    val id: String = "",
    val autorNombre: String = "Prestador ServixYa",
    val autorFotoUrl: String = "",
    val calificacion: Int = 5,
    val comentario: String = "",
    val fechaFormateada: String = "Reciente"
)

@Composable
fun DetalleSolicitudScreen(
    solicitud: Solicitud,
    onBack: () -> Unit,
    onConfirmarPostulacion: (monto: Double, propuesta: String) -> Unit
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var montoTexto by remember { mutableStateOf("") }
    var propuestaTexto by remember { mutableStateOf("") }
    var videoParaReproducir by remember { mutableStateOf<String?>(null) }
    var imagenParaVer by remember { mutableStateOf<String?>(null) }

    // ESTADOS DE RESEÑAS
    var mostrarModalResenas by remember { mutableStateOf(false) }
    var listaResenasCliente by remember { mutableStateOf<List<ReviewClienteItem>>(emptyList()) }
    var estaCargandoResenas by remember { mutableStateOf(true) }
    var promedioCliente by remember { mutableDoubleStateOf(0.0) }

    // ESTADOS DE VALIDACIÓN DE PROPUESTA PREVIA
    var yaTienePropuestaActiva by remember { mutableStateOf(false) }
    var propuestaExistenteMonto by remember { mutableStateOf<Double?>(null) }
    var propuestaExistenteTexto by remember { mutableStateOf("") }
    var cargandoEstadoPropuesta by remember { mutableStateOf(true) }

    val emoji = obtenerEmoji(solicitud.categoria)
    val esUrgente = solicitud.nivelUrgencia.equals("Urgente", ignoreCase = true)

    // 1. Carga de reseñas del cliente
    val clienteId = solicitud.clienteId
    LaunchedEffect(clienteId) {
        if (clienteId.isNotBlank()) {
            estaCargandoResenas = true
            val db = FirebaseFirestore.getInstance()

            db.collection("resenas")
                .whereEqualTo("destinatarioId", clienteId)
                .get()
                .addOnSuccessListener { snapshot1 ->
                    db.collection("resenas")
                        .whereEqualTo("clienteId", clienteId)
                        .get()
                        .addOnSuccessListener { snapshot2 ->
                            val docsUnicos = (snapshot1.documents + snapshot2.documents)
                                .distinctBy { it.id }
                                .filter { doc ->
                                    val tipo = doc.getString("tipo")
                                    tipo == "PRESTADOR_A_CLIENTE" || doc.getString("destinatarioId") == clienteId
                                }

                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val resenasTemp = docsUnicos.map { doc ->
                                val timestamp = doc.getTimestamp("fecha")
                                val fechaObj = timestamp?.toDate()
                                val calif = doc.getLong("calificacion")?.toInt() ?: 5

                                ReviewClienteItem(
                                    id = doc.id,
                                    autorNombre = doc.getString("autorNombre")
                                        ?: doc.getString("prestadorNombre")
                                        ?: "Prestador ServixYa",
                                    autorFotoUrl = doc.getString("autorFotoUrl")
                                        ?: doc.getString("prestadorFotoUrl")
                                        ?: "",
                                    calificacion = calif,
                                    comentario = doc.getString("comentario") ?: "",
                                    fechaFormateada = if (fechaObj != null) sdf.format(fechaObj) else "Reciente"
                                )
                            }

                            listaResenasCliente = resenasTemp
                            promedioCliente = if (resenasTemp.isNotEmpty()) {
                                resenasTemp.map { it.calificacion }.average()
                            } else {
                                0.0
                            }
                            estaCargandoResenas = false
                        }
                        .addOnFailureListener { estaCargandoResenas = false }
                }
                .addOnFailureListener { estaCargandoResenas = false }
        } else {
            estaCargandoResenas = false
        }
    }

    // 2. Verificación Robusta y Filtrada de Propuesta Existente
    LaunchedEffect(solicitud.id, currentUserId) {
        if (solicitud.id.isBlank() || currentUserId.isBlank()) {
            cargandoEstadoPropuesta = false
            return@LaunchedEffect
        }

        cargandoEstadoPropuesta = true
        val db = FirebaseFirestore.getInstance()
        val estadosInactivos = setOf("RECHAZADA", "RECHAZADO", "CANCELADA", "CANCELADO")

        fun verificarYEstablecerDoc(doc: DocumentSnapshot?): Boolean {
            if (doc != null && doc.exists()) {
                val estado = doc.getString("estado")?.uppercase() ?: "PENDIENTE"
                if (estado !in estadosInactivos) {
                    yaTienePropuestaActiva = true
                    propuestaExistenteMonto = doc.getDouble("monto")
                        ?: doc.getDouble("montoPropuesta")
                                ?: doc.getDouble("precioEstimado")
                                ?: doc.getDouble("precio")
                    propuestaExistenteTexto = doc.getString("mensaje")
                        ?: doc.getString("mensajePresentacion")
                                ?: doc.getString("propuesta")
                                ?: doc.getString("detalle")
                                ?: ""
                    cargandoEstadoPropuesta = false
                    return true
                }
            }
            return false
        }

        // Búsqueda 1: Documento directo por UID en solicitudes/{id}/propuestas/{uid}
        db.collection("solicitudes").document(solicitud.id)
            .collection("propuestas").document(currentUserId)
            .get()
            .addOnSuccessListener { docDirectoProp ->
                if (verificarYEstablecerDoc(docDirectoProp)) return@addOnSuccessListener

                // Búsqueda 2: Documento directo por UID en solicitudes/{id}/postulaciones/{uid}
                db.collection("solicitudes").document(solicitud.id)
                    .collection("postulaciones").document(currentUserId)
                    .get()
                    .addOnSuccessListener { docDirectoPost ->
                        if (verificarYEstablecerDoc(docDirectoPost)) return@addOnSuccessListener

                        // Búsqueda 3: Consulta en la subcolección 'propuestas' por prestadorId
                        db.collection("solicitudes").document(solicitud.id)
                            .collection("propuestas")
                            .get()
                            .addOnSuccessListener { snapPropSub ->
                                val docEncontrado = snapPropSub.documents.firstOrNull { doc ->
                                    val pId = doc.getString("prestadorId")
                                        ?: doc.getString("proveedorId")
                                        ?: doc.getString("idProveedor")
                                        ?: doc.getString("usuarioId")
                                        ?: doc.id
                                    pId == currentUserId
                                }

                                if (verificarYEstablecerDoc(docEncontrado)) return@addOnSuccessListener

                                // Búsqueda 4: Consulta en colección raíz 'postulaciones' con FILTROS
                                db.collection("postulaciones")
                                    .whereEqualTo("solicitudId", solicitud.id)
                                    .whereEqualTo("prestadorId", currentUserId)
                                    .get()
                                    .addOnSuccessListener { snapRaizPost ->
                                        val docRaiz = snapRaizPost.documents.firstOrNull()
                                        if (verificarYEstablecerDoc(docRaiz)) return@addOnSuccessListener

                                        // Búsqueda 5: Consulta en colección raíz 'propuestas' con FILTROS
                                        db.collection("propuestas")
                                            .whereEqualTo("solicitudId", solicitud.id)
                                            .whereEqualTo("prestadorId", currentUserId)
                                            .get()
                                            .addOnSuccessListener { snapRaizProp ->
                                                val docRaizProp = snapRaizProp.documents.firstOrNull()
                                                if (!verificarYEstablecerDoc(docRaizProp)) {
                                                    yaTienePropuestaActiva = false
                                                }
                                                cargandoEstadoPropuesta = false
                                            }
                                            .addOnFailureListener {
                                                yaTienePropuestaActiva = false
                                                cargandoEstadoPropuesta = false
                                            }
                                    }
                                    .addOnFailureListener {
                                        yaTienePropuestaActiva = false
                                        cargandoEstadoPropuesta = false
                                    }
                            }
                            .addOnFailureListener {
                                yaTienePropuestaActiva = false
                                cargandoEstadoPropuesta = false
                            }
                    }
                    .addOnFailureListener {
                        yaTienePropuestaActiva = false
                        cargandoEstadoPropuesta = false
                    }
            }
            .addOnFailureListener {
                yaTienePropuestaActiva = false
                cargandoEstadoPropuesta = false
            }
    }

    val estadosInactivosSolicitud = setOf(
        "EN_PROCESO", "COMPLETADA", "COMPLETADO",
        "FINALIZADA", "FINALIZADO", "CANCELADA", "CANCELADO"
    )
    val puedePostularse = solicitud.estado.uppercase() !in estadosInactivosSolicitud

    if (mostrarModalResenas) {
        ClienteResenasDialog(
            clienteNombre = solicitud.clienteNombre.ifBlank { "Cliente ServixYa" },
            promedio = promedioCliente,
            resenas = listaResenasCliente,
            estaCargando = estaCargandoResenas,
            onDismiss = { mostrarModalResenas = false }
        )
    }

    videoParaReproducir?.let { videoUrl ->
        SolicitudVideoPlayerDialog(
            videoUrl = videoUrl,
            onDismiss = { videoParaReproducir = null }
        )
    }

    imagenParaVer?.let { imageUrl ->
        SolicitudImageViewerDialog(
            imageUrl = imageUrl,
            onDismiss = { imagenParaVer = null }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Text(
                        text = "Detalle de Solicitud",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        },
        bottomBar = {
            if (puedePostularse && !yaTienePropuestaActiva && !cargandoEstadoPropuesta) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                val monto = montoTexto.toDoubleOrNull()
                                if (monto == null || monto <= 0) {
                                    Toast.makeText(context, "Ingresa un monto válido para la propuesta", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (propuestaTexto.trim().isEmpty()) {
                                    Toast.makeText(context, "Escribe una breve descripción de tu propuesta", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Actualiza el estado local de inmediato al confirmar
                                yaTienePropuestaActiva = true
                                propuestaExistenteMonto = monto
                                propuestaExistenteTexto = propuestaTexto.trim()

                                onConfirmarPostulacion(monto, propuestaTexto.trim())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Confirmar Postulación",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TARJETA CLIENTE
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF)
                        ) {
                            if (solicitud.clienteFotoUrl.isNotBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = solicitud.clienteFotoUrl),
                                    contentDescription = "Foto cliente",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val inicial = solicitud.clienteNombre.trim().take(1).uppercase().ifBlank { "C" }
                                    Text(
                                        text = inicial,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = solicitud.clienteNombre.ifBlank { "Cliente ServixYa" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cliente verificado · ${solicitud.localidad.ifBlank { "Bogotá" }}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { mostrarModalResenas = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Ver Reseñas",
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (promedioCliente > 0.0) String.format(Locale.US, "%.1f", promedioCliente) else if (estaCargandoResenas) "..." else "S/C",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            if (listaResenasCliente.isNotEmpty()) {
                                Text(
                                    text = "(${listaResenasCliente.size})",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // TARJETA ESPECIFICACIONES
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Especificaciones del Trabajo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Categoría Técnica", fontSize = 13.sp, color = Color(0xFF64748B))
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$emoji ${solicitud.categoria}",
                                color = Color(0xFFE65100),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Nivel de Urgencia", fontSize = 13.sp, color = Color(0xFF64748B))
                        Surface(
                            color = if (esUrgente) Color(0xFFFFEBEE) else Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (esUrgente) "Urgente 🚨" else solicitud.nivelUrgencia.ifBlank { "Normal" },
                                color = if (esUrgente) Color(0xFFD32F2F) else Color(0xFF2563EB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Dirección Aproximada", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text(
                            text = if (solicitud.direccion.isNotBlank()) solicitud.direccion else "Sector ${solicitud.localidad}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Descripción Completa:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = solicitud.detalleProblema,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // MULTIMEDIA
            if (solicitud.archivosUrls.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Fotos / Videos Adjuntos (${solicitud.archivosUrls.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            solicitud.archivosUrls.forEach { url ->
                                val esVid = esVideoUrl(url)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (esVid) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (esVid) {
                                                videoParaReproducir = url
                                            } else {
                                                imagenParaVer = url
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (esVid) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Reproducir Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "Ver Video",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = url),
                                            contentDescription = "Foto adjunta",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN DE PROPUESTA / AVISO DE PROPUESTA ENVIADA
            if (!puedePostularse) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Esta solicitud se encuentra en estado '${solicitud.estado.uppercase()}' y ya no admite nuevas propuestas.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else if (cargandoEstadoPropuesta) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF8F00),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else if (yaTienePropuestaActiva) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Propuesta Ya Enviada",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "En espera",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFFCD34D).copy(alpha = 0.5f), thickness = 1.dp)

                        if (propuestaExistenteMonto != null) {
                            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
                                maximumFractionDigits = 0
                            }
                            Text(
                                text = "Valor enviado: ${format.format(propuestaExistenteMonto)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        if (propuestaExistenteTexto.isNotBlank()) {
                            Text(
                                text = "\"$propuestaExistenteTexto\"",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "ℹ️ Ya enviaste una propuesta para esta solicitud. No es necesario enviar otra oferta.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tu Propuesta",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        OutlinedTextField(
                            value = montoTexto,
                            onValueChange = { montoTexto = it },
                            label = { Text("Valor Estimado / Visita ($)") },
                            placeholder = { Text("Ej: 50000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF8F00),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        OutlinedTextField(
                            value = propuestaTexto,
                            onValueChange = { propuestaTexto = it },
                            placeholder = {
                                Text(
                                    text = "Escribe un mensaje de presentación para el cliente describiendo cómo solucionarás su problema...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF8F00),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }
                }
            }
        }
    }
}

// DIÁLOGO DE CALIFICACIONES Y OPINIONES DEL CLIENTE
@Composable
private fun ClienteResenasDialog(
    clienteNombre: String,
    promedio: Double,
    resenas: List<ReviewClienteItem>,
    estaCargando: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .heightIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reputación del Cliente",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = clienteNombre,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (promedio > 0.0) String.format(Locale.US, "%.1f / 5.0", promedio) else "Sin promedio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${resenas.size} opiniones)",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309).copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (estaCargando) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF8F00))
                    }
                } else if (resenas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "💬", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aún no hay opiniones sobre este cliente",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(resenas, key = { it.id }) { resena ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = resena.autorNombre,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB800),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = resena.calificacion.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    if (resena.comentario.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = resena.comentario,
                                            fontSize = 12.sp,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// DIÁLOGOS MULTIMEDIA
@Composable
private fun SolicitudVideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(android.net.Uri.parse(videoUrl))
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setOnPreparedListener { mp: MediaPlayer ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SolicitudImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = "Imagen completa",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}