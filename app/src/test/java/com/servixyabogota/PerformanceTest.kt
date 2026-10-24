package com.servixyabogota

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceTest {

    @Test
    fun medir_tiempo_filtrado_lista_prestadores() {
        // Simula una lista grande de 5,000 prestadores
        val listaPrestadoresMock = List(5000) { id ->
            mapOf(
                "id" to "prestador_$id",
                "aprobado" to (id % 2 == 0),
                "localidades" to if (id % 2 == 0) listOf("Suba") else emptyList(),
                "descripcion" to "Descripción $id"
            )
        }

        val tiempoEjecucion = measureTimeMillis {
            val filtrados = listaPrestadoresMock.filter {
                (it["aprobado"] as Boolean) &&
                        (it["localidades"] as List<*>).isNotEmpty() &&
                        (it["descripcion"] as String).isNotBlank()
            }
        }

        // La prueba pasa si el filtrado toma menos de 100 milisegundos
        assertTrue("El filtrado fue muy lento: ${tiempoEjecucion}ms", tiempoEjecucion < 100)
    }
}