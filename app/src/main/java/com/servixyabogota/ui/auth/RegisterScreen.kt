package com.servixyabogota.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servixyabogota.ui.components.TerminosYCondicionesDialog

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var esPrestador by remember { mutableStateOf(false) }
    var habeasData by remember { mutableStateOf(false) }

    var aceptoTerminos by remember { mutableStateOf(false) }
    var mostrarTerminosDialog by remember { mutableStateOf(false) }

    val isLoading by viewModel.loading.observeAsState(initial = false)
    val authResult by viewModel.authResult.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(authResult) {
        authResult?.let { result ->
            result.onSuccess {
                Toast.makeText(context, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                onRegisterSuccess()
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "Error en el registro", Toast.LENGTH_LONG).show()
            }
            viewModel.clearAuthResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Encabezado
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1976D2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ServixYaBogota",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1976D2)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "Servicios del hogar en Bogotá", fontSize = 13.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // Nombres
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Nombre", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = { Text("Ej. Juan Manuel", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Apellidos
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Apellidos", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                placeholder = { Text("Ej. Pérez Rodríguez", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cédula
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Número de Cédula (C.C.)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = cedula,
                onValueChange = { cedula = it },
                placeholder = { Text("Ej. 1018293847", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Correo
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Correo electrónico", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("juan.perez@email.com", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Teléfono
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Teléfono (+57)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                placeholder = { Text("300 123 4567", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contraseña
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Contraseña", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••••••", color = Color.LightGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selección de Rol
        Text(
            text = "¿Cómo usarás la aplicación?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (!esPrestador) 2.dp else 1.dp,
                        color = if (!esPrestador) Color(0xFF1976D2) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(if (!esPrestador) Color(0xFFF0F7FF) else Color.White)
                    .clickable { esPrestador = false }
                    .padding(12.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = if (!esPrestador) Color(0xFF1976D2) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (!esPrestador) Color(0xFF1976D2) else Color.Transparent)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Busco un servicio", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (!esPrestador) Color(0xFF1976D2) else Color.Black)
                    Text("(Cliente)", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (esPrestador) 2.dp else 1.dp,
                        color = if (esPrestador) Color(0xFF1976D2) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(if (esPrestador) Color(0xFFF0F7FF) else Color.White)
                    .clickable { esPrestador = true }
                    .padding(12.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = if (esPrestador) Color(0xFF1976D2) else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (esPrestador) Color(0xFF1976D2) else Color.Transparent)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ofrezco mis servicios", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (esPrestador) Color(0xFF1976D2) else Color.Black)
                    Text("(Prestador)", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Habeas Data
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = aceptoTerminos,
                onCheckedChange = { aceptoTerminos = it }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Acepto los Términos y Condiciones y Tratamiento de Datos (Ley 1581)",
                fontSize = 12.sp,
                color = Color(0xFF1976D2),
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier.clickable { mostrarTerminosDialog = true }
            )
        }

        if (mostrarTerminosDialog) {
            TerminosYCondicionesDialog(
                onDismiss = { mostrarTerminosDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF1976D2))
        } else {
            Button(
                enabled = aceptoTerminos, // <-- CORREGIDO: ahora lee aceptoTerminos
                onClick = {
                    val rol = if (esPrestador) "prestador" else "cliente"
                    viewModel.registrarUsuario(
                        email = email,
                        pass = password,
                        nombre = nombre,
                        apellido = apellido,
                        cedula = cedula,
                        tel = telefono,
                        rol = rol,
                        habeasData = aceptoTerminos // <-- CORREGIDO: pasa aceptoTerminos
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Crear Cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text(text = "¿Ya tienes cuenta? Inicia sesión", color = Color.Gray, fontSize = 13.sp)
        }
    }
}