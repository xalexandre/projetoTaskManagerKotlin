package dev.brodt.taskmanager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.brodt.taskmanager.MainActivity
import dev.brodt.taskmanager.R
import dev.brodt.taskmanager.TaskManagerApplication
import dev.brodt.taskmanager.utils.CrashlyticsUtils
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service to handle Firebase Cloud Messaging (FCM) messages.
 */
class FCMService : FirebaseMessagingService() {
    private val TAG = "FCMService"
    
    // Counter for notification IDs
    companion object {
        private val notificationIdCounter = AtomicInteger(0)
        
        // Channel IDs
        const val CHANNEL_GENERAL = "general_notifications"
        const val CHANNEL_TASKS = "task_notifications"
        const val CHANNEL_REMINDERS = "reminder_notifications"
        
        // Create notification channels for Android O and above
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // General notifications channel
                val generalChannel = NotificationChannel(
                    CHANNEL_GENERAL,
                    context.getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_general_description)
                }
                
                // Task notifications channel
                val tasksChannel = NotificationChannel(
                    CHANNEL_TASKS,
                    context.getString(R.string.notification_channel_tasks),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_tasks_description)
                }
                
                // Reminder notifications channel
                val remindersChannel = NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.notification_channel_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_reminders_description)
                }
                
                // Register the channels with the system
                notificationManager.createNotificationChannels(listOf(generalChannel, tasksChannel, remindersChannel))
                Log.d("FCMService", "Notification channels created")
            }
        }
    }
    
    /**
     * Called when a new token is generated.
     * This happens on app install or when a new token is needed.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")
        
        try {
            // Log token refresh event
            CrashlyticsUtils.log("FCM token refreshed")
            
            // Send the token to your server
            sendRegistrationToServer(token)
            
            // Log analytics event
            val app = application as TaskManagerApplication
            app.logEvent("fcm_token_refreshed")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new FCM token", e)
            CrashlyticsUtils.recordException("Error handling new FCM token", e)
            // Continue app execution, this is not critical for core functionality
        }
    }
    
    /**
     * Handle FCM errors gracefully
     */
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "FCM messages were deleted")
        // This can happen if the FCM storage on the device is full
        // or if the server deleted pending messages
    }
    
    /**
     * Handle FCM errors gracefully
     */
    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "FCM send error: $msgId", exception)
        CrashlyticsUtils.recordException("FCM send error", exception)
    }
    
    /**
     * Called when a message is received.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        try {
            // Check if message contains a data payload
            if (remoteMessage.data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: ${remoteMessage.data}")
                
                // Handle data payload
                handleDataMessage(remoteMessage.data)
            }
            
            // Check if message contains a notification payload
            remoteMessage.notification?.let {
                Log.d(TAG, "Message Notification Body: ${it.body}")
                
                // Handle notification payload
                handleNotificationMessage(it)
            }
            
            // Log analytics event
            val app = application as TaskManagerApplication
            app.logEvent("push_notification_received")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing FCM message", e)
            CrashlyticsUtils.recordException("Error processing FCM message", e)
        }
    }
    
    /**
     * Handle data message payload.
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: getString(R.string.app_name)
        val message = data["message"] ?: ""
        val type = data["type"] ?: CHANNEL_GENERAL
        
        // Check if this notification is for the current user
        val targetUserId = data["userId"]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        // If targetUserId is specified and doesn't match current user, ignore the message
        if (targetUserId != null && currentUserId != null && targetUserId != currentUserId) {
            Log.d(TAG, "Notification not for current user. Target: $targetUserId, Current: $currentUserId")
            return
        }
        
        // Create and show notification
        showNotification(
            context = this,
            title = title,
            message = message,
            channelId = type,
            data = data
        )
    }
    
    /**
     * Handle notification message payload.
     */
    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        val title = notification.title ?: getString(R.string.app_name)
        val message = notification.body ?: ""
        
        // Create and show notification
        showNotification(
            context = this,
            title = title,
            message = message,
            channelId = CHANNEL_GENERAL,
            data = emptyMap()
        )
    }
    
    /**
     * Show notification with the given data.
     * This method can be called directly to show a notification without going through FCM.
     * 
     * @param context The context to use for creating the notification
     * @param title The notification title
     * @param message The notification message
     * @param channelId The notification channel ID
     * @param data Additional data to include in the notification intent
     */
    fun showNotification(
        context: Context = this,
        title: String,
        message: String,
        channelId: String,
        data: Map<String, String>
    ) {
        try {
            // Create intent for notification click
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                
                // Add any extra data to the intent
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
            
            // Create pending intent
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // Get notification sound
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            // Build notification
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            
            // Get notification manager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Generate unique notification ID
            val notificationId = notificationIdCounter.incrementAndGet()
            
            // Show notification
            notificationManager.notify(notificationId, notificationBuilder.build())
            
            // Log notification display
            Log.d(TAG, "Notification displayed: ID=$notificationId, Title=$title")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
            CrashlyticsUtils.recordException("Error showing notification", e)
        }
    }
    
    /**
     * Send registration token to server.
     */
    private fun sendRegistrationToServer(token: String) {
        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (userId != null) {
            // In a real app, you would send this token to your server
            // For now, we'll just log it
            Log.d(TAG, "Sending FCM token to server for user $userId: $token")
            
            // TODO: Implement server communication to store the token
            // Example: apiService.registerDevice(userId, token)
        } else {
            Log.d(TAG, "User not logged in, skipping token registration")
        }
    }
}
