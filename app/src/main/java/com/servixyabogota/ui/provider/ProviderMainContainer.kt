package com.servixyabogota.ui.provider

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.servixyabogota.data.model.Solicitud
import com.servixyabogota.ui.chat.ChatDetailScreen
import com.servixyabogota.ui.chat.ChatViewModel
import com.servixyabogota.ui.client.ProviderSecuritySettingsScreen

@Composable
fun ProviderMainContainer(
    viewModel: ProviderViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    onIrACorregirDocumentos: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // Obtenemos el estado de forma segura e inferida
    val uiStateState = viewModel.uiState.observeAsState(EstadoProveedorUiState())
    val uiState = uiStateState.value ?: EstadoProveedorUiState()

    // NAVEGACIÓN PRINCIPAL
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Trabajos, 1: Mensajes, 2: Historial, 3: Perfil
    var subPantallaPerfil by remember { mutableStateOf("PERFIL") }

    // ESTADO DE CHAT EN PANTALLA COMPLETA
    var activeChat by remember { mutableStateOf<ChatItemUi?>(null) }

    // DETALLE DE SOLICITUD / POSTULACIÓN
    var solicitudSeleccionada by remember { mutableStateOf<Solicitud?>(null) }

    // DIÁLOGO DE RECHAZO
    var mostrarModalRechazo by remember(uiState.estadoVerificacion) {
        mutableStateOf(uiState.estadoVerificacion == "RECHAZADO")
    }

    // Modal de Rechazo de Documentación
    if (uiState.estadoVerificacion == "RECHAZADO" && mostrarModalRechazo) {
        AlertDialog(
            onDismissRequest = { },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Observaciones en tu Verificación",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFD32F2F)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Hola, ${uiState.nombreCompleto}. Se requieren correcciones:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Motivo: ${uiState.motivoRechazo}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                            if (uiState.justificacionRechazo.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.justificacionRechazo,
                                    fontSize = 12.sp,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarModalRechazo = false
                        onIrACorregirDocumentos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Corregir Documentos", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalRechazo = false }) {
                    Text("Cerrar", color = Color.Gray)
                }
            }
        )
    }

    // 1. SI HAY UN CHAT ACTIVO: Superpone ChatDetailScreen
    if (activeChat != null) {
        ChatDetailScreen(
            chatId = activeChat!!.id,
            currentUserId = currentUserId,
            esCliente = false,
            interlocutorNombre = activeChat!!.nombreCliente,
            solicitudInfo = activeChat!!.tituloSolicitud,
            onVerSolicitudClick = if (activeChat!!.tituloSolicitud != null) {
                {
                    Toast.makeText(
                        context,
                        "Mostrando detalles de: ${activeChat!!.tituloSolicitud}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else null,
            viewModel = chatViewModel,
            onBack = { activeChat = null }
        )
    }
    // 2. SI EL PRESTADOR ESTÁ APROBADO: Muestra la App Principal
    else if (uiState.estadoVerificacion == "APROBADO") {
        // Sub-pantalla de Detalle de Solicitud (al pulsar postularme)
        if (solicitudSeleccionada != null) {
            DetalleSolicitudScreen(
                solicitud = solicitudSeleccionada!!,
                onBack = { solicitudSeleccionada = null },
                onConfirmarPostulacion = { monto, propuesta ->
                    viewModel.postularASolicitud(
                        solicitudId = solicitudSeleccionada!!.id,
                        montoPropuesta = monto,
                        mensajePresentacion = propuesta,
                        onSuccess = {
                            Toast.makeText(context, "¡Postulación enviada con éxito!", Toast.LENGTH_SHORT).show()
                            solicitudSeleccionada = null // Cierra el detalle y vuelve al listado
                        },
                        onError = { mensajeError ->
                            Toast.makeText(context, mensajeError, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.BusinessCenter, contentDescription = "Trabajos") },
                            label = { Text("Trabajos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFF8F00),
                                selectedTextColor = Color(0xFFFF8F00),
                                indicatorColor = Color(0xFFFFF3E0),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Chat, contentDescription = "Mensajes") },
                            label = { Text("Mensajes", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFF8F00),
                                selectedTextColor = Color(0xFFFF8F00),
                                indicatorColor = Color(0xFFFFF3E0),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                            label = { Text("Historial", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFF8F00),
                                selectedTextColor = Color(0xFFFF8F00),
                                indicatorColor = Color(0xFFFFF3E0),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Mi Perfil") },
                            label = { Text("Mi Perfil", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFF8F00),
                                selectedTextColor = Color(0xFFFF8F00),
                                indicatorColor = Color(0xFFFFF3E0),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedTab) {
                        0 -> OportunidadesScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            onSeleccionarSolicitud = { solicitud ->
                                solicitudSeleccionada = solicitud
                            }
                        )
                        1 -> ProviderMessagesContainerScreen(
                            onOpenChat = { chat ->
                                activeChat = chat
                            }
                        )
                        2 -> HistorialTrabajosScreen()
                        3 -> {
                            when (subPantallaPerfil) {
                                "PERFIL" -> {
                                    PerfilProfesionalScreen(
                                        nombre = uiState.nombreCompleto,
                                        fotoUrl = uiState.fotoUrl,
                                        telefonoInicial = uiState.telefono,
                                        correoInicial = uiState.correo,
                                        esVerificado = true,
                                        onIrAZonaCobertura = { subPantallaPerfil = "PORTAFOLIO" },
                                        onIrASeguridad = { subPantallaPerfil = "SEGURIDAD" },
                                        onIrAResenas = { subPantallaPerfil = "RESENAS" },
                                        onGuardarCambios = { nuevoTelefono, nuevoCorreo ->
                                            viewModel.actualizarContactoPerfil(
                                                telefono = nuevoTelefono,
                                                correo = nuevoCorreo,
                                                onSuccess = {
                                                    Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { error ->
                                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        onLogout = onLogout
                                    )
                                }
                                "PORTAFOLIO" -> {
                                    ZonaCoberturaPortafolioContainer(
                                        viewModel = viewModel,
                                        onVolver = { subPantallaPerfil = "PERFIL" }
                                    )
                                }
                                "SEGURIDAD" -> {
                                    ProviderSecuritySettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { subPantallaPerfil = "PERFIL" }
                                    )
                                }
                                "RESENAS" -> {
                                    ProviderReviewsScreen(
                                        onBack = { subPantallaPerfil = "PERFIL" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // 3. SI AÚN NO ESTÁ APROBADO: Muestra la pantalla de estado
    else {
        ProviderNonApprovedScreen(
            uiState = uiState,
            onIrACorregirDocumentos = onIrACorregirDocumentos,
            onLogout = onLogout
        )
    }
}