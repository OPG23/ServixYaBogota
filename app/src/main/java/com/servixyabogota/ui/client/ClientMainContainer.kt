package com.servixyabogota.ui.client

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.servixyabogota.ui.chat.ChatDetailScreen
import com.servixyabogota.ui.chat.ChatViewModel

data class ChatClienteUi(
    val id: String,
    val nombrePrestador: String,
    val fotoPrestadorUrl: String? = null,
    val prestadorId: String? = null,
    val tituloSolicitud: String? = null
)

@Composable
fun ClientMainContainer(
    viewModel: ClientViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var currentTab by rememberSaveable { mutableStateOf("Inicio") }
    var isCreatingRequest by rememberSaveable { mutableStateOf(false) }

    // Estado para ver el perfil detallado de un prestador
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedProvider = viewModel.listaPrestadores.find { it.id == selectedProviderId }

    // ESTADOS DE CHATS
    var showDirectChats by rememberSaveable { mutableStateOf(false) }
    var chatClienteActivo by remember { mutableStateOf<ChatClienteUi?>(null) }

    // Sub-pantallas
    var currentSettingsSubScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProposalRequestId by rememberSaveable { mutableStateOf<String?>(null) }

    // 1. PANTALLA DE CHAT ACTIVO
    if (chatClienteActivo != null) {
        val activeChat = chatClienteActivo!!

        val pEncontrado = viewModel.listaPrestadores.find { prestador ->
            (activeChat.prestadorId != null && prestador.id == activeChat.prestadorId) ||
                    prestador.nombre.equals(activeChat.nombrePrestador.trim(), ignoreCase = true) ||
                    prestador.nombre.contains(activeChat.nombrePrestador.trim(), ignoreCase = true)
        }

        ChatDetailScreen(
            chatId = activeChat.id,
            currentUserId = currentUserId,
            esCliente = true,
            interlocutorNombre = activeChat.nombrePrestador,
            interlocutorFotoUrl = activeChat.fotoPrestadorUrl ?: pEncontrado?.fotoUrl, // <-- SE USA fotoUrl
            solicitudInfo = activeChat.tituloSolicitud,
            actionButtonText = if (activeChat.tituloSolicitud != null) "Confirmar Servicio" else null,
            onActionButtonClick = {
                Toast.makeText(context, "Procesando contratación del servicio...", Toast.LENGTH_SHORT).show()
            },
            viewModel = chatViewModel,
            onBack = { chatClienteActivo = null },
            onVerPerfilPrestador = {
                if (pEncontrado != null) {
                    selectedProviderId = pEncontrado.id
                    showDirectChats = false
                    chatClienteActivo = null
                } else {
                    Toast.makeText(
                        context,
                        "No se encontró la información de ${activeChat.nombrePrestador}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
    // 2. VISTA DE CREAR SOLICITUD
    else if (isCreatingRequest) {
        CreateRequestScreen(
            onBack = { isCreatingRequest = false },
            onPublicarSolicitud = { categoria, detalle, localidad, direccion, urgencia, archivos ->
                viewModel.publicarSolicitud(
                    categoria = categoria,
                    detalle = detalle,
                    urgencia = urgencia,
                    direccion = direccion,
                    localidad = localidad,
                    urisArchivos = archivos,
                    context = context,
                    onSuccess = {
                        Toast.makeText(context, "¡Solicitud publicada con éxito!", Toast.LENGTH_SHORT).show()
                        isCreatingRequest = false
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
    // 3. VISTA DE DETALLE DEL PERFIL DEL PRESTADOR
    else if (selectedProvider != null) {
        val provider = selectedProvider // Variable local para garantizar no-nulo dentro del lambda

        ProviderDetailProfileScreen(
            prestador = provider,
            onBack = { selectedProviderId = null },
            onIniciarChat = {
                viewModel.obtenerOCrearChatDirecto(
                    prestadorId = provider.id,
                    onSuccess = { chatIdReal ->
                        selectedProviderId = null
                        chatClienteActivo = ChatClienteUi(
                            id = chatIdReal,
                            nombrePrestador = provider.nombre,
                            fotoPrestadorUrl = provider.fotoUrl, // <-- SE USA fotoUrl
                            prestadorId = provider.id,
                            tituloSolicitud = null
                        )
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    // 4. VISTA DE CHATS DIRECTOS
    else if (showDirectChats) {
        ClientDirectChatsScreen(
            onBack = { showDirectChats = false },
            onOpenChat = { chatId, providerName ->
                val prestadorMatch = viewModel.listaPrestadores.find {
                    it.nombre.equals(providerName.trim(), ignoreCase = true)
                }
                chatClienteActivo = ChatClienteUi(
                    id = chatId,
                    nombrePrestador = providerName,
                    fotoPrestadorUrl = prestadorMatch?.fotoUrl, // <-- SE USA fotoUrl
                    prestadorId = prestadorMatch?.id,
                    tituloSolicitud = null
                )
            }
        )
    }
    // 5. NAVEGACIÓN PRINCIPAL DE TABS
    else {
        when (currentTab) {
            "Inicio" -> {
                ClientHomeScreen(
                    viewModel = viewModel,
                    onNavigateTab = { selectedTab ->
                        currentSettingsSubScreen = null
                        selectedProposalRequestId = null
                        showDirectChats = false
                        currentTab = selectedTab
                    },
                    onIniciarChat = { prestador ->
                        viewModel.obtenerOCrearChatDirecto(
                            prestadorId = prestador.id,
                            onSuccess = { chatIdReal ->
                                chatClienteActivo = ChatClienteUi(
                                    id = chatIdReal,
                                    nombrePrestador = prestador.nombre,
                                    fotoPrestadorUrl = prestador.fotoUrl, // <-- SE USA fotoUrl
                                    prestadorId = prestador.id,
                                    tituloSolicitud = null
                                )
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onVerPerfilPrestador = { prestador ->
                        selectedProviderId = prestador.id
                    }
                )
            }
            "Solicitudes" -> {
                ClientRequestsScreen(
                    viewModel = viewModel,
                    onNavigateTab = { selectedTab ->
                        currentSettingsSubScreen = null
                        selectedProposalRequestId = null
                        showDirectChats = false
                        currentTab = selectedTab
                    },
                    onNuevaSolicitudClick = { isCreatingRequest = true },
                    onEditarClick = { _ -> },
                    onCancelarClick = { _ -> },
                    onVerChatClick = { solicitud ->
                        chatClienteActivo = ChatClienteUi(
                            id = solicitud.id,
                            nombrePrestador = "Prestador Asignado",
                            tituloSolicitud = solicitud.categoria
                        )
                    }
                )
            }
            "Propuestas", "Mensajes" -> {
                if (selectedProposalRequestId != null) {
                    ClientReceivedProposalsScreen(
                        solicitudId = selectedProposalRequestId!!,
                        onBack = { selectedProposalRequestId = null },
                        onOpenChat = { _ ->
                            chatClienteActivo = ChatClienteUi(
                                id = selectedProposalRequestId!!,
                                nombrePrestador = "Prestador",
                                tituloSolicitud = "Propuesta de Servicio"
                            )
                        }
                    )
                } else {
                    ClientProposalsScreen(
                        onNavigateTab = { selectedTab ->
                            currentSettingsSubScreen = null
                            selectedProposalRequestId = null
                            showDirectChats = false
                            currentTab = selectedTab
                        },
                        onVerPropuestasClick = { id ->
                            selectedProposalRequestId = id
                        },
                        onVerChatClick = { id ->
                            chatClienteActivo = ChatClienteUi(
                                id = id,
                                nombrePrestador = "Prestador",
                                tituloSolicitud = "Solicitud Activa"
                            )
                        },
                        onVerChatsDirectosClick = {
                            showDirectChats = true
                        }
                    )
                }
            }
            "Ajustes" -> {
                when (currentSettingsSubScreen) {
                    "security" -> {
                        ClientSecuritySettingsScreen(
                            onBack = { currentSettingsSubScreen = null }
                        )
                    }
                    "reviews" -> {
                        ClientReviewsScreen(
                            onBack = { currentSettingsSubScreen = null }
                        )
                    }
                    else -> {
                        ClientProfileScreen(
                            onBack = { currentTab = "Inicio" },
                            onNavigateToSecurity = { currentSettingsSubScreen = "security" },
                            onNavigateToReviews = { currentSettingsSubScreen = "reviews" },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}