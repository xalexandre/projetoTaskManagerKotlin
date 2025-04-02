package dev.brodt.taskmanager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import dev.brodt.taskmanager.BuildConfig
import dev.brodt.taskmanager.notifications.MyFirebaseMessagingService
import dev.brodt.taskmanager.utils.CrashlyticsUtils
import java.util.Arrays
import java.util.Locale

class TaskManagerApplication : Application() {
    private val TAG = "TaskManagerApp"
    
    companion object {
        private var instance: TaskManagerApplication? = null
        
        fun getInstance(): TaskManagerApplication {
            return instance!!
        }
        
        fun isOnline(): Boolean {
            return instance?.isDeviceOnline() ?: false
        }
    }

    // Firebase Analytics instance
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Load saved settings
        loadSettings()
        
        // Inicializar Firebase
        try {
            Log.d(TAG, "Inicializando Firebase")
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase inicializado com sucesso")
            
            // Initialize Firebase App Check
            try {
                val firebaseAppCheck = FirebaseAppCheck.getInstance()
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "Firebase App Check initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase App Check: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
            
            // Initialize Firebase Analytics
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            Log.d(TAG, "Firebase Analytics inicializado com sucesso")
            
            // Initialize Firebase Crashlytics with enhanced configuration
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            
            // Set app information in Crashlytics
            crashlytics.setCustomKey("app_version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("app_version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("device_language", Locale.getDefault().language)
            crashlytics.setCustomKey("device_country", Locale.getDefault().country)
            
            // Set device information
            crashlytics.setCustomKey("android_version", Build.VERSION.SDK_INT)
            crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("device_model", Build.MODEL)
            
            Log.d(TAG, "Firebase Crashlytics inicializado com sucesso")
            
            // Initialize AdMob
            try {
                // Set up test device IDs for development
                val testDeviceIds = Arrays.asList("ABCDEF012345678901234567890ABCDEF")
                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(configuration)
                
                // Initialize the Mobile Ads SDK
                MobileAds.initialize(this) { initializationStatus ->
                    Log.d(TAG, "AdMob inicializado com sucesso: $initializationStatus")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao inicializar AdMob: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
            
            // Configurar persistência offline
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                Log.d(TAG, "Firebase Persistence habilitada com sucesso")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao habilitar Firebase Persistence: ${e.message}", e)
            }
            
            // Initialize Firebase Cloud Messaging
            try {
                // Create notification channel for Android O and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = getString(R.string.notification_channel_general)
                    val channel = NotificationChannel(
                        channelId,
                        getString(R.string.notification_channel_general),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)
                    Log.d(TAG, "Notification channel created")
                }
                
                // Check if Google Play Services are available
                if (isGooglePlayServicesAvailable() && isDeviceOnline()) {
                    Log.d(TAG, "Google Play Services available, initializing FCM")
                    
                    // Subscribe to topics with error handling
                    try {
                        FirebaseMessaging.getInstance().subscribeToTopic("general")
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "Subscribed to 'general' topic")
                                } else {
                                    Log.e(TAG, "Failed to subscribe to 'general' topic", task.exception)
                                    // Don't show error to user as this is not critical
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Exception when subscribing to topic: ${e.message}", e)
                                // Continue app execution, this is not a critical error
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error subscribing to FCM topic: ${e.message}", e)
                    }
                    
                    // Get FCM token with better error handling
                    try {
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    Log.d(TAG, "FCM Token: $token")
                                    
                                    // Log token in Crashlytics for debugging
                                    CrashlyticsUtils.setCustomKey("fcm_token", token)
                                } else {
                                    Log.e(TAG, "Failed to get FCM token", task.exception)
                                    // This is not critical for app functionality
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Exception when getting FCM token: ${e.message}", e)
                                // Continue app execution, this is not critical for core functionality
                            }
                    } catch (e: Exception) {
                        // Catch any unexpected exceptions that might occur
                        Log.e(TAG, "Unexpected error getting FCM token: ${e.message}", e)
                        // Continue app execution, FCM is not critical for core functionality
                    }
                } else {
                    if (!isGooglePlayServicesAvailable()) {
                        Log.w(TAG, "Google Play Services not available, skipping FCM initialization")
                    } else {
                        Log.w(TAG, "Device is offline, skipping FCM token retrieval and topic subscription")
                    }
                }
                
                Log.d(TAG, "Firebase Cloud Messaging initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase Cloud Messaging: ${e.message}", e)
                CrashlyticsUtils.recordException("Error initializing FCM", e)
                // Continue app execution, FCM is not critical for core functionality
            }
            
            // Verificar conectividade com Firebase
            if (isDeviceOnline()) {
                checkFirebaseConnection()
            } else {
                Log.w(TAG, "Dispositivo offline. Não é possível verificar conexão com Firebase.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar Firebase: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            Toast.makeText(this, "Erro ao inicializar Firebase: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Verifica se o dispositivo está conectado à internet
    fun isDeviceOnline(): Boolean {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Para Android 10 (API 29) e superior
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val network = connectivityManager.activeNetwork ?: return true // Assume online se não conseguir verificar
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return true
                
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } 
            // Para versões mais antigas do Android
            else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar conectividade: ${e.message}", e)
            return true // Em caso de erro, assume que está online para não bloquear funcionalidades
        }
    }
    
    /**
     * Load saved settings for language and theme
     */
    private fun loadSettings() {
        try {
            val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            
            // Load theme setting
            val savedTheme = sharedPreferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppCompatDelegate.setDefaultNightMode(savedTheme)
            Log.d(TAG, "Theme loaded: $savedTheme")
            
            // Load language setting
            val savedLanguage = sharedPreferences.getString("language", null)
            if (savedLanguage != null) {
                val locale = java.util.Locale(savedLanguage)
                java.util.Locale.setDefault(locale)
                
                val config = Configuration(resources.configuration)
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
                Log.d(TAG, "Language loaded: $savedLanguage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings: ${e.message}", e)
        }
    }
    
    /**
     * Log an event to Firebase Analytics
     */
    fun logEvent(eventName: String, params: Bundle? = null) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.logEvent(eventName, params)
            Log.d(TAG, "Event logged: $eventName")
        } else {
            Log.w(TAG, "Firebase Analytics not initialized, can't log event: $eventName")
        }
    }
    
    /**
     * Set user property in Firebase Analytics
     */
    fun setUserProperty(name: String, value: String) {
        if (::firebaseAnalytics.isInitialized) {
            firebaseAnalytics.setUserProperty(name, value)
            Log.d(TAG, "User property set: $name = $value")
        } else {
            Log.w(TAG, "Firebase Analytics not initialized, can't set user property: $name")
        }
    }
    
    /**
     * Check if Google Play Services are available on the device
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }
    
    // Verifica se o Firebase está conectado ao servidor
    private fun checkFirebaseConnection() {
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d(TAG, "Estado de conexão do Firebase alterado: ${if (connected) "Conectado" else "Desconectado"}")
                
                if (!connected) {
                    Log.w(TAG, "Firebase está desconectado do servidor. Os dados serão armazenados localmente até que a conexão seja restabelecida.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao verificar conexão Firebase: ${error.message}", error.toException())
            }
        })
    }
}
