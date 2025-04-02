package dev.brodt.taskmanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

/**
 * BroadcastReceiver that starts the TaskReminderService when the device boots
 */
class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking if user is logged in")
            
            // Only start the service if a user is logged in
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Log.d(TAG, "User is logged in, starting TaskReminderService")
                val serviceIntent = Intent(context, TaskReminderService::class.java)
                context.startService(serviceIntent)
            } else {
                Log.d(TAG, "No user logged in, not starting TaskReminderService")
            }
        }
    }
}
