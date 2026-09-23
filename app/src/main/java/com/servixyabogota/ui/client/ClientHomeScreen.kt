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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    viewModel: ClientViewModel = viewModel(),
    onNavigateTab: (String) -> Unit = {},
    onIniciarChat: (ClientProviderModel) -> Unit = {},
    onVerPerfilPrestador: (ClientProviderModel) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("") }

    // Obtenemos los prestadores reales desde el ViewModel
    val listaPrestadores = viewModel.listaPrestadores
    val estaCargando = viewModel.estaCargandoPrestadores

    // CATEGORÍAS CON EMOJIS
    val categoriasList = remember {
        listOf(
            CategoriaItem("Plomería", "🪠"),
            CategoriaItem("Electricidad", "⚡"),
            CategoriaItem("Cerrajería", "🔑"),
            CategoriaItem("Pintura", "🎨"),
            CategoriaItem("Aseo y Limpieza", "🧹"),
            CategoriaItem("Reparación de Electrodomésticos", "🔌"),
            CategoriaItem("Carpintería", "🪚")
        )
    }

    // FILTRADO DINÁMICO POR CATEGORÍA Y BÚSQUEDA
    val prestadoresFiltrados = remember(listaPrestadores, categoriaSeleccionada, searchQuery) {
        listaPrestadores.filter { prestador ->
            val coincideCategoria = categoriaSeleccionada.isEmpty() ||
                    prestador.categorias.any { cat -> cat.contains(categoriaSeleccionada, ignoreCase = true) }
            val coincideBusqueda = searchQuery.isEmpty() ||
                    prestador.nombre.contains(searchQuery, ignoreCase = true) ||
                    prestador.categorias.any { cat -> cat.contains(searchQuery, ignoreCase = true) }

            coincideCategoria && coincideBusqueda
        }
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
            // 1. HEADER (Ubicación + Perfil Cliente)
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
                            text = viewModel.ciudad.ifBlank { "Bogotá, D.C." },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    AsyncImage(
                        model = viewModel.fotoUrl.ifBlank { "https://i.pravatar.cc/150?img=32" },
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
                            "Buscar por nombre o servicio...",
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
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color(0xFF64748B))
                            }
                        }
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

            // 3. BARRA DE CATEGORÍAS CON EMOJIS
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categoriasList) { cat ->
                        val isSelected = cat.nombre.equals(categoriaSeleccionada, ignoreCase = true)
                        Surface(
                            onClick = {
                                categoriaSeleccionada = if (isSelected) "" else cat.nombre
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color.White,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = cat.emoji, fontSize = 16.sp)
                                Text(
                                    text = cat.nombre,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }

            // 4. TÍTULO SECCIÓN Y RESULTADOS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (categoriaSeleccionada.isBlank()) "Todos los profesionales" else "Especialistas en $categoriaSeleccionada",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${prestadoresFiltrados.size} disponibles",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 5. CARGANDO O LISTADO DE PRESTADORES
            if (estaCargando) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                    }
                }
            } else if (prestadoresFiltrados.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron prestadores disponibles.",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(prestadoresFiltrados, key = { it.id }) { prestador ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        ProviderCard(
                            prestador = prestador,
                            onClickCard = { onVerPerfilPrestador(prestador) },
                            onIniciarChat = { onIniciarChat(prestador) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE TARJETA DE PRESTADOR
// ==========================================

@Composable
private fun ProviderCard(
    prestador: ClientProviderModel,
    onClickCard: () -> Unit,
    onIniciarChat: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickCard() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Foto de perfil + Badge Verificado
                Box(modifier = Modifier.size(64.dp)) {
                    AsyncImage(
                        model = prestador.fotoUrl.ifBlank { "https://i.pravatar.cc/150?img=11" },
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

                // Información
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

                    // Categorías
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        prestador.categorias.take(2).forEach { cat ->
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Barra Inferior: Estado y Botón de Chat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (prestador.disponibleHoy) Color(0xFF16A34A) else Color(0xFF94A3B8))
                    )
                    Text(
                        text = if (prestador.disponibleHoy) "Disponible hoy" else "No disponible hoy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (prestador.disponibleHoy) Color(0xFF15803D) else Color(0xFF64748B)
                    )
                }

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