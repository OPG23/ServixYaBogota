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

    // Estado para ver el perfil detallado de un prestador y sus reseñas
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var showProviderReviews by rememberSaveable { mutableStateOf(false) }
    val selectedProvider = viewModel.listaPrestadores.find { it.id == selectedProviderId }

    // ESTADOS DE CHATS
    var showDirectChats by rememberSaveable { mutableStateOf(false) }
    var chatClienteActivo by remember { mutableStateOf<ChatClienteUi?>(null) }

    // Sub-pantallas
    var currentSettingsSubScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProposalRequestId by rememberSaveable { mutableStateOf<String?>(null) }

    // 1. PANTALLA DE CHAT ACTIVO (Navegación al detalle del chat)
    if (chatClienteActivo != null) {
        val activeChat = chatClienteActivo!!

        ChatDetailScreen(
            chatId = activeChat.id,
            currentUserId = currentUserId,
            esCliente = true,
            interlocutorNombre = activeChat.nombrePrestador,
            interlocutorFotoUrl = activeChat.fotoPrestadorUrl,
            solicitudInfo = activeChat.tituloSolicitud,
            onVerSolicitudClick = if (activeChat.tituloSolicitud != null) {
                {
                    Toast.makeText(context, "Mostrando detalle de: ${activeChat.tituloSolicitud}", Toast.LENGTH_SHORT).show()
                }
            } else null,
            viewModel = chatViewModel,
            onBack = { chatClienteActivo = null },
            onVerPerfilPrestador = { idPrestadorEmitido ->
                val idTarget = idPrestadorEmitido.ifBlank { activeChat.prestadorId ?: "" }

                val prestadorEncontrado = viewModel.listaPrestadores.find { prestador ->
                    prestador.id == idTarget ||
                            (activeChat.prestadorId != null && prestador.id == activeChat.prestadorId) ||
                            prestador.nombre.equals(activeChat.nombrePrestador.trim(), ignoreCase = true)
                }

                if (prestadorEncontrado != null) {
                    selectedProviderId = prestadorEncontrado.id
                    showProviderReviews = false
                    showDirectChats = false
                    chatClienteActivo = null
                } else if (idTarget.isNotBlank()) {
                    selectedProviderId = idTarget
                    showProviderReviews = false
                    showDirectChats = false
                    chatClienteActivo = null
                } else {
                    Toast.makeText(
                        context,
                        "No se encontró la información del prestador",
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
    // 3. VISTA DE RESEÑAS PÚBLICAS DEL PRESTADOR
    else if (selectedProvider != null && showProviderReviews) {
        ProviderPublicReviewsScreen(
            prestador = selectedProvider,
            viewModel = viewModel,
            onBack = { showProviderReviews = false }
        )
    }
    // 4. VISTA DE DETALLE DEL PERFIL DEL PRESTADOR
    else if (selectedProvider != null) {
        val provider = selectedProvider

        ProviderDetailProfileScreen(
            prestador = provider,
            onBack = {
                selectedProviderId = null
                showProviderReviews = false
            },
            onIniciarChat = {
                viewModel.obtenerOCrearChatDirecto(
                    prestadorId = provider.id,
                    onSuccess = { chatIdReal ->
                        selectedProviderId = null
                        showProviderReviews = false
                        chatClienteActivo = ChatClienteUi(
                            id = chatIdReal,
                            nombrePrestador = provider.nombre,
                            fotoPrestadorUrl = provider.fotoUrl,
                            prestadorId = provider.id,
                            tituloSolicitud = null
                        )
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onVerResenas = {
                showProviderReviews = true
            }
        )
    }
    // 5. VISTA DE CHATS DIRECTOS
    else if (showDirectChats) {
        ClientDirectChatsScreen(
            onBack = { showDirectChats = false },
            onOpenChat = { chatId, providerName, providerId ->
                val prestadorMatch = viewModel.listaPrestadores.find {
                    it.id == providerId || it.nombre.equals(providerName.trim(), ignoreCase = true)
                }
                chatClienteActivo = ChatClienteUi(
                    id = chatId,
                    nombrePrestador = providerName,
                    fotoPrestadorUrl = prestadorMatch?.fotoUrl,
                    prestadorId = providerId.ifBlank { prestadorMatch?.id },
                    tituloSolicitud = null
                )
            }
        )
    }
    // 6. NAVEGACIÓN PRINCIPAL DE TABS
    else {
        when (currentTab) {
            "Inicio" -> {
                ClientHomeScreen(
                    viewModel = viewModel,
                    onNavigateTab = { selectedTab ->
                        currentSettingsSubScreen = null
                        selectedProposalRequestId = null
                        showDirectChats = false
                        showProviderReviews = false
                        currentTab = selectedTab
                    },
                    onIniciarChat = { prestador ->
                        viewModel.obtenerOCrearChatDirecto(
                            prestadorId = prestador.id,
                            onSuccess = { chatIdReal ->
                                chatClienteActivo = ChatClienteUi(
                                    id = chatIdReal,
                                    nombrePrestador = prestador.nombre,
                                    fotoPrestadorUrl = prestador.fotoUrl,
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
                        showProviderReviews = false
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
                        showProviderReviews = false
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
                        onOpenChat = { solicitudId, prestadorId, nombrePrestador ->
                            val prestadorMatch = viewModel.listaPrestadores.find { it.id == prestadorId }
                            chatClienteActivo = ChatClienteUi(
                                id = solicitudId,
                                nombrePrestador = nombrePrestador,
                                fotoPrestadorUrl = prestadorMatch?.fotoUrl,
                                prestadorId = prestadorId,
                                tituloSolicitud = "Propuesta de Servicio"
                            )
                        }
                    )
                } else {
                    ClientProposalsScreen(
                        clientViewModel = viewModel,
                        onNavigateTab = { selectedTab ->
                            currentSettingsSubScreen = null
                            selectedProposalRequestId = null
                            showDirectChats = false
                            showProviderReviews = false
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