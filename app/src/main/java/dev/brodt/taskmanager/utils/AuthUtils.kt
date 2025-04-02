package dev.brodt.taskmanager.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import dev.brodt.taskmanager.LoginActivity

class AuthUtils {
    companion object {
        private const val TAG = "AuthUtils"
        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

        fun logout(context: Context){
            try {
                Log.d(TAG, "Iniciando processo de logout")
                
                val currentUser = firebaseAuth.currentUser
                Log.d(TAG, "Usuário atual: ${currentUser?.email ?: "Nenhum"}")
                
                firebaseAuth.signOut()
                Log.d(TAG, "Logout realizado com sucesso")
                
                Toast.makeText(context, "Até logo!!!", Toast.LENGTH_SHORT).show()
                Navigation.goToScreen(context, LoginActivity::class.java)
            } catch (error: Exception) {
                Log.e(TAG, "Erro ao realizar logout: ${error.message}", error)
                Toast.makeText(context, "Falha ao realizar logout: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        fun checkAuthState() {
            val currentUser = firebaseAuth.currentUser
            Log.d(TAG, "Estado de autenticação atual: ${if (currentUser != null) "Autenticado como ${currentUser.email}" else "Não autenticado"}")
            
            firebaseAuth.addAuthStateListener { auth ->
                val user = auth.currentUser
                Log.d(TAG, "Mudança no estado de autenticação: ${if (user != null) "Autenticado como ${user.email}" else "Não autenticado"}")
            }
        }
    }
}
