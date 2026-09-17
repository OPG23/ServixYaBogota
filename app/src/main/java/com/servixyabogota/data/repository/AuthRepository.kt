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

    suspend fun registerUser(user: User, password: String): Result<String> {
        return try {
            val creds = auth.createUserWithEmailAndPassword(user.email, password).await()
            val uid = creds.user?.uid ?: throw Exception("Error al obtener UID")
            val newUser = user.copy(uid = uid)

            // Guarda unificado en la colección "usuarios"
            db.collection(USERS_COLLECTION).document(uid).set(newUser).await()
            Result.success(newUser.rol)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val creds = auth.signInWithEmailAndPassword(email, password).await()
            val uid = creds.user?.uid ?: throw Exception("Error de autenticación")

            val doc = db.collection(USERS_COLLECTION).document(uid).get().await()
            val rol = doc.getString("rol") ?: "cliente"
            Result.success(rol)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}