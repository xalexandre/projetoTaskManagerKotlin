package dev.brodt.taskmanager.utils

import android.util.Log
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.google.firebase.perf.metrics.HttpMetric
import dev.brodt.taskmanager.utils.CrashlyticsUtils

/**
 * Utility class for Firebase Performance Monitoring.
 * Provides methods for creating and managing traces and HTTP metrics.
 */
object PerformanceUtils {
    private const val TAG = "PerformanceUtils"
    
    // Cache of active traces to prevent leaks
    private val activeTraces = mutableMapOf<String, Trace>()
    private val activeHttpMetrics = mutableMapOf<String, HttpMetric>()
    
    /**
     * Start a trace to measure a specific operation.
     * 
     * @param traceName The name of the trace
     * @return The trace object for chaining
     */
    fun startTrace(traceName: String): Trace {
        try {
            Log.d(TAG, "Starting trace: $traceName")
            val trace = FirebasePerformance.getInstance().newTrace(traceName)
            trace.start()
            activeTraces[traceName] = trace
            return trace
        } catch (e: Exception) {
            Log.e(TAG, "Error starting trace: ${e.message}", e)
            CrashlyticsUtils.recordException("Error starting performance trace", e)
            // Return a dummy trace that does nothing to avoid null checks
            return FirebasePerformance.getInstance().newTrace("dummy_trace")
        }
    }
    
    /**
     * Stop a previously started trace.
     * 
     * @param traceName The name of the trace to stop
     */
    fun stopTrace(traceName: String) {
        try {
            val trace = activeTraces.remove(traceName)
            if (trace != null) {
                trace.stop()
                Log.d(TAG, "Stopped trace: $traceName")
            } else {
                Log.w(TAG, "Attempted to stop non-existent trace: $traceName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping trace: ${e.message}", e)
            CrashlyticsUtils.recordException("Error stopping performance trace", e)
        }
    }
    
    /**
     * Add a metric to a trace.
     * 
     * @param traceName The name of the trace
     * @param metricName The name of the metric
     * @param value The value of the metric
     */
    fun putMetric(traceName: String, metricName: String, value: Long) {
        try {
            val trace = activeTraces[traceName]
            if (trace != null) {
                trace.putMetric(metricName, value)
                Log.d(TAG, "Added metric to trace $traceName: $metricName = $value")
            } else {
                Log.w(TAG, "Attempted to add metric to non-existent trace: $traceName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding metric to trace: ${e.message}", e)
            CrashlyticsUtils.recordException("Error adding metric to trace", e)
        }
    }
    
    /**
     * Add an attribute to a trace.
     * 
     * @param traceName The name of the trace
     * @param attributeName The name of the attribute
     * @param value The value of the attribute
     */
    fun putAttribute(traceName: String, attributeName: String, value: String) {
        try {
            val trace = activeTraces[traceName]
            if (trace != null) {
                trace.putAttribute(attributeName, value)
                Log.d(TAG, "Added attribute to trace $traceName: $attributeName = $value")
            } else {
                Log.w(TAG, "Attempted to add attribute to non-existent trace: $traceName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding attribute to trace: ${e.message}", e)
            CrashlyticsUtils.recordException("Error adding attribute to trace", e)
        }
    }
    
    /**
     * Start an HTTP metric to measure network requests.
     * 
     * @param url The URL of the request
     * @param httpMethod The HTTP method (GET, POST, etc.)
     * @return The HTTP metric object for chaining
     */
    fun startHttpMetric(url: String, httpMethod: String): HttpMetric {
        try {
            Log.d(TAG, "Starting HTTP metric: $httpMethod $url")
            val key = "$httpMethod:$url"
            val metric = FirebasePerformance.getInstance().newHttpMetric(url, httpMethod)
            metric.start()
            activeHttpMetrics[key] = metric
            return metric
        } catch (e: Exception) {
            Log.e(TAG, "Error starting HTTP metric: ${e.message}", e)
            CrashlyticsUtils.recordException("Error starting HTTP metric", e)
            // Return a dummy metric that does nothing to avoid null checks
            return FirebasePerformance.getInstance().newHttpMetric("https://example.com", "GET")
        }
    }
    
    /**
     * Stop a previously started HTTP metric.
     * 
     * @param url The URL of the request
     * @param httpMethod The HTTP method (GET, POST, etc.)
     * @param responseCode The HTTP response code
     * @param responseSize The size of the response in bytes
     */
    fun stopHttpMetric(url: String, httpMethod: String, responseCode: Int = 200, responseSize: Long = 0) {
        try {
            val key = "$httpMethod:$url"
            val metric = activeHttpMetrics.remove(key)
            if (metric != null) {
                metric.setHttpResponseCode(responseCode)
                metric.setResponseContentType("application/json")
                metric.setResponsePayloadSize(responseSize)
                metric.stop()
                Log.d(TAG, "Stopped HTTP metric: $httpMethod $url, response code: $responseCode")
            } else {
                Log.w(TAG, "Attempted to stop non-existent HTTP metric: $httpMethod $url")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTP metric: ${e.message}", e)
            CrashlyticsUtils.recordException("Error stopping HTTP metric", e)
        }
    }
    
    /**
     * Execute a block of code with performance tracing.
     * 
     * @param traceName The name of the trace
     * @param block The code block to execute
     * @return The result of the code block
     */
    inline fun <T> trace(traceName: String, block: () -> T): T {
        val trace = startTrace(traceName)
        try {
            return block()
        } finally {
            stopTrace(traceName)
        }
    }
}
