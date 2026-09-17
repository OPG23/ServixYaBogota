package com.servixyabogota.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val tag: String? = null,
    val tagColor: Color = Color.Unspecified,
    val tagBgColor: Color = Color.Unspecified,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconColor: Color
)

@Composable
fun OnboardingScreen(onFinishOnboarding: () -> Unit) {
    val pages = listOf(
        OnboardingPageData(
            title = "¿Necesitas ayuda en casa?",
            description = "Encuentra profesionales verificados cerca de ti en cuestión de minutos.",
            icon = Icons.Default.Build,
            iconBgColor = Color(0xFF1976D2),
            iconColor = Color.White
        ),
        OnboardingPageData(
            tag = "CONFIANZA",
            tagColor = Color(0xFF2E7D32),
            tagBgColor = Color(0xFFE8F5E9),
            title = "Profesionales Verificados",
            description = "Todos nuestros proveedores pasan por un riguroso proceso de verificación de identidad para tu tranquilidad.",
            icon = Icons.Default.Shield,
            iconBgColor = Color(0xFF2E7D32),
            iconColor = Color.White
        ),
        OnboardingPageData(
            tag = "TRANSPARENCIA",
            tagColor = Color(0xFFE65100),
            tagBgColor = Color(0xFFFFF3E0),
            title = "Calificaciones Reales",
            description = "Toma decisiones informadas leyendo reseñas honestas de clientes que ya han contratado sus servicios.",
            icon = Icons.Default.Star,
            iconBgColor = Color(0xFFFB8C00),
            iconColor = Color.White
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // TopBar (Logo + Botón Saltar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ServixYaBogota",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1976D2)
                )
            }

            if (pagerState.currentPage > 0) {
                TextButton(onClick = onFinishOnboarding) {
                    Text("Saltar", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager Central
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(page.iconBgColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(page.iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = page.iconColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                page.tag?.let { tagText ->
                    Surface(
                        color = page.tagBgColor,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = tagText,
                            color = page.tagColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = page.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1D1B20)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = page.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Barra Inferior (Indicador + Botón)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF1976D2) else Color.LightGray)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinishOnboarding()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = when (pagerState.currentPage) {
                        0 -> "Comenzar"
                        pages.size - 1 -> "Ingresar"
                        else -> "Siguiente"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}