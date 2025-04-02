package dev.brodt.taskmanager.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import dev.brodt.taskmanager.R

/**
 * A reusable email input fragment that can be used throughout the application.
 * This fragment provides an email input field with validation.
 */
class EmailInputFragment : Fragment() {
    private lateinit var emailInput: EditText
    private lateinit var errorText: TextView
    private var onEmailValidationListener: OnEmailValidationListener? = null
    private var isEmailValid = false

    interface OnEmailValidationListener {
        fun onEmailValidationChanged(email: String, isValid: Boolean)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_email_input, container, false)
        
        emailInput = view.findViewById(R.id.email_input)
        errorText = view.findViewById(R.id.email_error_text)
        
        emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                validateEmail(s.toString())
            }
        })
        
        return view
    }

    /**
     * Validates the email format and updates the UI accordingly.
     *
     * @param email The email to validate.
     */
    private fun validateEmail(email: String) {
        if (email.isEmpty()) {
            errorText.text = getString(R.string.email_required)
            errorText.visibility = View.VISIBLE
            isEmailValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText.text = getString(R.string.invalid_email_format)
            errorText.visibility = View.VISIBLE
            isEmailValid = false
        } else {
            errorText.visibility = View.GONE
            isEmailValid = true
        }
        
        onEmailValidationListener?.onEmailValidationChanged(email, isEmailValid)
    }

    /**
     * Get the current email text.
     *
     * @return The current email text.
     */
    fun getEmail(): String {
        return if (::emailInput.isInitialized) emailInput.text.toString() else ""
    }

    /**
     * Set the email text.
     *
     * @param email The email text to set.
     */
    fun setEmail(email: String) {
        if (::emailInput.isInitialized) {
            emailInput.setText(email)
        }
    }

    /**
     * Check if the current email is valid.
     *
     * @return True if the email is valid, false otherwise.
     */
    fun isValid(): Boolean {
        return isEmailValid
    }

    /**
     * Set a listener to be notified when the email validation status changes.
     *
     * @param listener The listener to be notified.
     */
    fun setOnEmailValidationListener(listener: OnEmailValidationListener) {
        onEmailValidationListener = listener
    }
}
