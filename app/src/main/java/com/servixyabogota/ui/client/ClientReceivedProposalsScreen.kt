package com.servixyabogota.ui.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage

data class PropuestaRecibidaUI(
    val prestadorId: String = "",
    val prestadorNombre: String = "",
    val prestadorFotoUrl: String = "",
    val calificacion: Double = 5.0,
    val totalResenas: Int = 0,
    val monto: Double = 0.0,
    val mensaje: String = ""
)

@Composable
fun ClientReceivedProposalsScreen(
    solicitudId: String,
    onBack: () -> Unit = {},
    onOpenChat: (solicitudId: String, prestadorId: String, nombrePrestador: String) -> Unit = { _, _, _ -> }
) {
    var listaPropuestas by remember { mutableStateOf<List<PropuestaRecibidaUI>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(solicitudId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("solicitudes")
            .document(solicitudId)
            .collection("propuestas")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val propuestas = snapshot.documents.mapNotNull { doc ->
                        PropuestaRecibidaUI(
                            prestadorId = doc.getString("prestadorId") ?: doc.id,
                            prestadorNombre = doc.getString("prestadorNombre") ?: "Prestador",
                            prestadorFotoUrl = doc.getString("prestadorFotoUrl") ?: "",
                            calificacion = doc.getDouble("calificacion") ?: 5.0,
                            totalResenas = doc.getLong("totalResenas")?.toInt() ?: 0,
                            monto = doc.getDouble("monto") ?: 0.0,
                            mensaje = doc.getString("mensaje") ?: ""
                        )
                    }
                    listaPropuestas = propuestas
                }
                cargando = false
            }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2563EB))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Propuestas Recibidas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${listaPropuestas.size} postulados",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (listaPropuestas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no hay prestadores postulados a esta solicitud.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listaPropuestas, key = { it.prestadorId }) { propuesta ->
                        TarjetaPropuestaItem(
                            propuesta = propuesta,
                            onOpenChat = {
                                onOpenChat(solicitudId, propuesta.prestadorId, propuesta.prestadorNombre)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPropuestaItem(
    propuesta: PropuestaRecibidaUI,
    onOpenChat: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (propuesta.prestadorFotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = propuesta.prestadorFotoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = propuesta.prestadorNombre.take(2).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = propuesta.prestadorNombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${propuesta.calificacion}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "(${propuesta.totalResenas})",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Text(
                    text = "$${propuesta.monto.toInt()} COP",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )
            }

            if (propuesta.mensaje.isNotBlank()) {
                Text(
                    text = "\"${propuesta.mensaje}\"",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Abrir Chat",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}