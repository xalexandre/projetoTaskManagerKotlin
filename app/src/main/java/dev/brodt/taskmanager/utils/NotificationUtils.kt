package dev.brodt.taskmanager.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import dev.brodt.taskmanager.notifications.FCMService

/**
 * Utility class for working with push notifications.
 */
object NotificationUtils {
    private const val TAG = "NotificationUtils"
    
    /**
     * Subscribe the current user to a specific topic.
     * Topics allow sending messages to multiple devices that have opted in to that topic.
     * 
     * @param topic The topic name to subscribe to
     * @param callback Callback to be invoked when subscription is complete
     */
    fun subscribeToTopic(topic: String, callback: (Boolean, Exception?) -> Unit) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                    callback(true, null)
                } else {
                    Log.e(TAG, "Failed to subscribe to topic: $topic", task.exception)
                    callback(false, task.exception)
                }
            }
    }
    
    /**
     * Unsubscribe the current user from a specific topic.
     * 
     * @param topic The topic name to unsubscribe from
     * @param callback Callback to be invoked when unsubscription is complete
     */
    fun unsubscribeFromTopic(topic: String, callback: (Boolean, Exception?) -> Unit) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                    callback(true, null)
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
                    callback(false, task.exception)
                }
            }
    }
    
    /**
     * Get the current FCM token for this device.
     * This token is used to target this specific device for notifications.
     * 
     * @param callback Callback to be invoked when token retrieval is complete
     */
    fun getDeviceToken(callback: (String?, Exception?) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Token: $token")
                    callback(token, null)
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                    callback(null, task.exception)
                }
            }
    }
    
    /**
     * Send a test notification to the current device.
     * This is useful for testing notification handling.
     * 
     * Note: In a real app, notifications should be sent from a server,
     * not directly from the app. This is just for testing purposes.
     * 
     * @param title The notification title
     * @param message The notification message
     * @param callback Callback to be invoked when sending is complete
     */
    fun sendTestNotification(title: String, message: String, callback: (Boolean, Exception?) -> Unit) {
        try {
            // Get current user ID
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            
            if (userId == null) {
                callback(false, Exception("User not logged in"))
                return
            }
            
            // Create notification data
            val data = hashMapOf(
                "title" to title,
                "message" to message,
                "userId" to userId,
                "type" to FCMService.CHANNEL_GENERAL,
                "timestamp" to System.currentTimeMillis().toString()
            )
            
            // Call Firebase Cloud Function to send the notification
            // Note: You need to have a Cloud Function set up to handle this
            FirebaseFunctions.getInstance()
                .getHttpsCallable("sendNotification")
                .call(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Test notification sent successfully")
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error sending test notification", e)
                    callback(false, e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test notification", e)
            callback(false, e)
        }
    }
    
    /**
     * Send a notification to a specific user.
     * 
     * Note: In a real app, notifications should be sent from a server,
     * not directly from the app. This is just for demonstration purposes.
     * 
     * @param userId The user ID to send the notification to
     * @param title The notification title
     * @param message The notification message
     * @param callback Callback to be invoked when sending is complete
     */
    fun sendNotificationToUser(userId: String, title: String, message: String, callback: (Boolean, Exception?) -> Unit) {
        try {
            // Create notification data
            val data = hashMapOf(
                "title" to title,
                "message" to message,
                "userId" to userId,
                "type" to FCMService.CHANNEL_TASKS,
                "timestamp" to System.currentTimeMillis().toString()
            )
            
            // Call Firebase Cloud Function to send the notification
            FirebaseFunctions.getInstance()
                .getHttpsCallable("sendNotification")
                .call(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Notification sent to user $userId successfully")
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error sending notification to user $userId", e)
                    callback(false, e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification to user", e)
            callback(false, e)
        }
    }
    
    /**
     * Send a notification to all users subscribed to a topic.
     * 
     * Note: In a real app, notifications should be sent from a server,
     * not directly from the app. This is just for demonstration purposes.
     * 
     * @param topic The topic to send the notification to
     * @param title The notification title
     * @param message The notification message
     * @param callback Callback to be invoked when sending is complete
     */
    fun sendNotificationToTopic(topic: String, title: String, message: String, callback: (Boolean, Exception?) -> Unit) {
        try {
            // Create notification data
            val data = hashMapOf(
                "title" to title,
                "message" to message,
                "topic" to topic,
                "type" to FCMService.CHANNEL_GENERAL,
                "timestamp" to System.currentTimeMillis().toString()
            )
            
            // Call Firebase Cloud Function to send the notification
            FirebaseFunctions.getInstance()
                .getHttpsCallable("sendTopicNotification")
                .call(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Notification sent to topic $topic successfully")
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error sending notification to topic $topic", e)
                    callback(false, e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification to topic", e)
            callback(false, e)
        }
    }
}
