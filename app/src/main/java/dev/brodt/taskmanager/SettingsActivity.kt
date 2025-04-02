package dev.brodt.taskmanager

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import dev.brodt.taskmanager.fragments.SettingsFragment

/**
 * Activity for app settings (language and theme)
 */
class SettingsActivity : AppCompatActivity() {
    private val TAG = "SettingsActivity"
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        Log.d(TAG, "Iniciando SettingsActivity")
        
        setupUI()
        setupFragment()
    }
    
    private fun setupUI() {
        backButton = findViewById(R.id.back_button)
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun setupFragment() {
        // Initialize and add the settings fragment
        settingsFragment = SettingsFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, settingsFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }
}
