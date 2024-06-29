package com.dausuel.countdownwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for monitoring screen state changes.
 *
 * This class:
 * - Receives broadcasts for screen on/off events
 * - Logs the screen state changes
 *
 * Note: This receiver is currently not actively used in the main countdown logic,
 *      but provides a framework for handling screen state changes if needed in the future.
 *
 * Note 2: ACTION_SCREEN_ON/OFF are burst transmissions sent when the event
 *      happens and can't be used to determine if the screen is currently on/off.
 *      The screenOff() function in CountdownApp.kt is used to dynamically determine
 *      the current screen state.
 */
class ScreenStateReceiver : BroadcastReceiver(){

    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast.
     *
     * @param context The context in which the receiver is running
     * @param intent The Intent being received
     */
    override fun onReceive(context: Context, intent: Intent) {
        var pauseCount : Boolean
        when (intent.action){
            Intent.ACTION_SCREEN_ON ->{
                Log.d("ScreenState", "Screen On")
                pauseCount = screenOff(context)

            }
            Intent.ACTION_SCREEN_OFF ->{
                Log.d("ScreenState", "Screen Off")
                pauseCount = screenOff(context)
            }
        }
    }
}

// XML Modifications:
// No direct XML modifications required for this file.
// The receiver is registered programmatically in CountdownApp.kt
