package com.dausuel.countdownwidget


import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.Handler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Manages the UI and state of the widget
 *
 * This object:
 * - preserves the data using Glance preferences
 * - defines UI layout
 *
 * The initial countdown value and the Start button are displayed while the
 * countdown is not running. The Start button disappears once the countdown
 * starts.
 */
object CountdownWidget: GlanceAppWidget() {
    private const val TAG = "CountdownWidget"
    val countdownKey = intPreferencesKey("countdown") //Stores current val for countdown
    val isRunningKey = booleanPreferencesKey("is_running") //Bool for if countdown is running
    val pausedCountdown = booleanPreferencesKey("paused")

    /**
     * Updates the widget UI when a change is made that should be reflected to the user
     *
     * @param context The app context
     * @param glanceId Unique ID for the instance of the widget
     */
    suspend fun updateWidget(context: Context, glanceId: GlanceId){ this.update(context, glanceId) }


    /**
     * Gets the starting value for the countdown from the configuration done by
     * the user (stored in SharedPreferences)
     *
     * @param context App context
     * @param glanceId Unique ID for instance
     * @return The starting value for the countdown or the default value 25 if null
     */
    fun getInitialNumber(context: Context, glanceId: GlanceId) : Int{
        val config = context.getSharedPreferences("CountConfig", Context.MODE_PRIVATE)
        Log.d("getInitialNUmber", "value pulled from config: ${config.getInt(
            "initialNumber-$glanceId", 25)}")
        return config.getInt("initialNumber-$glanceId", 25)
    }

    /**
     * Generates the Glance UI content/layout for the widget
     *
     * - Checks for overlay permission to put dialog over other apps (User has to approve it)
     * - Gets the current value and state for the countdown
     * - Creates the UI
     *
     * @param context app context
     * @param id Unique id for instance (GlanceID)
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        //Function for driving UI
        Log.d(TAG, "provideGlance called")
        checkOverlayPermission(context)
        provideContent {
            val countdown =  currentState(key = countdownKey) ?: getInitialNumber(context, id)
            val isRunning = currentState(key = isRunningKey) ?: false
            Column(GlanceModifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = if (countdown > 0) "Countdown: $countdown" else "Done!",
                    style = TextStyle(
                        color = ColorProvider(Color.Black),
                        fontSize = 24.sp,
                        fontFamily = androidx.glance.text.FontFamily.SansSerif,
                        fontWeight = androidx.glance.text.FontWeight.Bold)
                    )
                if (!isRunning && countdown > 0){
                    Button(
                        text = "Start",
                        onClick = actionRunCallback(UpdateCountdownCallback::class.java)
                    )
                }
            }
        }
    }
}

/**
 * Required bridge between Android system and Glance implementation
 *
 * This class:
 * - declares widget to Android system
 * - handles lifecycle events (create, update, delete)
 * - provides GlanceAppWidget implementation to system
 *
 * Android uses this receiver to manage widget lifecycle but application
 * logic is contained in UpdateCountdownCallback
 */
class CountdownWidgetReceiver : GlanceAppWidgetReceiver(){
    override val glanceAppWidget : GlanceAppWidget
        get() = CountdownWidget

