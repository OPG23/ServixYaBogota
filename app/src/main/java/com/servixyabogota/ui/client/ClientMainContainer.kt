package com.servixyabogota.ui.client

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ClientMainContainer(
    onLogout: () -> Unit = {}
) {
    var currentTab by rememberSaveable { mutableStateOf("Inicio") }
    var isCreatingRequest by rememberSaveable { mutableStateOf(false) }

    // Estado para navegar a sub-pantallas de Ajustes
    var currentSettingsSubScreen by rememberSaveable { mutableStateOf<String?>(null) }

    // Estado para la sub-pantalla de Propuestas Recibidas
    var selectedProposalRequestId by rememberSaveable { mutableStateOf<String?>(null) }

    if (isCreatingRequest) {
        CreateRequestScreen(
            onBack = { isCreatingRequest = false },
            onPublicarSolicitud = { categoria, detalle, localidad, direccion, urgencia, archivos ->
                // Aquí pasas estos 6 parámetros al ViewModel o backend
                isCreatingRequest = false
            }
        )
    } else {
        when (currentTab) {
            "Inicio" -> {
                ClientHomeScreen(
                    onLogout = onLogout,
                    onNavigateTab = { selectedTab ->
                        currentSettingsSubScreen = null
                        selectedProposalRequestId = null
                        currentTab = selectedTab
                    },
                    onIniciarChat = { }
                )
            }
            "Solicitudes" -> {
                ClientRequestsScreen(
                    onNavigateTab = { selectedTab ->
                        currentSettingsSubScreen = null
                        selectedProposalRequestId = null
                        currentTab = selectedTab
                    },
                    onNuevaSolicitudClick = { isCreatingRequest = true },
                    onEditarClick = { },
                    onCancelarClick = { },
                    onVerChatClick = { }
                )
            }
            "Propuestas" -> {
                if (selectedProposalRequestId != null) {
                    // Pantalla de Propuestas Recibidas (#1024)
                    ClientReceivedProposalsScreen(
                        solicitudId = selectedProposalRequestId!!,
                        onBack = { selectedProposalRequestId = null },
                        onOpenChat = { prestadorNombre ->
                            /* Abrir Chat con el prestador */
                        }
                    )
                } else {
                    // Pantalla Principal de Propuestas
                    ClientProposalsScreen(
                        onNavigateTab = { selectedTab ->
                            currentSettingsSubScreen = null
                            selectedProposalRequestId = null
                            currentTab = selectedTab
                        },
                        onVerPropuestasClick = { id ->
                            selectedProposalRequestId = id
                        },
                        onVerChatClick = { id ->
                            /* Navegar directamente a chat si está en proceso */
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
                            onLogout = onLogout // <--- Pasa el evento de logout recibido
                        )
                    }
                }
            }
        }
    }
}