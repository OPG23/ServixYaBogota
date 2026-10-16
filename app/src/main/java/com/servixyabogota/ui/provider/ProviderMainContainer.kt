package com.servixyabogota.ui.provider

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
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

    val uiStateState = viewModel.uiState.observeAsState(EstadoProveedorUiState())
    val uiState = uiStateState.value ?: EstadoProveedorUiState()

    val solicitudesDisponibles by viewModel.getSolicitudesDisponibles(
        misCategorias = uiState.categorias,
        misLocalidades = emptyList()
    ).collectAsState(initial = emptyList())

    // 🟢 Evalúa si el prestador aprobado requiere completar su configuración inicial
    val requiereOnboarding = remember(uiState.localidades, uiState.descripcion, uiState.portafolioUrls) {
        uiState.localidades.isEmpty() || uiState.descripcion.isBlank()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var subPantallaPerfil by remember { mutableStateOf("PERFIL") }

    var activeChat by remember { mutableStateOf<ChatItemUi?>(null) }
    var solicitudSeleccionada by remember { mutableStateOf<Solicitud?>(null) }

    var mostrarModalRechazo by remember(uiState.estadoVerificacion) {
        mutableStateOf(uiState.estadoVerificacion == "RECHAZADO")
    }

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

    if (activeChat != null) {
        val currentChat = activeChat!!

        ChatDetailScreen(
            chatId = currentChat.id,
            currentUserId = currentUserId,
            esCliente = false,
            interlocutorNombre = currentChat.nombreCliente,
            solicitudInfo = currentChat.tituloSolicitud,
            onVerSolicitudClick = if (!currentChat.tituloSolicitud.isNullOrEmpty()) {
                {
                    val localFound = solicitudesDisponibles.find { it.id == currentChat.id }

                    if (localFound != null) {
                        solicitudSeleccionada = localFound
                        activeChat = null
                    } else {
                        viewModel.obtenerSolicitudPorId(currentChat.id) { solicitudCargada ->
                            if (solicitudCargada != null) {
                                solicitudSeleccionada = solicitudCargada
                                activeChat = null
                            } else {
                                Toast.makeText(
                                    context,
                                    "No se pudo cargar la información de la solicitud.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } else null,
            viewModel = chatViewModel,
            onBack = { activeChat = null }
        )
    }

    else if (uiState.estadoVerificacion == "APROBADO") {

        // 🟢 FLUJO PRIMERA VEZ: REDIRECCIÓN OBLIGATORIA A PORTAFOLIO Y COBERTURA
        if (requiereOnboarding) {
            Scaffold(
                containerColor = Color(0xFFFAFAFA)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Surface(
                        color = Color(0xFFFFF7ED),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD8A8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "¡Cuenta Aprobada! 🎉",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF9A3412)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Antes de empezar a postularte a trabajos, configura tus zonas de atención, portafolio y descripción.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC2410C),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    ZonaCoberturaPortafolioContainer(
                        viewModel = viewModel,
                        onVolver = { }
                    )
                }
            }
        }

        // 🟢 NAVEGACIÓN NORMAL UNA VEZ CONFIGURADO SU PERFIL
        else if (solicitudSeleccionada != null) {
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
                            solicitudSeleccionada = null
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
                        2 -> ProviderHistoryScreen(
                            trabajosActivos = viewModel.listaMisTrabajosActivos,
                            historialReciente = viewModel.listaHistorialTrabajos,
                            estaCargando = viewModel.estaCargandoMisTrabajos,
                            onVerDetalleSolicitud = { idSolicitud ->
                                viewModel.obtenerSolicitudPorId(idSolicitud) { solicitudCargada ->
                                    if (solicitudCargada != null) {
                                        solicitudSeleccionada = solicitudCargada
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No se encontró la información del trabajo.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onAbrirChatCliente = { solicitudId, _ ->
                                activeChat = ChatItemUi(
                                    id = solicitudId,
                                    nombreCliente = "Cliente",
                                    tituloSolicitud = "Detalles del servicio",
                                    ultimoMensaje = "",
                                    hora = ""
                                )
                            }
                        )
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
    else {
        ProviderNonApprovedScreen(
            uiState = uiState,
            onIrACorregirDocumentos = onIrACorregirDocumentos,
            onLogout = onLogout
        )
    }
}