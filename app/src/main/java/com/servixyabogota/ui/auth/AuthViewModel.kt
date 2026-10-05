package com.servixyabogota.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servixyabogota.data.model.User
import com.servixyabogota.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _authResult = MutableLiveData<Result<String>?>()
    val authResult: LiveData<Result<String>?> = _authResult

    // 1. VARIABLE PARA GUARDAR EL USUARIO ACTUAL
    var usuarioActual by mutableStateOf<User?>(null)
        private set

    fun clearAuthResult() {
        _authResult.value = null
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authResult.value = Result.failure(Exception("Por favor ingresa tu correo y contraseña."))
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val result = repository.loginUser(email.trim(), pass.trim())
                result.onSuccess { user ->
                    usuarioActual = user // Se asigna el usuario ANTES de navegar
                    _authResult.value = Result.success(user.rol)
                }.onFailure { e ->
                    _authResult.value = Result.failure(e)
                }
            } catch (e: Exception) {
                _authResult.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // Método opcional para obtener los datos del usuario logueado
    private fun obtenerDatosUsuario() {
        viewModelScope.launch {
            try {
                // Si tu AuthRepository tiene una función para obtener el perfil del usuario actual,
                // asignalo aquí. Ejemplo:
                // usuarioActual = repository.getUsuarioActual()
            } catch (_: Exception) { }
        }
    }

    fun registrarUsuario(
        email: String,
        pass: String,
        nombre: String,
        apellido: String,
        cedula: String,
        tel: String,
        rol: String,
        habeasData: Boolean
    ) {
        if (email.isBlank() || pass.isBlank() || nombre.isBlank() || apellido.isBlank() || cedula.isBlank() || tel.isBlank()) {
            _authResult.value = Result.failure(Exception("Por favor completa todos los campos."))
            return
        }

        if (!habeasData) {
            _authResult.value = Result.failure(Exception("Debe aceptar el tratamiento de datos."))
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val estadoVerif = if (rol == "prestador") "NO_ENVIADO" else "N/A"

                val user = User(
                    email = email.trim(),
                    nombre = nombre.trim(),
                    apellido = apellido.trim(),
                    cedula = cedula.trim(),
                    telefono = tel.trim(),
                    rol = rol,
                    estadoVerificacion = estadoVerif
                )

                val result = repository.registerUser(user, pass.trim())
                if (result.isSuccess) {
                    usuarioActual = user
                }
                _authResult.value = result
            } catch (e: Exception) {
                _authResult.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // 2. FUNCIÓN PARA CERRAR SESIÓN
    fun cerrarSesion() {
        try {
            // Si tu AuthRepository tiene un método para desloguear (ej: Firebase.auth.signOut()), llámalo aquí
            // repository.logout()
        } catch (_: Exception) { }

        usuarioActual = null
        clearAuthResult()
    }
}