package dev.brodt.taskmanager.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.brodt.taskmanager.utils.CrashlyticsUtils

/**
 * Base activity class that all activities should extend from.
 * Provides common functionality like Crashlytics page tracking.
 */
abstract class BaseActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the current page in Crashlytics
        val pageName = getPageName()
        CrashlyticsUtils.setCurrentPage(pageName)
    }
    
    /**
     * Get the name of the current page/screen for Crashlytics tracking.
     * By default, it uses the simple name of the activity class.
     * Subclasses can override this method to provide a custom page name.
     * 
     * @return The name of the current page/screen
     */
    protected open fun getPageName(): String {
        return this.javaClass.simpleName
    }
}
