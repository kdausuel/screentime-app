package com.dausuel.countdownwidget
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Application class for the widget
 *
 * This class:
 * - initializes app components/services
 *
 * This class initializes the UpdateCountdownCallback to ensure the background
 * update system is ready when the app starts. This is important because it
 * allows the app to work even if the user doesn't launch the main app (Main
 * Activity).
 */
class CountdownApp : Application() {
    private val screenStateReceiver = ScreenStateReceiver()

    /**
     * Called when the application is starting, before any other application objects have
     * been created.
     *
     * Initializes the UpdateCountdownCallback and registers the ScreenStateReceiver.
     */
    override fun onCreate() {
        // Called when the application process is created automatically
        super.onCreate()

        Log.d("App", "Application onCreate called")
        UpdateCountdownCallback.initialize(this)


        val filter = IntentFilter().apply{
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    /**
     * Called when the application is stopping.
     *
     * Unregisters the ScreenStateReceiver to prevent memory leaks.
     */
    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(screenStateReceiver)
    }

    /**
     * Cleans up all app-related data when all widgets have been removed.
     *
     * This method should be called when the last widget is deleted.
     */
    fun cleanupAllData() {
        val prefs = getSharedPreferences("CountConfig", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        // Cancel all coroutines
        UpdateCountdownCallback.killScope()

        // Unregister receivers
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was already unregistered
            Log.d("CountdownApp", "ScreenStateReceiver was already unregistered", e)
        }
        Log.d("CountdownApp", "All app data cleaned up")
    }
}


/**
 * Checks if the device screen is off.
 *
 * @param context The application context
 * @return true if the screen is off, false otherwise
 */
fun screenOff(context: Context) : Boolean{
    // Check initial screen state
    // Helpful for checking the screen state at any point
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return !powerManager.isInteractive
}

/**
 * Checks and requests overlay permission if not granted.
 *
 * This is necessary for displaying the notification dialog over other apps.
 *
 * @param context The application context
 */

fun checkOverlayPermission(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

// XML Modifications:
// 1. Updated AndroidManifest.xml to declare CountdownApp as the application class:
//    android:name=".CountdownApp"
// 2. Added SYSTEM_ALERT_WINDOW permission to AndroidManifest.xml for overlay functionality:
//    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />