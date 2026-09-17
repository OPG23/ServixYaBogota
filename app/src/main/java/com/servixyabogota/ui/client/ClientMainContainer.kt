package com.servixyabogota.ui.client

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ClientMainContainer(
    viewModel: ClientViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
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
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    viewModel.publicarSolicitud(
                        clienteId = currentUser.uid,
                        clienteNombre = currentUser.displayName.orEmpty().ifBlank { "Cliente" },
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
                } else {
                    Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                }
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
                    viewModel = viewModel,
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
                    // Pantalla de Propuestas Recibidas
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
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}