    /**
     * Deletes all of the data once the widget is deleted. Runs automatically.
     *
     * @param context app context
     * @param appWidgetIds array of the appWidgetIds for all of the widgets being deleted
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        (context.applicationContext as CountdownApp).cleanupAllData()
    }
}

/**
 * Main widget logic, state updates, and initializes the widget
 *
 * This object:
 * - starts/stops the countdown
 * - manages the scheduling of the updates using Handler
 * - updates the countdown state and values being stored
 * - updating the values displayed on the UI.
 *
 * initialize() is called from the Application class in CountdownApp.kt to
 * setup the widget's background update mechanism
 */
object UpdateCountdownCallback : ActionCallback {
    private const val TAG = "CountdownWidget"
    val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun killScope(){widgetScope.cancel()}

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var appContext: Context

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (::appContext.isInitialized) {
                widgetScope.launch{
                    val widgetManager = GlanceAppWidgetManager(appContext)
                    widgetManager.getGlanceIds(CountdownWidget::class.java).forEach { glanceId ->
                        if (CountdownConfigActivity.getConfig()){
                        updateCountdown(appContext, glanceId)
                        }

                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * Initializes the updating mechanism
     *
     * @param context app context
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        handler.post(countdownRunnable)
    }

    /**
     * Function that handles what happens when user clicks the Start button
     *
     * @param context app context
     * @param glanceId Unique ID for instance
     * @param parameters Not used
     */
    override suspend fun onAction(context: Context, glanceId: GlanceId,
                                  parameters: ActionParameters) {
        Log.d(TAG, "onAction called")
        startCountdown(context, glanceId)
    }

    /**
     * Updates the countdown value and widget state.
     *
     * - Checks if the countdown is running or paused
     * - Decrements the countdown value
     * - Handles pause state
     * - Updates the widget UI
     *
     * @param context app context
     * @param glanceId Unique id for this widget instance
     */
    suspend fun updateCountdown(context: Context, glanceId: GlanceId){
        Log.d(TAG, "updateCountdown called")
        updateAppWidgetState(context, glanceId) { prefs ->
            val isRunning = prefs[CountdownWidget.isRunningKey] ?: false
            prefs[CountdownWidget.pausedCountdown] = screenOff(context)
            val paused = prefs[CountdownWidget.pausedCountdown] ?: false
            Log.d(TAG, "paused: ${paused.toString()}")
            Log.d(TAG, "isRunning Status: $isRunning")
            if (isRunning) {
                val currentCount = prefs[CountdownWidget.countdownKey] ?: 0
                if (currentCount > 0 && !paused) {
                    Log.d("Update", "currentCount before decrement: $currentCount")
                    prefs[CountdownWidget.countdownKey] = currentCount - 1
                } else if (paused){
                    prefs[CountdownWidget.countdownKey] = currentCount
                    Log.d("Paused", "currentCount: $currentCount")
                }
                else {
                    prefs[CountdownWidget.isRunningKey] = false
                    widgetScope.launch(Dispatchers.Main) {
                        launchNotification(context)
                    }
                    stopCountdown(context, glanceId)
                }
            }
        }
        CountdownWidget.updateWidget(context,glanceId)
        Log.d(TAG, "Widget update called")
    }

    /**
     * Starts the countdown for the widget.
     *
     * This function:
     * - Retrieves the initial countdown value
     * - Sets the running state to true
     * - Initializes the countdown value
     * - Sets the pause state to false
     *
     * @param context The application context
     * @param glanceId The unique identifier for this widget instance
     */
    private suspend fun startCountdown(context: Context, glanceId: GlanceId) {
        Log.d(TAG, "startCountdown called")
        Log.d(TAG, "saved val: ${CountdownWidget.getInitialNumber(context, glanceId)}")
        val initialNumber = CountdownWidget.getInitialNumber(context, glanceId)
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[CountdownWidget.isRunningKey] = true
            prefs[CountdownWidget.countdownKey] = initialNumber
            prefs[CountdownWidget.pausedCountdown] = false
        }
    }

    /**
     * Stops the countdown for the widget.
     *
     * This function:
     * - Sets the running state to false
     * - Updates the widget UI
     *
     * @param context The application context
     * @param glanceId The unique identifier for this widget instance
     */
    private suspend fun stopCountdown(context: Context, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[CountdownWidget.isRunningKey] = false
        }
        CountdownWidget.update(context, glanceId)
    }

}

/**
 * Launches the notification activity when the countdown reaches zero.
 *
 * @param context The application context
 */
private fun launchNotification(context: Context){
    Log.d("CountdownWidget", "launchNotification...launched")
    val intent = Intent(context, NotificationActivity::class.java).apply{
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

// XML Modifications:
// 1. Created a new XML file glance_widget_provider.xml in the res/xml directory to define widget
//    properties:
//    - Set updatePeriodMillis to 0 (update frequency is controlled in the code)
//    - Defined initial layout and size
//    - Set resizeMode to horizontal|vertical for user resizing
// 2. Modified the AndroidManifest.xml to declare the widget:
//    - Added a receiver for CountdownWidgetReceiver
//    - Set android:exported="false" for security
//    - Added intent-filter for APPWIDGET_UPDATE action
//    - Added meta-data pointing to the glance_widget_provider.xml
// 3. Created a layout XML file for the widget UI -- NOT BEING USED/DELETED