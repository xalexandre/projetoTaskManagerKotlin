package dev.brodt.taskmanager

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import dev.brodt.taskmanager.fragments.EmailInputFragment
import dev.brodt.taskmanager.fragments.PasswordDifficult
import dev.brodt.taskmanager.utils.Navigation
import dev.brodt.taskmanager.utils.Password

class RegisterActivity : AppCompatActivity(), EmailInputFragment.OnEmailValidationListener {
    private val TAG = "RegisterActivity"
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var emailFragment: EmailInputFragment
    private var isEmailValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        lateinit var text: Editable;

        // Get reference to the email fragment
        emailFragment = supportFragmentManager.findFragmentById(R.id.email_fragment) as EmailInputFragment
        emailFragment.setOnEmailValidationListener(this)

        val btnRegister = findViewById<Button>(R.id.submitRegisterBtn)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val LoginLink = findViewById<TextView>(R.id.login)
        val confirmPassword = findViewById<EditText>(R.id.passwordConfirmInput)
        val password = supportFragmentManager.findFragmentById(R.id.passwordInput) as PasswordDifficult

        btnRegister.setOnClickListener {
            val email = emailFragment.getEmail()
            val passText = password.passwordInput.text.toString()
            val confirmPasswordText = confirmPassword.text.toString()

            // Validar email
            if (!isEmailValid) {
                Log.d(TAG, "Email inválido")
                Toast.makeText(this, "Email válido é obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Tentando registrar com email: $email")
            Toast.makeText(this, "Tentando registrar usuário...", Toast.LENGTH_SHORT).show()
            
            register(email, passText, confirmPasswordText)
        }

        password.passwordInput.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    text = s;
                }
                val result = Password.verifyPasswordDificult(text.toString());
                if (result < 3) {
                    btnRegister.isEnabled = false;
                } else {
                    btnRegister.isEnabled = true;
                }
            }

        })


        forgotPassword.setOnClickListener {
            Navigation.goToScreen(this, ForgotPasswordActivity::class.java)
        }

        LoginLink.setOnClickListener {
            Navigation.goToScreen(this, LoginActivity::class.java)
        }
    }

    // EmailInputFragment.OnEmailValidationListener implementation
    override fun onEmailValidationChanged(email: String, isValid: Boolean) {
        this.isEmailValid = isValid
        Log.d(TAG, "Email validation changed: $email, isValid: $isValid")
    }
    
    private fun register(email: String, password: String, confirmPassword: String) {
        Log.d(TAG, "Verificando senhas")
        
        if (password != confirmPassword){
            Log.d(TAG, "Senhas não conferem")
            Toast.makeText(
                baseContext,
                "As senhas não são iguais",
                Toast.LENGTH_SHORT,
            ).show()
        } else if (password == "" || confirmPassword == ""){
            Log.d(TAG, "Senhas vazias")
            Toast.makeText(
                baseContext,
                "As senhas não podem ser vazias",
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            Log.d(TAG, "Iniciando processo de registro com Firebase")
            
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    Log.d(TAG, "Registro bem-sucedido")
                    Toast.makeText(
                        baseContext,
                        "Usuário cadastrado com sucesso!!!",
                        Toast.LENGTH_SHORT,
                    ).show()
                    Navigation.goToScreen(this, MainActivity::class.java)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Falha no registro: ${exception.message}", exception)
                    Toast.makeText(
                        baseContext,
                        "Falha ao registrar usuário: ${exception.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .addOnCompleteListener { task ->
                    Log.d(TAG, "Processo de registro completo. Sucesso: ${task.isSuccessful}")
                }
        }
    }
}
