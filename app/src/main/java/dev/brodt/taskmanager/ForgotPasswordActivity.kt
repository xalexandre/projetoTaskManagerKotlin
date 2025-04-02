package dev.brodt.taskmanager

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import dev.brodt.taskmanager.fragments.EmailInputFragment
import dev.brodt.taskmanager.utils.Navigation

class ForgotPasswordActivity : AppCompatActivity(), EmailInputFragment.OnEmailValidationListener {
    private val TAG = "ForgotPasswordActivity"
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var emailFragment: EmailInputFragment
    private var isEmailValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Get reference to the email fragment
        emailFragment = supportFragmentManager.findFragmentById(R.id.email_fragment) as EmailInputFragment
        emailFragment.setOnEmailValidationListener(this)

        val forgotPasswordBtn = findViewById<Button>(R.id.forgotPasswordBtn)
        val login = findViewById<TextView>(R.id.login)

        forgotPasswordBtn.setOnClickListener {
            val email = emailFragment.getEmail()
            
            // Validar email
            if (!isEmailValid) {
                Log.d(TAG, "Email inválido")
                Toast.makeText(this, "Email válido é obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            Log.d(TAG, "Tentando enviar email de recuperação para: $email")
            Toast.makeText(this, "Enviando email de recuperação...", Toast.LENGTH_SHORT).show()
            
            recoveryPassword(email)
        }

        login.setOnClickListener {
            Navigation.goToScreen(this, LoginActivity::class.java)
        }
    }

    // EmailInputFragment.OnEmailValidationListener implementation
    override fun onEmailValidationChanged(email: String, isValid: Boolean) {
        this.isEmailValid = isValid
        Log.d(TAG, "Email validation changed: $email, isValid: $isValid")
    }
    
    private fun recoveryPassword(email: String) {
        Log.d(TAG, "Iniciando processo de recuperação de senha com Firebase")
        
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Log.d(TAG, "Email de recuperação enviado com sucesso")
                Toast.makeText(this@ForgotPasswordActivity, "Link enviado com sucesso!!!", Toast.LENGTH_SHORT).show()
                Navigation.goToScreen(this@ForgotPasswordActivity, LoginActivity::class.java)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao enviar email de recuperação: ${exception.message}", exception)
                Toast.makeText(
                    this@ForgotPasswordActivity, 
                    "Falha ao enviar link de reset de senha: ${exception.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener { task ->
                Log.d(TAG, "Processo de recuperação de senha completo. Sucesso: ${task.isSuccessful}")
            }
    }
}
