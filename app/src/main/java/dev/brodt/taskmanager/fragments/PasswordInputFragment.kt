package dev.brodt.taskmanager.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import dev.brodt.taskmanager.R
import dev.brodt.taskmanager.utils.Password

/**
 * A reusable password input fragment that can be used throughout the application.
 * This fragment provides a password input field with strength validation and show/hide functionality.
 */
class PasswordInputFragment : Fragment() {
    private lateinit var passwordInput: EditText
    private lateinit var passwordStrengthText: TextView
    private lateinit var togglePasswordVisibility: ImageView
    private var onPasswordValidationListener: OnPasswordValidationListener? = null
    private var isPasswordValid = false
    private var isPasswordVisible = false
    private var minPasswordStrength = 3 // Default minimum strength (normal)

    interface OnPasswordValidationListener {
        fun onPasswordValidationChanged(password: String, isValid: Boolean, strength: Int)
    }

    companion object {
        private const val ARG_HINT = "hint"
        private const val ARG_MIN_STRENGTH = "min_strength"

        /**
         * Use this factory method to create a new instance of this fragment
         * with the provided parameters.
         *
         * @param hint Hint text for the password field.
         * @param minStrength Minimum password strength required (1-5).
         * @return A new instance of PasswordInputFragment.
         */
        fun newInstance(hint: String, minStrength: Int = 3): PasswordInputFragment {
            val fragment = PasswordInputFragment()
            val args = Bundle()
            args.putString(ARG_HINT, hint)
            args.putInt(ARG_MIN_STRENGTH, minStrength)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            minPasswordStrength = it.getInt(ARG_MIN_STRENGTH, 3)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_password_input, container, false)
        
        passwordInput = view.findViewById(R.id.password_input)
        passwordStrengthText = view.findViewById(R.id.password_strength_text)
        togglePasswordVisibility = view.findViewById(R.id.toggle_password_visibility)
        
        // Set hint if provided
        arguments?.getString(ARG_HINT)?.let {
            passwordInput.hint = it
        }
        
        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })
        
        togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }
        
        return view
    }

    /**
     * Validates the password strength and updates the UI accordingly.
     *
     * @param password The password to validate.
     */
    private fun validatePassword(password: String) {
        val strength = Password.verifyPasswordDificult(password)
        
        when (strength) {
            0 -> {
                passwordStrengthText.text = getString(R.string.password_empty)
                passwordStrengthText.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            1 -> {
                passwordStrengthText.text = getString(R.string.password_very_weak)
                passwordStrengthText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            }
            2 -> {
                passwordStrengthText.text = getString(R.string.password_weak)
                passwordStrengthText.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            3 -> {
                passwordStrengthText.text = getString(R.string.password_normal)
                passwordStrengthText.setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
            }
            4 -> {
                passwordStrengthText.text = getString(R.string.password_strong)
                passwordStrengthText.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
            }
            5 -> {
                passwordStrengthText.text = getString(R.string.password_very_strong)
                passwordStrengthText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
            else -> {
                passwordStrengthText.text = getString(R.string.password_very_weak)
                passwordStrengthText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            }
        }
        
        isPasswordValid = strength >= minPasswordStrength
        onPasswordValidationListener?.onPasswordValidationChanged(password, isPasswordValid, strength)
    }

    /**
     * Toggles the visibility of the password text.
     */
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        
        if (isPasswordVisible) {
            // Show password
            passwordInput.transformationMethod = null
            togglePasswordVisibility.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            // Hide password
            passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            togglePasswordVisibility.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
        
        // Move cursor to the end
        passwordInput.setSelection(passwordInput.text.length)
    }

    /**
     * Get the current password text.
     *
     * @return The current password text.
     */
    fun getPassword(): String {
        return if (::passwordInput.isInitialized) passwordInput.text.toString() else ""
    }

    /**
     * Set the password text.
     *
     * @param password The password text to set.
     */
    fun setPassword(password: String) {
        if (::passwordInput.isInitialized) {
            passwordInput.setText(password)
        }
    }

    /**
     * Check if the current password is valid (meets the minimum strength requirement).
     *
     * @return True if the password is valid, false otherwise.
     */
    fun isValid(): Boolean {
        return isPasswordValid
    }

    /**
     * Set a listener to be notified when the password validation status changes.
     *
     * @param listener The listener to be notified.
     */
    fun setOnPasswordValidationListener(listener: OnPasswordValidationListener) {
        onPasswordValidationListener = listener
    }

    /**
     * Set the minimum password strength required for validation.
     *
     * @param strength The minimum strength (1-5).
     */
    fun setMinPasswordStrength(strength: Int) {
        minPasswordStrength = strength.coerceIn(1, 5)
        // Re-validate current password with new strength requirement
        if (::passwordInput.isInitialized) {
            validatePassword(passwordInput.text.toString())
        }
    }
}
