package com.servixyabogota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.servixyabogota.ui.admin.AdminUsersScreen
import com.servixyabogota.ui.admin.AdminVerificationScreen
import com.servixyabogota.ui.admin.AdminViewModel
import com.servixyabogota.ui.auth.AuthViewModel
import com.servixyabogota.ui.auth.LoginScreen
import com.servixyabogota.ui.auth.RegisterScreen
import com.servixyabogota.ui.client.ClientMainContainer
import com.servixyabogota.ui.onboarding.OnboardingScreen
import com.servixyabogota.ui.provider.CargarDocumentosPrestadorScreen
import com.servixyabogota.ui.provider.ProviderMainContainer
import com.servixyabogota.ui.provider.ProviderViewModel
import com.servixyabogota.ui.theme.ServixYaBogotaTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val adminViewModel: AdminViewModel by viewModels()
    private val providerViewModel: ProviderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ServixYaBogotaTheme {
                var currentScreen by remember { mutableStateOf("onboarding") }

                when (currentScreen) {
                    "onboarding" -> OnboardingScreen(
                        onFinishOnboarding = {
                            authViewModel.clearAuthResult()
                            currentScreen = "login"
                        }
                    )

                    "login" -> LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToRegister = {
                            authViewModel.clearAuthResult()
                            currentScreen = "register"
                        },
                        onLoginSuccess = { rol ->
                            authViewModel.clearAuthResult()
                            currentScreen = when (rol.lowercase()) {
                                "administrador", "admin" -> "home_admin"
                                "prestador" -> "home_prestador"
                                else -> "home_cliente"
                            }
                        }
                    )

                    "register" -> RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = {
                            authViewModel.clearAuthResult()
                            currentScreen = "login"
                        },
                        onBackToLogin = {
                            authViewModel.clearAuthResult()
                            currentScreen = "login"
                        }
                    )

                    "home_admin" -> {
                        LaunchedEffect(Unit) {
                            adminViewModel.cargarSolicitudes()
                        }
                        AdminVerificationScreen(
                            viewModel = adminViewModel,
                            onLogout = { currentScreen = "login" },
                            onNavigateToUsuarios = { currentScreen = "admin_users" }
                        )
                    }

                    // PANTALLA DE GESTIÓN DE USUARIOS (ADMIN)
                    "admin_users" -> {
                        AdminUsersScreen(
                            onNavigateToHome = { currentScreen = "home_admin" },
                            onVerDetalleUsuario = { _ ->
                                // Detalle de usuario cuando crees esa vista
                            }
                        )
                    }

                    "home_prestador" -> {
                        // Refresca el estado real del usuario cada vez que entra a esta pantalla
                        LaunchedEffect(Unit) {
                            providerViewModel.cargarPerfil()
                        }
                        ProviderMainContainer(
                            viewModel = providerViewModel,
                            onIrACorregirDocumentos = {
                                currentScreen = "cargar_documentos"
                            },
                            onLogout = { currentScreen = "login" }
                        )
                    }

                    "cargar_documentos" -> CargarDocumentosPrestadorScreen(
                        viewModel = providerViewModel,
                        onDocumentosEnviados = {
                            providerViewModel.cargarPerfil()
                            currentScreen = "home_prestador"
                        },
                        onVolver = { currentScreen = "home_prestador" }
                    )

                    "home_cliente" -> ClientMainContainer(
                        onLogout = { currentScreen = "login" }
                    )
                }
            }
        }
    }
}