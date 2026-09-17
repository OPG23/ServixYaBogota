package com.servixyabogota.ui.provider

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object ProviderRoutes {
    const val HOME = "provider_home"
    const val UPLOAD_DOCS = "provider_upload_docs"
    const val PORTFOLIO = "provider_portfolio"
}

@Composable
fun ProviderNavGraph(
    navController: NavHostController = rememberNavController(),
    providerViewModel: ProviderViewModel = viewModel(),
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ProviderRoutes.HOME
    ) {
        // Pantalla Principal: Administra estados APROBADO, PENDIENTE, RECHAZADO y NO_ENVIADO
        composable(ProviderRoutes.HOME) {
            ProviderHomeScreen(
                viewModel = providerViewModel,
                onIrACorregirDocumentos = {
                    navController.navigate(ProviderRoutes.UPLOAD_DOCS)
                },
                onLogout = onLogout
            )
        }

        // Subida y corrección parcial de documentos de identidad
        composable(ProviderRoutes.UPLOAD_DOCS) {
            CargarDocumentosPrestadorScreen(
                viewModel = providerViewModel,
                onDocumentosEnviados = {
                    navController.popBackStack()
                },
                onVolver = {
                    navController.popBackStack()
                }
            )
        }

        // Configuración de zona de cobertura y evidencias de portafolio (Prestadores APROBADOS)
        composable(ProviderRoutes.PORTFOLIO) {
            ZonaCoberturaPortafolioContainer(
                viewModel = providerViewModel,
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }

}

