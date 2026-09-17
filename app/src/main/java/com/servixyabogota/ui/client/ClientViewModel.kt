package com.servixyabogota.ui.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ClientViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Estados del perfil
    var nombre by mutableStateOf("")
    var telefono by mutableStateOf("")
    var correo by mutableStateOf("")
    var ciudad by mutableStateOf("Bogotá, D.C.")
    var fotoUrl by mutableStateOf("")
    var tipoCliente by mutableStateOf("Cliente Residencial")

    // Estados de UI
    var estaCargando by mutableStateOf(false)
    var estaGuardando by mutableStateOf(false)

    init {
        cargarPerfilCliente()
    }

    /**
     * Carga la información del usuario concatenando Nombre y Apellido si existen
     */
    fun cargarPerfilCliente() {
        val user = auth.currentUser ?: return
        estaCargando = true

        db.collection("usuarios").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nombreCompletoDoc = doc.getString("nombreCompleto")
                    val primerNombre = doc.getString("nombre") ?: doc.getString("primerNombre") ?: ""
                    val apellido = doc.getString("apellido") ?: doc.getString("apellidos") ?: ""
                    val nameAttr = doc.getString("name") ?: ""

                    // Obtener el nombre completo
                    nombre = when {
                        !nombreCompletoDoc.isNullOrBlank() -> nombreCompletoDoc
                        primerNombre.isNotBlank() && apellido.isNotBlank() -> "$primerNombre $apellido"
                        primerNombre.isNotBlank() -> primerNombre
                        nameAttr.isNotBlank() -> nameAttr
                        else -> user.displayName ?: ""
                    }

                    telefono = doc.getString("telefono") ?: doc.getString("phone") ?: ""
                    correo = doc.getString("correo") ?: doc.getString("email") ?: user.email ?: ""
                    ciudad = doc.getString("ciudad") ?: doc.getString("city") ?: "Bogotá, D.C."
                    fotoUrl = doc.getString("fotoUrl") ?: doc.getString("photoUrl") ?: ""
                    tipoCliente = doc.getString("tipoCliente") ?: "Cliente Residencial"
                } else {
                    correo = user.email ?: ""
                    nombre = user.displayName ?: ""
                }
                estaCargando = false
            }
            .addOnFailureListener {
                estaCargando = false
            }
    }

    /**
     * Guarda el nombre completo e incrementa la compatibilidad con campos nombre / apellido
     */
    fun guardarCambiosPerfil(
        nuevoNombre: String,
        nuevoTelefono: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("No hay una sesión activa de usuario.")
            return
        }

        estaGuardando = true

        val partesNombre = nuevoNombre.trim().split("\\s+".toRegex())
        val nombrePropio = partesNombre.firstOrNull() ?: ""
        val apellidoPropio = if (partesNombre.size > 1) partesNombre.drop(1).joinToString(" ") else ""

        val updates = mapOf(
            "nombre" to nuevoNombre,
            "nombreCompleto" to nuevoNombre,
            "primerNombre" to nombrePropio,
            "apellido" to apellidoPropio,
            "telefono" to nuevoTelefono,
            "ultimaActualizacion" to Timestamp.now()
        )

        db.collection("usuarios").document(user.uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                nombre = nuevoNombre
                telefono = nuevoTelefono
                estaGuardando = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                estaGuardando = false
                onError(e.localizedMessage ?: "Error al actualizar la información.")
            }
    }

    /**
     * Cerrar sesión en Firebase Auth
     */
    fun cerrarSesion(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }

    fun cambiarContrasena(
        contrasenaActual: String,
        nuevaContrasena: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrEmpty()) {
            onError("No hay una sesión activa de usuario.")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, contrasenaActual)

        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(nuevaContrasena).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        db.collection("usuarios").document(user.uid)
                            .update("ultimaActualizacionPassword", Timestamp.now())
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onSuccess() }
                    } else {
                        onError(updateTask.exception?.localizedMessage ?: "Error al actualizar la contraseña.")
                    }
                }
            } else {
                onError("La contraseña actual es incorrecta.")
            }
        }
    }

    fun actualizarPreferenciasNotificaciones(
        notificacionesPushChat: Boolean,
        alertasCorreo: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser ?: return

        val updates = mapOf(
            "notificacionesPushChat" to notificacionesPushChat,
            "alertasCorreo" to alertasCorreo
        )

        db.collection("usuarios").document(user.uid)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Error al actualizar preferencias")
            }
    }
}