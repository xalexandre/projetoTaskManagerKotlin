package dev.brodt.taskmanager.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dev.brodt.taskmanager.Task
import dev.brodt.taskmanager.TaskManagerApplication
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

/**
 * Background service for checking upcoming tasks and showing reminders
 */
class TaskReminderService : Service() {
    private val TAG = "TaskReminderService"
    private lateinit var timer: Timer
    private lateinit var notificationHelper: NotificationHelper
    
    companion object {
        private const val CHECK_INTERVAL = 15 * 60 * 1000L // 15 minutes
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TaskReminderService created")
        notificationHelper = NotificationHelper(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TaskReminderService started")
        
        // Log event to Firebase Analytics
        val app = application as TaskManagerApplication
        app.logEvent("reminder_service_started")
        
        startReminderChecks()
        
        // If service is killed, restart it
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        Log.d(TAG, "TaskReminderService destroyed")
    }
    
    /**
     * Start periodic checks for upcoming tasks
     */
    private fun startReminderChecks() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkUpcomingTasks()
            }
        }, 0, CHECK_INTERVAL)
    }
    
    /**
     * Check for tasks that are coming up soon
     */
    private fun checkUpcomingTasks() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, skipping task check")
            return
        }
        
        val userId = currentUser.uid
        val tasksRef = FirebaseDatabase.getInstance().getReference("users/$userId/tasks")
        
        tasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calendar = Calendar.getInstance()
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val currentTime = calendar.timeInMillis
                
                for (taskSnapshot in snapshot.children) {
                    try {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null && task.date == currentDate) {
                            // Parse task time
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val taskTime = timeFormat.parse("${task.time}")
                            
                            if (taskTime != null) {
                                val taskCalendar = Calendar.getInstance()
                                taskCalendar.time = taskTime
                                
                                // Set task calendar to today with the task time
                                val currentCalendar = Calendar.getInstance()
                                taskCalendar.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
                                taskCalendar.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
                                taskCalendar.set(Calendar.DAY_OF_MONTH, currentCalendar.get(Calendar.DAY_OF_MONTH))
                                
                                // Calculate time difference in minutes
                                val timeDiff = (taskCalendar.timeInMillis - currentTime) / (60 * 1000)
                                
                                // If task is coming up in the next 30 minutes
                                if (timeDiff in 0..30) {
                                    Log.d(TAG, "Found upcoming task: ${task.title} at ${task.time}")
                                    notificationHelper.showTaskReminderNotification(
                                        task.id,
                                        task.title,
                                        task.time
                                    )
                                    
                                    // Log event to Firebase Analytics
                                    val app = application as TaskManagerApplication
                                    val params = android.os.Bundle().apply {
                                        putString("task_id", task.id)
                                        putString("task_title", task.title)
                                    }
                                    app.logEvent("task_reminder_sent", params)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing task: ${e.message}", e)
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error checking tasks: ${error.message}", error.toException())
            }
        })
    }
}
