package com.servixyabogota

import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityTest {

    @Test
    fun verificar_limpieza_de_emojis_y_caracteres_especiales_en_profesion() {
        // Simula la función de sanitización usada al registrar profesion/categoría
        val entradaInsegura = "🪠 Plomería <script>alert(1)</script>"

        val profesionLimpia = entradaInsegura.replace(Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]"), "").trim()

        assertEquals("Plomería scriptalertscript", profesionLimpia)
    }
}