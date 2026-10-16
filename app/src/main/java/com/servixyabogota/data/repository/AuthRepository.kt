package com.servixyabogota.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.servixyabogota.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val USERS_COLLECTION = "usuarios"
    }

    /**
     * Retorna el UID del usuario autenticado en el dispositivo (si existe).
     */
    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Obtiene el perfil del usuario actualmente autenticado desde Firestore.
     */
    suspend fun getUsuarioActual(): Result<User> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No hay sesión activa")
            val doc = db.collection(USERS_COLLECTION).document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: throw Exception("Usuario no encontrado")
            val userConUid = user.copy(uid = uid)
            Result.success(userConUid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión en Firebase Authentication.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Consulta si la cédula ya existe registrada bajo un mismo rol.
     */
    suspend fun existeCedulaConRol(cedula: String, rol: String): Boolean {
        return try {
            val snapshot = db.collection(USERS_COLLECTION)
                .whereEqualTo("cedula", cedula)
                .whereEqualTo("rol", rol)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerUser(user: User, password: String): Result<String> {
        return try {
            val existe = existeCedulaConRol(user.cedula, user.rol)
            if (existe) {
                val rolTexto = if (user.rol == "prestador") "Prestador" else "Cliente"
                return Result.failure(
                    Exception("La cédula ${user.cedula} ya está registrada para una cuenta de $rolTexto.")
                )
            }

            val creds = auth.createUserWithEmailAndPassword(user.email, password).await()
            val uid = creds.user?.uid ?: throw Exception("Error al obtener UID")
            val newUser = user.copy(uid = uid)

            db.collection(USERS_COLLECTION).document(uid).set(newUser).await()
            Result.success(newUser.rol)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val creds = auth.signInWithEmailAndPassword(email, password).await()
            val uid = creds.user?.uid ?: throw Exception("Error de autenticación")

            val doc = db.collection(USERS_COLLECTION).document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: throw Exception("Usuario no encontrado")
            val userConUid = user.copy(uid = uid)

            Result.success(userConUid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}