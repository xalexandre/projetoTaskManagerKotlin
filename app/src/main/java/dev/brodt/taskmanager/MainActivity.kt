package dev.brodt.taskmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.brodt.taskmanager.base.BaseActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.brodt.taskmanager.BuildConfig
import dev.brodt.taskmanager.fragments.SettingsFragment
import dev.brodt.taskmanager.fragments.TaskListFragment
import dev.brodt.taskmanager.fragments.WeatherFragment
import dev.brodt.taskmanager.utils.AuthUtils
import dev.brodt.taskmanager.utils.CrashlyticsUtils
import dev.brodt.taskmanager.utils.Navigation
import dev.brodt.taskmanager.utils.PerformanceUtils

// Modelo de dados para Task
data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = ""
)

class MainActivity : BaseActivity(), TaskListFragment.OnTaskClickListener {
    private val TAG = "MainActivity"
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var firebaseUser: FirebaseUser? = null
    
    private lateinit var taskListFragment: TaskListFragment
    private lateinit var weatherFragment: WeatherFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        // Start performance trace for onCreate
        val trace = PerformanceUtils.startTrace("main_activity_onCreate")
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.d(TAG, "Iniciando MainActivity")
        
        // Log event to Firebase Analytics
        val app = application as TaskManagerApplication
        app.logEvent("main_activity_opened")
        
        // Verificar e registrar o estado de autenticação
        AuthUtils.checkAuthState()
        
        // Verificar autenticação primeiro
        firebaseUser = firebaseAuth.currentUser
        Log.d(TAG, "Estado de autenticação: ${if (firebaseUser != null) "Autenticado como ${firebaseUser?.email}" else "Não autenticado"}")
        
        if (firebaseUser == null) {
            // Usuário não autenticado, redirecionar para login
            Log.d(TAG, "Usuário não autenticado, redirecionando para tela de login")
            Navigation.goToScreen(this, LoginActivity::class.java)
            finish() // Importante: encerrar a atividade para evitar retorno
            PerformanceUtils.stopTrace("main_activity_onCreate")
            return
        }

        // Initialize MobileAds
        MobileAds.initialize(this) { initializationStatus ->
            Log.d(TAG, "MobileAds initialization status: $initializationStatus")
        }
        
        // Request notification permission
        requestNotificationPermission()
        
        setupUI()
        setupFragments()
        setupAds()
        
        // Start the task reminder service
        val serviceIntent = Intent(this, dev.brodt.taskmanager.notifications.TaskReminderService::class.java)
        startService(serviceIntent)
        
