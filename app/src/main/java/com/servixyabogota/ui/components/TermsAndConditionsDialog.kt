package com.servixyabogota.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TerminosYCondicionesDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "TÉRMINOS Y CONDICIONES Y POLÍTICA DE TRATAMIENTO DE DATOS PERSONALES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "SERVIXYA BOGOTÁ S.A.S. | NIT: 901.876.543-2\nVersión 1.0 (15 de Septiembre de 2026)",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Divider()

                // Contenido desplazable del documento legal
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    ArticuloItem(
                        titulo = "Artículo 1. Objeto y Marco Legal",
                        contenido = "El presente documento regula el acceso, uso y funcionamiento de la plataforma móvil ServixYaBogota, la cual opera como una solución tecnológica de intermediación entre Clientes y Prestadores en Bogotá D.C. Cumple estrictamente con la Ley 1581 de 2012, el Decreto 1377 de 2013, la Ley 1266 de 2008 y la Ley 1480 de 2011 (Estatuto del Consumidor)."
                    )
                    ArticuloItem(
                        titulo = "Artículo 2. Aceptación Expresa y Evidencia Digital",
                        contenido = "El registro o uso de la aplicación implica la aceptación previa, expresa e informada de estos Términos y de la Política de Tratamiento de Datos. La aceptación se recaba mediante casilla de verificación. El sistema almacena evidencia técnica auditable que incluye ID de usuario, fecha, hora exacta, dirección IP y versión del documento."
                    )
                    ArticuloItem(
                        titulo = "Artículo 3. Definiciones Operativas y Roles",
                        contenido = "• Cliente: Usuario que solicita servicios técnicos.\n• Prestador: Técnico u oficiante que adjunta documentación para verificación.\n• Prestador Verificado: Usuario validado manualmente tras revisar su cédula y antecedentes.\n• Plataforma Intermediaria: ServixYaBogota facilita el contacto, pero no constituye relación laboral."
                    )
                    ArticuloItem(
                        titulo = "Artículo 4. Alcance del Servicio y Funcionalidades",
                        contenido = "La plataforma habilita la creación de solicitudes por categoría, tablero de oportunidades, interacción mediante chat 1:1 en tiempo real para cotización y módulo de calificación mediante estrellas y reseñas."
                    )
                    ArticuloItem(
                        titulo = "Artículo 5. Verificación de Identidad",
                        contenido = "Para activar el perfil público, los Prestadores deben aportar información veraz:\n1. Documento de Identidad por ambas caras.\n2. Selfie de validación biométrica con documento en mano.\n3. Certificado de Antecedentes Judiciales de la Policía Nacional (< 30 días).\n4. Soportes de idoneidad técnica (SENA, CONTE o equivalentes)."
                    )
                    ArticuloItem(
                        titulo = "Artículo 6. Licencia de Uso y Propiedad Intelectual",
                        contenido = "El código fuente, interfaces, logotipos y arquitecturas son propiedad exclusiva de SERVIXYA BOGOTÁ S.A.S. El Usuario recibe una licencia limitada, revocable, no exclusiva e intransferible."
                    )
                    ArticuloItem(
                        titulo = "Artículo 7. Limitación de Responsabilidad",
                        contenido = "ServixYaBogota actúa exclusivamente como canal de intermediación tecnológica. La ejecución material del servicio, precios, puntualidad y garantías son responsabilidad directa entre Cliente y Prestador."
                    )
                    ArticuloItem(
                        titulo = "Artículo 8. Categorías de Datos Recabados",
                        contenido = "• Datos de Identificación: Nombre, cédula, correo, teléfono y dirección en Bogotá.\n• Documentación: Cédula, antecedentes judicial, diplomas y fotografía.\n• Datos Transaccionales: Historial de solicitudes, chats, calificaciones e IP."
                    )
                    ArticuloItem(
                        titulo = "Artículo 9. Finalidades del Tratamiento de Datos",
                        contenido = "1. Validar identidad y antecedentes de Prestadores.\n2. Conectar solicitudes con Prestadores de la categoría correspondiente.\n3. Garantizar comunicación segura por chat interno.\n4. Calcular reputación pública mediante puntuaciones.\n5. Cumplir obligaciones legales o requerimientos de la SIC."
                    )
                    ArticuloItem(
                        titulo = "Artículo 10. Derechos ARCO (Consultas y Reclamos)",
                        contenido = "Los Titulares pueden consultar, actualizar, rectificar o suprimir sus datos escribiendo a dpo@servixyabogota.co. Las consultas se atenderán en máximo 10 días hábiles y reclamos en máximo 15 días hábiles."
                    )
                    ArticuloItem(
                        titulo = "Artículo 11. Encargados y Transferencia Internacional",
                        contenido = "Se utiliza la infraestructura serverless de Google Firebase como Encargado del Tratamiento. Las transferencias cuentan con cifrado HTTPS/TLS y AES-256."
                    )
                    ArticuloItem(
                        titulo = "Artículo 12. Conservación de Datos",
                        contenido = "Los datos de identificación se conservarán durante la vigencia de la cuenta + 2 años. Los documentos de verificación se conservarán durante la vigencia del perfil + 3 años."
                    )
                    ArticuloItem(
                        titulo = "Artículo 13. Medidas de Seguridad",
                        contenido = "Se aplican controles de acceso por roles (RBAC) y reglas de seguridad en Firestore. Ante incidentes de seguridad se activará el protocolo de contención (SLA 72 horas) y reporte a la SIC."
                    )
                    ArticuloItem(
                        titulo = "Artículo 14. Modificaciones y Ley Aplicable",
                        contenido = "Cualquier cambio se notificará con 15 días hábiles de anticipación. Este contrato se rige por la legislación de la República de Colombia y la jurisdicción de Bogotá D.C."
                    )
                }

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido y Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ArticuloItem(titulo: String, contenido: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = titulo,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = contenido,
            fontSize = 12.sp,
            color = Color(0xFF374151),
            lineHeight = 16.sp
        )
    }
}