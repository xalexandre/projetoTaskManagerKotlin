package dev.brodt.taskmanager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.brodt.taskmanager.MainActivity
import dev.brodt.taskmanager.R
import dev.brodt.taskmanager.TaskActivity

/**
 * Helper class for creating and showing notifications
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationHelper"
        
        // Notification channels
        const val CHANNEL_TASKS = "tasks_channel"
        const val CHANNEL_REMINDERS = "reminders_channel"
        
        // Notification IDs
        const val NOTIFICATION_TASK_CREATED = 1001
        const val NOTIFICATION_TASK_REMINDER = 1002
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Tasks channel
            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Tasks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for task creation and updates"
            }
            
            // Reminders channel
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming tasks"
            }
            
            // Register the channels with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(tasksChannel)
            notificationManager.createNotificationChannel(remindersChannel)
            
            Log.d(TAG, "Notification channels created")
        }
    }
    
    /**
     * Show a notification for task creation
     */
    fun showTaskCreatedNotification(taskId: String, taskTitle: String) {
        val intent = Intent(context, TaskActivity::class.java).apply {
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.add_48)
            .setContentTitle("Task Created")
            .setContentText("Task \"$taskTitle\" has been created")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_TASK_CREATED, notification)
            Log.d(TAG, "Task created notification shown for task: $taskTitle")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for showing notification: ${e.message}")
        }
    }
    
    /**
     * Show a notification for task reminder
     */
    fun showTaskReminderNotification(taskId: String, taskTitle: String, taskTime: String) {
        val intent = Intent(context, TaskActivity::class.java).apply {
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.schedule_48)
            .setContentTitle("Task Reminder")
            .setContentText("Task \"$taskTitle\" is scheduled for $taskTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_TASK_REMINDER, notification)
            Log.d(TAG, "Task reminder notification shown for task: $taskTitle")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for showing notification: ${e.message}")
        }
    }
}
