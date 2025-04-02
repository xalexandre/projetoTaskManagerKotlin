package dev.brodt.taskmanager

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.brodt.taskmanager.fragments.EmailInputFragment
import dev.brodt.taskmanager.utils.CrashlyticsUtils
import dev.brodt.taskmanager.utils.Navigation

class LoginActivity : AppCompatActivity(), EmailInputFragment.OnEmailValidationListener {
    private val TAG = "LoginActivity"
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var emailFragment: EmailInputFragment
    private var isEmailValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Get reference to the email fragment
        emailFragment = supportFragmentManager.findFragmentById(R.id.email_fragment) as EmailInputFragment
        emailFragment.setOnEmailValidationListener(this)

        val btnLogin = findViewById<Button>(R.id.submitLoginBtn)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val registerLink = findViewById<TextView>(R.id.create_account)

        btnLogin.setOnClickListener {
            val email = emailFragment.getEmail()
            val password = findViewById<TextView>(R.id.passwordInput).text.toString()

            // Validar campos vazios
            if (!isEmailValid || password.isEmpty()) {
                Log.d(TAG, "Email inválido ou senha vazia")
                Toast.makeText(this, "Email válido e senha são obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Tentando fazer login com email: $email")
            Toast.makeText(this, "Tentando fazer login...", Toast.LENGTH_SHORT).show()
            
            login(email, password)
        }

        forgotPassword.setOnClickListener {
            Navigation.goToScreen(this, ForgotPasswordActivity::class.java)
        }

        registerLink.setOnClickListener {
            Navigation.goToScreen(this, RegisterActivity::class.java)
        }
    }

    // EmailInputFragment.OnEmailValidationListener implementation
    override fun onEmailValidationChanged(email: String, isValid: Boolean) {
        this.isEmailValid = isValid
        Log.d(TAG, "Email validation changed: $email, isValid: $isValid")
    }
    
    private fun login(email: String, password: String) {
        Log.d(TAG, "Iniciando processo de autenticação com Firebase")
        
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d(TAG, "Login bem-sucedido")
                val user = firebaseAuth.currentUser
                if (user != null) {
                    // Set user ID in Crashlytics
                    CrashlyticsUtils.setUserId()
                    
                    // Log login event
                    CrashlyticsUtils.log("User logged in: ${user.uid}")
                    
                    // Set session information
                    CrashlyticsUtils.setCustomKey("session_login_time", System.currentTimeMillis())
                    CrashlyticsUtils.setCustomKey("session_login_method", "email")
                    
                    Toast.makeText(this@LoginActivity, "Seja bem-vindo ${user.email}", Toast.LENGTH_SHORT).show()
                    Navigation.goToScreen(this@LoginActivity, MainActivity::class.java)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha no login: ${exception.message}", exception)
                
                // Record the exception in Crashlytics
                CrashlyticsUtils.setCustomKey("login_attempt_email", email)
                CrashlyticsUtils.setCustomKey("login_failure_time", System.currentTimeMillis())
                CrashlyticsUtils.recordException("Login failure for email: $email", exception)
                
                Toast.makeText(
                    this@LoginActivity, 
                    "Falha ao realizar login: ${exception.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener { task ->
                Log.d(TAG, "Processo de login completo. Sucesso: ${task.isSuccessful}")
            }
    }
}
