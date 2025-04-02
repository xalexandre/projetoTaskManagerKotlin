package dev.brodt.taskmanager.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dev.brodt.taskmanager.utils.CrashlyticsUtils

/**
 * Base fragment class that all fragments should extend from.
 * Provides common functionality like Crashlytics page tracking.
 */
abstract class BaseFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set the current page in Crashlytics
        val pageName = getPageName()
        CrashlyticsUtils.setCurrentPage(pageName)
    }
    
    /**
     * Get the name of the current page/screen for Crashlytics tracking.
     * By default, it uses the simple name of the fragment class.
     * Subclasses can override this method to provide a custom page name.
     * 
     * @return The name of the current page/screen
     */
    protected open fun getPageName(): String {
        return this.javaClass.simpleName
    }
}
