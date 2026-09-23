package com.servixyabogota.ui.client

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.servixyabogota.ui.chat.ChatDetailScreen
import com.servixyabogota.ui.chat.ChatViewModel

// Modelo simple para manejar el estado del chat en el Cliente
data class ChatClienteUi(
    val id: String,
    val nombrePrestador: String,
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
        ChatDetailScreen(
            chatId = chatClienteActivo!!.id,
            currentUserId = currentUserId,
            esCliente = true,
            interlocutorNombre = chatClienteActivo!!.nombrePrestador,
            solicitudInfo = chatClienteActivo!!.tituloSolicitud,
            actionButtonText = if (chatClienteActivo!!.tituloSolicitud != null) "Confirmar Servicio" else null,
            onActionButtonClick = {
                Toast.makeText(context, "Procesando contratación del servicio...", Toast.LENGTH_SHORT).show()
            },
            viewModel = chatViewModel,
            onBack = { chatClienteActivo = null }
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
    // 3. VISTA DE CHATS DIRECTOS
    else if (showDirectChats) {
        ClientDirectChatsScreen(
            onBack = { showDirectChats = false },
            onOpenChat = { chatId, providerName ->
                chatClienteActivo = ChatClienteUi(
                    id = chatId,
                    nombrePrestador = providerName,
                    tituloSolicitud = null
                )
            }
        )
    }
    // 4. VISTA DE DETALLE DEL PERFIL DEL PRESTADOR
    else if (selectedProviderId != null && selectedProvider != null) {
        ProviderDetailProfileScreen(
            prestador = selectedProvider,
            onBack = { selectedProviderId = null },
            onIniciarChat = {
                chatClienteActivo = ChatClienteUi(
                    id = "chat_${selectedProvider.id}",
                    nombrePrestador = selectedProvider.nombre,
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
                        chatClienteActivo = ChatClienteUi(
                            id = "chat_${prestador.id}",
                            nombrePrestador = prestador.nombre,
                            tituloSolicitud = null
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