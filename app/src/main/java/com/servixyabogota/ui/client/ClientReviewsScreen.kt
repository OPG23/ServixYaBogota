package com.servixyabogota.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
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
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// Modelo exclusivo para la UI de esta pantalla
data class ReviewDisplayItem(
    val id: String = "",
    val nombre: String = "",
    val fotoUrl: String = "",
    val fecha: String = "",
    val calificacion: Int = 5,
    val comentario: String = ""
)

@Composable
fun ClientReviewsScreen(
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Otorgadas, 1 = Recibidas

    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var promedioCliente by remember { mutableStateOf(0.0) }
    var totalEvaluacionesCliente by remember { mutableStateOf(0) }

    var otorgadasReviews by remember { mutableStateOf<List<ReviewDisplayItem>>(emptyList()) }
    var recibidasReviews by remember { mutableStateOf<List<ReviewDisplayItem>>(emptyList()) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // 1. Datos de reputación del cliente
        db.collection("usuarios").document(currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val calif = snapshot.getDouble("calificacion")
                        ?: snapshot.getDouble("promedioCalificacion")
                        ?: 0.0
                    val total = snapshot.getLong("totalResenas")?.toInt() ?: 0

                    promedioCliente = calif
                    totalEvaluacionesCliente = total
                }
            }

        // 2. Escuchar reseñas de Firestore
        val queryResenas = db.collection("resenas")
            .where(
                Filter.or(
                    Filter.equalTo("autorId", currentUserId),
                    Filter.equalTo("clienteId", currentUserId),
                    Filter.equalTo("destinatarioId", currentUserId)
                )
            )

        queryResenas.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                scope.launch {
                    val listaOtorgadas = mutableListOf<ReviewDisplayItem>()
                    val listaRecibidas = mutableListOf<ReviewDisplayItem>()

                    for (doc in snapshot.documents) {
                        val autorId = doc.getString("autorId") ?: ""
                        val clienteId = doc.getString("clienteId") ?: ""
                        val destinatarioId = doc.getString("destinatarioId") ?: ""
                        val prestadorIdDoc = doc.getString("prestadorId") ?: ""
                        val tipo = doc.getString("tipo") ?: ""

                        val calificacion = doc.getLong("calificacion")?.toInt() ?: 5
                        val comentario = doc.getString("comentario") ?: ""
                        val timestamp = doc.getTimestamp("fecha")
                        val fechaStr = timestamp?.toDate()?.let { sdf.format(it) } ?: "Reciente"

                        // CLASIFICACIÓN RIGUROSA: Determinar si la reseña fue recibida o dada
                        val esRecibida: Boolean = when {
                            // 1. Prioridad por tipo de reseña explícito
                            tipo == "PRESTADOR_A_CLIENTE" -> true
                            tipo == "CLIENTE_A_PRESTADOR" -> false

                            // 2. Prioridad por autoría explícita
                            autorId.isNotBlank() && autorId == currentUserId -> false
                            autorId.isNotBlank() && autorId != currentUserId -> true

                            // 3. Prioridad por destinatario explícito
                            destinatarioId.isNotBlank() && destinatarioId == currentUserId -> true
                            destinatarioId.isNotBlank() && destinatarioId != currentUserId -> false

                            // 4. Fallback para documentos sin campos estandarizados
                            else -> doc.contains("clienteFotoUrl") || doc.contains("clienteFoto")
                        }

                        // Determinar el ID del prestador a consultar
                        val idPrestador = if (esRecibida) {
                            autorId.ifBlank { prestadorIdDoc }
                        } else {
                            destinatarioId.ifBlank { prestadorIdDoc }
                        }

                        // Buscar datos actualizados del prestador en la colección /usuarios
                        var nombreCompleto = ""
                        var fotoUrl = ""

                        if (idPrestador.isNotBlank() && idPrestador != currentUserId) {
                            try {
                                val userDoc = db.collection("usuarios").document(idPrestador).get().await()
                                if (userDoc.exists()) {
                                    val nom = userDoc.getString("nombre") ?: ""
                                    val ape = userDoc.getString("apellido") ?: userDoc.getString("apellidos") ?: ""
                                    val nombreDoc = userDoc.getString("nombreCompleto") ?: ""

                                    nombreCompleto = if (nombreDoc.isNotBlank()) nombreDoc else "$nom $ape".trim()

                                    fotoUrl = userDoc.getString("fotoUrl")
                                        ?: userDoc.getString("fotoPerfilUrl")
                                                ?: userDoc.getString("fotoPerfil")
                                                ?: userDoc.getString("foto")
                                                ?: ""
                                }
                            } catch (_: Exception) { }
                        }

                        // Fallbacks de nombre y foto si no existen en /usuarios
                        if (nombreCompleto.isBlank()) {
                            nombreCompleto = if (esRecibida) {
                                doc.getString("autorNombre")
                                    ?: doc.getString("prestadorNombre")
                                    ?: doc.getString("nombrePrestador")
                                    ?: "Prestador ServixYa"
                            } else {
                                doc.getString("destinatarioNombre")
                                    ?: doc.getString("prestadorNombre")
                                    ?: doc.getString("nombrePrestador")
                                    ?: "Prestador ServixYa"
                            }
                        }

                        if (fotoUrl.isBlank()) {
                            fotoUrl = if (esRecibida) {
                                doc.getString("autorFotoUrl")
                                    ?: doc.getString("autorFoto")
                                    ?: doc.getString("fotoUrl")
                                    ?: ""
                            } else {
                                doc.getString("destinatarioFotoUrl")
                                    ?: doc.getString("destinatarioFoto")
                                    ?: doc.getString("fotoUrl")
                                    ?: ""
                            }
                        }

                        val item = ReviewDisplayItem(
                            id = doc.id,
                            nombre = nombreCompleto,
                            fotoUrl = fotoUrl,
                            fecha = fechaStr,
                            calificacion = calificacion,
                            comentario = comentario
                        )

                        if (esRecibida) {
                            listaRecibidas.add(item)
                        } else {
                            listaOtorgadas.add(item)
                        }
                    }

                    otorgadasReviews = listaOtorgadas
                    recibidasReviews = listaRecibidas
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }
    }

    val currentReviews = if (selectedTab == 0) otorgadasReviews else recibidasReviews

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    text = "Mis Calificaciones Y Reseñas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // 2. TARJETA DE RESUMEN DE REPUTACIÓN
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (promedioCliente > 0) String.format(Locale.US, "%.1f", promedioCliente) else "5.0",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "como cliente",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Text(
                        text = "Basado en $totalEvaluacionesCliente evaluaciones",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 3. SEGMENTED TABS
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pestaña Otorgadas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 0) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Otorgadas a Prestadores",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else Color(0xFF475569)
                        )
                    }

                    // Pestaña Recibidas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 1) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Recibidas de Prestadores",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            // 4. LISTA DE RESEÑAS O ESTADO VACÍO
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (currentReviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "Aún no has otorgado ninguna reseña." else "Aún no has recibido reseñas de ningún prestador.",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    currentReviews.forEach { review ->
                        ReviewCard(review = review)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: ReviewDisplayItem) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (review.fotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = review.fotoUrl,
                        contentDescription = review.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    )
                } else {
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
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = review.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = review.fecha,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..5) {
                    val isFilled = i <= review.calificacion
                    Icon(
                        imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (isFilled) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (review.comentario.isNotBlank()) {
                Text(
                    text = review.comentario,
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )
            }
        }
    }
}