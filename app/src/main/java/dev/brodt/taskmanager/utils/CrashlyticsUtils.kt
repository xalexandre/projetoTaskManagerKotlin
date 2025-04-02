package dev.brodt.taskmanager.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Utility class for Firebase Crashlytics operations.
 * Provides methods for setting user information, logging, and recording exceptions.
 */
object CrashlyticsUtils {
    private const val TAG = "CrashlyticsUtils"
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    /**
     * Sets the user ID in Crashlytics to help identify crash reports.
     * Should be called after user login.
     */
    fun setUserId() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                // Set user ID in Crashlytics
                crashlytics.setUserId(user.uid)
                Log.d(TAG, "Set user ID in Crashlytics: ${user.uid}")
                
                // Set additional user information as custom keys
                user.email?.let { email ->
                    crashlytics.setCustomKey("user_email", email)
                }
                
                user.displayName?.let { name ->
                    crashlytics.setCustomKey("user_display_name", name)
                }
                
                crashlytics.setCustomKey("user_is_anonymous", user.isAnonymous)
                crashlytics.setCustomKey("user_is_email_verified", user.isEmailVerified)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user ID in Crashlytics", e)
        }
    }
    
    /**
     * Clears the user ID from Crashlytics.
     * Should be called after user logout.
     */
    fun clearUserId() {
        try {
            crashlytics.setUserId("")
            crashlytics.setCustomKey("user_email", "")
            crashlytics.setCustomKey("user_display_name", "")
            crashlytics.setCustomKey("user_is_anonymous", false)
            crashlytics.setCustomKey("user_is_email_verified", false)
            Log.d(TAG, "Cleared user ID from Crashlytics")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user ID from Crashlytics", e)
        }
    }
    
    /**
     * Sets a custom key in Crashlytics.
     * Custom keys help provide additional context in crash reports.
     *
     * @param key The key name
     * @param value The string value
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key in Crashlytics", e)
        }
    }
    
    /**
     * Sets a custom key in Crashlytics.
     *
     * @param key The key name
     * @param value The boolean value
     */
    fun setCustomKey(key: String, value: Boolean) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key in Crashlytics", e)
        }
    }
    
    /**
     * Sets a custom key in Crashlytics.
     *
     * @param key The key name
     * @param value The int value
     */
    fun setCustomKey(key: String, value: Int) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key in Crashlytics", e)
        }
    }
    
    /**
     * Sets a custom key in Crashlytics.
     *
     * @param key The key name
     * @param value The long value
     */
    fun setCustomKey(key: String, value: Long) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key in Crashlytics", e)
        }
    }
    
    /**
     * Sets a custom key in Crashlytics.
     *
     * @param key The key name
     * @param value The float value
     */
    fun setCustomKey(key: String, value: Float) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key in Crashlytics", e)
        }
    }
    
    /**
     * Sets a custom key in Crashlytics.
     *
     * @param key The key name
     * @param value The double value
     */
    fun setCustomKey(key: String, value: Double) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key in Crashlytics", e)
        }
    }
    
    /**
     * Logs a message to Crashlytics.
     * These logs will be included in crash reports.
     *
     * @param message The message to log
     */
    fun log(message: String) {
        try {
            crashlytics.log(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging to Crashlytics", e)
        }
    }
    
    /**
     * Records a non-fatal exception in Crashlytics.
     * Use this for exceptions that don't crash the app but should be tracked.
     *
     * @param throwable The exception to record
     */
    fun recordException(throwable: Throwable) {
        try {
            crashlytics.recordException(throwable)
            Log.e(TAG, "Recorded exception in Crashlytics", throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording exception in Crashlytics", e)
        }
    }
    
    /**
     * Records a non-fatal exception in Crashlytics with a custom message.
     *
     * @param message A custom message describing the exception
     * @param throwable The exception to record
     */
    fun recordException(message: String, throwable: Throwable) {
        try {
            log(message)
            crashlytics.recordException(throwable)
            Log.e(TAG, message, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording exception in Crashlytics", e)
        }
    }
    
    /**
     * Sets the current page/screen in Crashlytics.
     * This helps identify which screen the user was on when a crash occurred.
     *
     * @param pageName The name of the current page/screen
     */
    fun setCurrentPage(pageName: String) {
        try {
            crashlytics.setCustomKey("page", pageName)
            log("User navigated to page: $pageName")
            Log.d(TAG, "Set current page in Crashlytics: $pageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting current page in Crashlytics", e)
        }
    }
    
    /**
     * Forces a crash for testing Crashlytics.
     * Only use this in debug builds for testing purposes.
     */
    fun forceCrash() {
        crashlytics.log("Forced crash for testing Crashlytics")
        throw RuntimeException("Test Crash")
    }
}
