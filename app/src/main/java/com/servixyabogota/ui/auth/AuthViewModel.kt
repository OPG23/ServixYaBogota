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

    var usuarioActual by mutableStateOf<User?>(null)
        private set

    fun clearAuthResult() {
        _authResult.value = null
    }

    /**
     * Verifica si existe una sesión guardada en el dispositivo al iniciar la app.
     */
    fun verificarSesionActiva(onFinished: (String) -> Unit) {
        val currentUid = repository.getCurrentUid()
        if (currentUid.isNullOrBlank()) {
            onFinished("onboarding")
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getUsuarioActual()
                result.onSuccess { user ->
                    usuarioActual = user
                    val estado = user.estadoVerificacion.uppercase()
                    val estaDeshabilitado = estado == "DESHABILITADO" || estado == "INHABILITADO" || estado == "DESACTIVADO"

                    val pantallaDestino = if (estaDeshabilitado) {
                        "cuenta_deshabilitada"
                    } else {
                        when (user.rol.lowercase()) {
                            "administrador", "admin" -> "home_admin"
                            "prestador" -> "home_prestador"
                            else -> "home_cliente"
                        }
                    }
                    onFinished(pantallaDestino)
                }.onFailure {
                    // Si falla la lectura del perfil, cerramos sesión por seguridad
                    repository.logout()
                    usuarioActual = null
                    onFinished("login")
                }
            } catch (e: Exception) {
                repository.logout()
                usuarioActual = null
                onFinished("login")
            } finally {
                _loading.value = false
            }
        }
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
                    usuarioActual = user
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

    /**
     * Cierra la sesión activa en Firebase y limpia el estado local.
     */
    fun cerrarSesion() {
        try {
            repository.logout()
        } catch (_: Exception) { }

        usuarioActual = null
        clearAuthResult()
    }

    // Estado para la recuperación de contraseña
    private val _resetPasswordResult = MutableLiveData<Result<String>?>()
    val resetPasswordResult: LiveData<Result<String>?> = _resetPasswordResult

    fun clearResetPasswordResult() {
        _resetPasswordResult.value = null
    }

    fun enviarCorreoRecuperacion(email: String) {
        if (email.isBlank()) {
            _resetPasswordResult.value = Result.failure(Exception("Por favor ingresa tu correo electrónico."))
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val result = repository.sendPasswordResetEmail(email.trim())
                result.onSuccess {
                    _resetPasswordResult.value = Result.success("Hemos enviado un enlace de recuperación a tu correo.")
                }.onFailure { e ->
                    _resetPasswordResult.value = Result.failure(e)
                }
            } catch (e: Exception) {
                _resetPasswordResult.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }
}