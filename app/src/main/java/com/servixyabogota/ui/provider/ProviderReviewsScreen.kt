package com.servixyabogota.ui.provider

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ReviewItem(
    val id: String,
    val nombre: String,
    val fecha: String,
    val calificacion: Int,
    val comentario: String
)

@Composable
fun ProviderReviewsScreen(
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Recibidas de Clientes, 1 = Otorgadas a Clientes

    val recibidasReviews = remember {
        listOf(
            ReviewItem(
                id = "1",
                nombre = "María López",
                fecha = "12/08/2026",
                calificacion = 5,
                comentario = "Excelente servicio de plomería, resolvió la fuga del baño rápidamente y dejó todo impecable. Muy recomendado."
            ),
            ReviewItem(
                id = "2",
                nombre = "Juan Rodríguez",
                fecha = "05/07/2026",
                calificacion = 4,
                comentario = "Buen trabajo y muy puntual. Solucionó el problema del calentador sin contratiempos."
            ),
            ReviewItem(
                id = "3",
                nombre = "Ana García",
                fecha = "20/06/2026",
                calificacion = 5,
                comentario = "Excelente experiencia. Muy profesional, amable y con muy buenas herramientas de trabajo."
            )
        )
    }

    val otorgadasReviews = remember {
        listOf(
            ReviewItem(
                id = "4",
                nombre = "María López",
                fecha = "12/08/2026",
                calificacion = 5,
                comentario = "Cliente muy amable, dio indicaciones claras para la llegada y realizó el pago oportunamente."
            ),
            ReviewItem(
                id = "5",
                nombre = "Juan Rodríguez",
                fecha = "05/07/2026",
                calificacion = 5,
                comentario = "Excelente comunicación, lugar listo para trabajar y pago al instante."
            )
        )
    }

    val currentReviews = if (selectedTab == 0) recibidasReviews else otorgadasReviews

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

            // 2. RESUMEN DE REPUTACIÓN COMO PRESTADOR
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
                            tint = Color(0xFFFF8F00),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "4.9",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8F00)
                        )
                        Text(
                            text = "como Prestador",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Text(
                        text = "Basado en 8 evaluaciones",
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
                    // Pestaña Recibidas de Clientes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 0) Color(0xFFFF8F00) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Recibidas de Clientes",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else Color(0xFF475569)
                        )
                    }

                    // Pestaña Otorgadas a Clientes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 1) Color(0xFFFF8F00) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Otorgadas a Clientes",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            // 4. LISTA DE RESEÑAS
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

@Composable
private fun ReviewCard(review: ReviewItem) {
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
                        tint = if (isFilled) Color(0xFFFF8F00) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = review.comentario,
                fontSize = 14.sp,
                color = Color(0xFF475569),
                lineHeight = 20.sp
            )
        }
    }
}