package dev.brodt.taskmanager.fragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import dev.brodt.taskmanager.MainActivity
import dev.brodt.taskmanager.R
import java.util.Locale

/**
 * A fragment that provides settings for language and theme.
 */
class SettingsFragment : Fragment() {
    private lateinit var btnEnglish: Button
    private lateinit var btnPortuguese: Button
    private lateinit var btnSpanish: Button
    private lateinit var btnToggleTheme: ImageView
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        btnEnglish = view.findViewById(R.id.btn_english)
        btnPortuguese = view.findViewById(R.id.btn_portuguese)
        btnSpanish = view.findViewById(R.id.btn_spanish)
        btnToggleTheme = view.findViewById(R.id.btn_toggle_theme)
        
        setupListeners()
        updateThemeIcon()
        
        return view
    }
    
    private fun setupListeners() {
        btnEnglish.setOnClickListener {
            setLocale("en")
        }
        
        btnPortuguese.setOnClickListener {
            setLocale("pt")
        }
        
        btnSpanish.setOnClickListener {
            setLocale("es")
        }
        
        btnToggleTheme.setOnClickListener {
            toggleTheme()
        }
    }
    
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        val context = requireContext()
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        
        // Save the selected language
        val sharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("language", languageCode).apply()
        
        // Restart the activity to apply the language change
        restartApp()
    }
    
    private fun toggleTheme() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            // Currently in dark mode, switch to light mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            // Currently in light mode, switch to dark mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        
        // Save the selected theme
        val sharedPreferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("theme", AppCompatDelegate.getDefaultNightMode()).apply()
        
        // Update the theme icon
        updateThemeIcon()
        
        // Recreate the activity to apply the theme change immediately
        requireActivity().recreate()
    }
    
    private fun updateThemeIcon() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            // Dark mode is active
            btnToggleTheme.setImageResource(R.drawable.ic_light_mode)
        } else {
            // Light mode is active
            btnToggleTheme.setImageResource(R.drawable.ic_dark_mode)
        }
    }
    
    private fun restartApp() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        requireActivity().finish()
    }
    
    companion object {
        /**
         * Use this factory method to create a new instance of this fragment.
         *
         * @return A new instance of SettingsFragment.
         */
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
