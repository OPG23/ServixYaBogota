package com.servixyabogota.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

// ==========================================
// MODELOS DE DATOS MOCK
// ==========================================

data class CategoriaQuickFilter(
    val id: String,
    val nombre: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector
)

data class PrestadorClienteModel(
    val id: String,
    val nombre: String,
    val fotoUrl: String,
    val calificacion: Double,
    val totalResenas: Int,
    val categorias: List<String>,
    val disponibleHoy: Boolean = true,
    val verificado: Boolean = true
)

// ==========================================
// COMPOSABLE PRINCIPAL
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    onLogout: () -> Unit = {},
    onNavigateTab: (String) -> Unit = {},
    onIniciarChat: (PrestadorClienteModel) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Plomería") }
    var selectedBottomTab by remember { mutableStateOf("Inicio") }

    val categoriasList = remember {
        listOf(
            CategoriaQuickFilter("1", "Plomería", Icons.Default.Build),
            CategoriaQuickFilter("2", "Electricidad", Icons.Default.ElectricBolt),
            CategoriaQuickFilter("3", "Pintura", Icons.Default.FormatPaint),
            CategoriaQuickFilter("4", "Aseo", Icons.Default.CleaningServices)
        )
    }

    val listaPrestadores = remember {
        listOf(
            PrestadorClienteModel(
                id = "1",
                nombre = "Carlos Pérez",
                fotoUrl = "https://i.pravatar.cc/150?img=11",
                calificacion = 4.8,
                totalResenas = 15,
                categorias = listOf("Plomería", "Gas")
            ),
            PrestadorClienteModel(
                id = "2",
                nombre = "María López",
                fotoUrl = "https://i.pravatar.cc/150?img=47",
                calificacion = 4.9,
                totalResenas = 32,
                categorias = listOf("Electricidad")
            ),
            PrestadorClienteModel(
                id = "3",
                nombre = "Juan Rodríguez",
                fotoUrl = "https://i.pravatar.cc/150?img=68",
                calificacion = 4.6,
                totalResenas = 8,
                categorias = listOf("Pintura", "Aseo")
            )
        )
    }

    Scaffold(
        bottomBar = {
            ClientBottomNavigation(
                selectedTab = "Inicio",
                onTabSelected = onNavigateTab
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER (Ubicación + Perfil)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Ubicación",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Usaquén, Bogotá",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    AsyncImage(
                        model = "https://i.pravatar.cc/150?img=32",
                        contentDescription = "Perfil Cliente",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    )
                }
            }

            // 2. BUSCADOR
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Buscar plomero, electricista...",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
            }

            // 3. CHIPS CATEGORÍAS RÁPIDAS
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categoriasList) { item ->
                        val isSelected = item.nombre == categoriaSeleccionada
                        Surface(
                            onClick = { categoriaSeleccionada = item.nombre },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color.White,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = item.icono,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = item.nombre,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }

            // 4. BOTONES DE FILTRO Y ORDENAMIENTO
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filtrar por Categoría
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text(
                            text = "Filtrar por Categoría",
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Ordenar por estrellas
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text(
                            text = "★ Ordenar por estrellas",
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 5. LISTA DE PRESTADORES
            items(listaPrestadores, key = { it.id }) { prestador ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    PrestadorCard(
                        prestador = prestador,
                        onIniciarChat = { onIniciarChat(prestador) }
                    )
                }
            }
        }
    }
}

// ==========================================
// TARJETA DE PRESTADOR
// ==========================================

@Composable
private fun PrestadorCard(
    prestador: PrestadorClienteModel,
    onIniciarChat: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Foto de perfil con Badge de Verificado
                Box(modifier = Modifier.size(64.dp)) {
                    AsyncImage(
                        model = prestador.fotoUrl,
                        contentDescription = prestador.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    )
                    if (prestador.verificado) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Verificado",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info del Prestador
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prestador.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Calificación y Reseñas
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prestador.calificacion.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${prestador.totalResenas})",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips de Categorías
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        prestador.categorias.forEach { cat ->
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Fila Inferior: Estado Disponible + Botón Iniciar Chat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge "Disponible hoy"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Text(
                        text = "Disponible hoy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }

                // Botón Iniciar Chat
                OutlinedButton(
                    onClick = onIniciarChat,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Iniciar chat",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
