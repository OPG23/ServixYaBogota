package com.servixyabogota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.servixyabogota.ui.admin.AdminUsersContainer
import com.servixyabogota.ui.admin.AdminVerificationScreen
import com.servixyabogota.ui.admin.AdminViewModel
import com.servixyabogota.ui.auth.AccountDisabledScreen
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
                var currentScreen by remember { mutableStateOf("cargando_sesion") }

                // Comprobación de sesión al iniciar la App
                LaunchedEffect(Unit) {
                    authViewModel.verificarSesionActiva { pantalla ->
                        currentScreen = pantalla
                    }
                }

                when (currentScreen) {
                    "cargando_sesion" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1976D2))
                        }
                    }

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

                            val estado = authViewModel.usuarioActual?.estadoVerificacion?.uppercase() ?: ""
                            val estaDeshabilitado = estado == "DESHABILITADO" || estado == "INHABILITADO" || estado == "DESACTIVADO"

                            if (estaDeshabilitado) {
                                currentScreen = "cuenta_deshabilitada"
                            } else {
                                currentScreen = when (rol.lowercase()) {
                                    "administrador", "admin" -> "home_admin"
                                    "prestador" -> "home_prestador"
                                    else -> "home_cliente"
                                }
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

                    "cuenta_deshabilitada" -> {
                        AccountDisabledScreen(
                            nombreUsuario = authViewModel.usuarioActual?.nombre ?: "Usuario",
                            onCerrarSesion = {
                                authViewModel.cerrarSesion()
                                currentScreen = "login"
                            }
                        )
                    }

                    "home_admin" -> {
                        LaunchedEffect(Unit) {
                            adminViewModel.cargarSolicitudes()
                        }
                        AdminVerificationScreen(
                            viewModel = adminViewModel,
                            onLogout = {
                                authViewModel.cerrarSesion()
                                currentScreen = "login"
                            },
                            onNavigateToUsuarios = { currentScreen = "admin_users" }
                        )
                    }

                    "admin_users" -> {
                        AdminUsersContainer(
                            viewModel = adminViewModel,
                            onNavigateToHome = { currentScreen = "home_admin" }
                        )
                    }

                    "home_prestador" -> {
                        LaunchedEffect(Unit) {
                            providerViewModel.cargarPerfil()
                        }
                        ProviderMainContainer(
                            viewModel = providerViewModel,
                            onIrACorregirDocumentos = {
                                currentScreen = "cargar_documentos"
                            },
                            onLogout = {
                                authViewModel.cerrarSesion()
                                currentScreen = "login"
                            }
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
                        onLogout = {
                            authViewModel.cerrarSesion()
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}