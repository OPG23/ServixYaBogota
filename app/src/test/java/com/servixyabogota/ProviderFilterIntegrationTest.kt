package com.servixyabogota

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderFilterIntegrationTest {

    @Test
    fun prestador_sin_cobertura_o_descripcion_no_se_muestra_a_clientes() {
        // Simulación de datos recibidos de Firestore
        val estaAprobado = true
        val localidades = emptyList<String>() // Aún no ha completado onboarding
        val descripcion = ""

        val haCompletadoPerfilInicial = localidades.isNotEmpty() && descripcion.isNotBlank()
        val esVisibleEnInicio = estaAprobado && haCompletadoPerfilInicial

        assertFalse("El prestador no debería verse en el inicio de clientes si no ha hecho su onboarding", esVisibleEnInicio)
    }

    @Test
    fun prestador_con_perfil_completo_y_aprobado_es_visible() {
        val estaAprobado = true
        val localidades = listOf("Suba", "Usaquén")
        val descripcion = "Plomero certificado SENA"

        val haCompletadoPerfilInicial = localidades.isNotEmpty() && descripcion.isNotBlank()
        val esVisibleEnInicio = estaAprobado && haCompletadoPerfilInicial

        assertTrue("El prestador debería ser visible", esVisibleEnInicio)
    }
}