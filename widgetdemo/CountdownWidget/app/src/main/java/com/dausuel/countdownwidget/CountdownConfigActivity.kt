package com.dausuel.countdownwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Activity for configuring the Countdown Widget
 *
 * This activity:
 * - Displays a UI for setting the initial countdown value
 * - Saves the configuration when the user confirms
 * - Manages the configuration state of the widget
 *
 */

class CountdownConfigActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    /**
     * Initializes the activity and sets up the configuration UI.
     *
     * @param savedInstanceState If non-null, this activity is being re-initialized after
     * previously being shut down.
     */
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        // Pulling the ID for the widget instance
        appWidgetId = getAppWidgetId(intent)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Config UI
        setContent {
            var initialNumber by remember { mutableStateOf(10) }
            Column {
                TextField(
                    value = initialNumber.toString(),
                    onValueChange = {
                        initialNumber = it.toIntOrNull() ?: 10
                    },
                    label = {Text("Initial Number")}
                )
                Button(
                    onClick = {
                        lifecycleScope.launch {saveConfig(initialNumber)}
                    }
                ){
                    Text("Save Changes")
                }
                Button(
                    onClick = {finish()}
                ){
                    Text("Cancel")
                }
            }
        }
    }

    /**
     * Saves the widget configuration.
     *
     * This function:
     * - Stores the initial countdown value in SharedPreferences
     * - Sets the widget as configured
     * - Finishes the activity with a result
     *
     * @param initialNumber The initial countdown value set by the user
     */
    private suspend fun saveConfig(initialNumber: Int){
        Log.d("saveConfig", "widgetID: $appWidgetId")
        Log.d("saveConfig", "initial number: $initialNumber")
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

        val config = getSharedPreferences("CountConfig", Context.MODE_PRIVATE)
        config.edit().putInt("initialNumber-$glanceId", initialNumber).apply()


        val config2 = getSharedPreferences("CountConfig", Context.MODE_PRIVATE)
        val stored = config2.getInt("initialNumber-$glanceId", 25)
        Log.d("saveConfig", "value pulled from config: $stored")
        setConfig(status = true)

        val resultVal = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultVal)
        finish()
    }

    companion object {
        private var IS_CONFIGURED : Boolean = false

        /**
         * Sets the configuration status of the widget.
         *
         * @param status The configuration status to set
         */
        fun setConfig(status: Boolean) { IS_CONFIGURED = status }

        /**
         * Gets the current configuration status of the widget.
         *
         * @return The current configuration status
         */
        fun getConfig() : Boolean { return IS_CONFIGURED }
    }
}

/**
 * Retrieves the widget ID from the intent.
 *
 * @param intent The intent that started the activity
 * @return The widget ID, or INVALID_APPWIDGET_ID if not found
 */
fun getAppWidgetId(intent: Intent?) : Int{
    return intent?.extras?.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
}

// XML Modifications:
// 1. Added CountdownConfigActivity to AndroidManifest.xml:
//    - Set android:exported="true"
//    - Added intent-filter for APPWIDGET_CONFIGURE action
// 2. Updated glance_widget_provider.xml to include:
//    android:configure="com.dausuel.countdownwidget.CountdownConfigActivity"
//    This tells the system to launch this activity when the widget is added