        // Stop performance trace for onCreate
        PerformanceUtils.stopTrace("main_activity_onCreate")
    }
    
    private fun setupUI() {
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fab_add_task)
        val logoutBtn = findViewById<ImageView>(R.id.logout)
        val profileBtn = findViewById<ImageView>(R.id.profile)
        val settingsBtn = findViewById<ImageView>(R.id.settings)
        val notificationTestBtn = findViewById<ImageView>(R.id.notification_test)
        
        fabAddTask.setOnClickListener {
            Navigation.goToScreen(this, TaskActivity::class.java)
        }

        logoutBtn.setOnClickListener {
            AuthUtils.logout(this)
        }

        profileBtn.setOnClickListener {
            Navigation.goToScreen(this, ProfileActivity::class.java)
        }
        
        settingsBtn.setOnClickListener {
            Navigation.goToScreen(this, SettingsActivity::class.java)
        }
        
        notificationTestBtn.setOnClickListener {
            // Show a toast indicating that we're sending a test notification
            Toast.makeText(this, "Enviando notificação de teste...", Toast.LENGTH_SHORT).show()
            
            // Show a local notification using FCMService
            val notificationTitle = "Notificação de Teste"
            val notificationMessage = "Esta é uma notificação de teste do aplicativo TaskManager"
            
            // Create and show the notification directly
            val fcmService = dev.brodt.taskmanager.notifications.FCMService()
            fcmService.showNotification(
                context = this,
                title = notificationTitle,
                message = notificationMessage,
                channelId = dev.brodt.taskmanager.notifications.FCMService.CHANNEL_GENERAL,
                data = emptyMap()
            )
            
            // Log the event
            Log.d(TAG, "Test notification sent")
            val app = application as TaskManagerApplication
            app.logEvent("test_notification_sent")
        }
    }
    
    private fun setupFragments() {
        // Use the trace helper method to measure fragment setup time
        PerformanceUtils.trace("main_activity_setupFragments") {
            // Initialize and add the weather fragment
            weatherFragment = WeatherFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_weather, weatherFragment)
                .commit()

            // Initialize and add the task list fragment
            taskListFragment = TaskListFragment()
            taskListFragment.setOnTaskClickListener(this)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_task_list, taskListFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Recarregando dados")
        // Refresh task list when activity returns to foreground
        if (::taskListFragment.isInitialized) {
            taskListFragment.refresh()
        }
    }

    // TaskListFragment.OnTaskClickListener implementation
    override fun onTaskClick(taskId: String) {
        val activity = Intent(this, TaskActivity::class.java)
        activity.putExtra("taskId", taskId)
        Navigation.goToScreen(this, activity)
    }

    override fun onTaskLongClick(taskId: String) {
        // Default implementation is in the fragment
    }
    
    private fun setupAds() {
        // Start a trace for ad setup
        val adTrace = PerformanceUtils.startTrace("main_activity_setupAds")
        
        try {
            val adView = findViewById<AdView>(R.id.adView)
            
            // Make sure the AdView is visible
            adView.visibility = android.view.View.VISIBLE
            
            // Create an ad request
            val adRequest = AdRequest.Builder().build()
            
            // Set ad listener to handle events
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Ad loaded successfully")
                    // Make sure the AdView is visible after loading
                    adView.visibility = android.view.View.VISIBLE
                    
                    // Stop the trace when the ad is loaded
                    PerformanceUtils.stopTrace("main_activity_setupAds")
                    
                    // Add metrics about the ad loading
                    PerformanceUtils.putMetric("main_activity_setupAds", "adLoaded", 1)
                    
                    // Show a toast for debugging
                    Toast.makeText(this@MainActivity, "Ad loaded successfully", Toast.LENGTH_SHORT).show()
                }
                
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${adError.message}")
                    // Log to Crashlytics for monitoring
                    CrashlyticsUtils.setCustomKey("ad_error_code", adError.code)
                    CrashlyticsUtils.setCustomKey("ad_error_domain", adError.domain)
                    CrashlyticsUtils.log("Ad failed to load: ${adError.message}")
                    
                    // Stop the trace when the ad fails to load
                    PerformanceUtils.stopTrace("main_activity_setupAds")
                    
                    // Add metrics about the ad failure
                    PerformanceUtils.putMetric("main_activity_setupAds", "adFailed", 1)
                    
                    // Show a toast for debugging
                    Toast.makeText(this@MainActivity, "Ad failed to load: ${adError.message}", Toast.LENGTH_SHORT).show()
                }
                
                override fun onAdOpened() {
                    Log.d(TAG, "Ad opened")
                    // Log event to Analytics
                    val app = application as TaskManagerApplication
                    app.logEvent("ad_opened")
                }
                
                override fun onAdClicked() {
                    Log.d(TAG, "Ad clicked")
                    // Log event to Analytics
                    val app = application as TaskManagerApplication
                    app.logEvent("ad_clicked")
                }
                
                override fun onAdClosed() {
                    Log.d(TAG, "Ad closed")
                }
                
                override fun onAdImpression() {
                    Log.d(TAG, "Ad impression recorded")
                }
            }
            
            // Load the ad
            adView.loadAd(adRequest)
            Log.d(TAG, "Ad request sent")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ads: ${e.message}", e)
            CrashlyticsUtils.recordException("Error setting up ads", e)
            
            // Stop the trace in case of error
            PerformanceUtils.stopTrace("main_activity_setupAds")
            
            // Show a toast for debugging
            Toast.makeText(this, "Error setting up ads: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Add options menu for debug features
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Only add debug options in debug builds
        if (BuildConfig.DEBUG) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }
        return true
    }
    
    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
            Log.d(TAG, "Requesting notification permission")
        } else {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Notification permission already granted")
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Notification permission granted by user")
                } else {
                    // Permission denied
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Notification permission denied by user")
                }
                return
            }
        }
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_test_crash -> {
                // Show a toast before crashing
                Toast.makeText(this, "Testing Crashlytics with a forced crash", Toast.LENGTH_SHORT).show()
                
                // Add some custom keys to help with debugging
                CrashlyticsUtils.setCustomKey("crash_test_timestamp", System.currentTimeMillis())
                CrashlyticsUtils.setCustomKey("crash_test_user_id", firebaseUser?.uid ?: "unknown")
                
                // Log the test crash
                CrashlyticsUtils.log("Manual test crash triggered by user")
                
                // Force a crash after a short delay to allow the toast to show
                android.os.Handler().postDelayed({
                    CrashlyticsUtils.forceCrash()
                }, 1000)
                true
            }
            R.id.action_test_notification -> {
                // Show a toast indicating that we're sending a test notification
                Toast.makeText(this, "Sending test notification...", Toast.LENGTH_SHORT).show()
                
                // Show a local notification using FCMService
                val notificationTitle = "Test Notification"
                val notificationMessage = "This is a test notification from TaskManager app"
                
                // Create and show the notification directly
                val fcmService = dev.brodt.taskmanager.notifications.FCMService()
                fcmService.showNotification(
                    context = this,
                    title = notificationTitle,
                    message = notificationMessage,
                    channelId = dev.brodt.taskmanager.notifications.FCMService.CHANNEL_GENERAL,
                    data = emptyMap()
                )
                
                // Log the event
                Log.d(TAG, "Test notification sent")
                val app = application as TaskManagerApplication
                app.logEvent("test_notification_sent")
                
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
