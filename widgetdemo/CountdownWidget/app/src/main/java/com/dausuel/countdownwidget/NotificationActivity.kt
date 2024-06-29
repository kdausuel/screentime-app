package com.dausuel.countdownwidget


import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


/**
 * Activity for displaying a notification dialog when the countdown reaches zero.
 *
 * This activity:
 * - Creates and shows an AlertDialog as an overlay
 * - Handles user interaction with the dialog
 */
class NotificationActivity: AppCompatActivity() {

    /**
     * Called when the activity is starting.
     *
     * Creates and displays an AlertDialog as an overlay.
     *
     * @param savedInstanceState If non-null, this activity is being re-initialized after
     * previously being shut down.
     */
    override fun onCreate(savedInstanceState: Bundle?){
        Log.d("NotificationSys", "onCreate launched")
        Log.d("NotificationSys",
            "Overlay Code: ${Settings.ACTION_MANAGE_OVERLAY_PERMISSION}")
        runOnUiThread {
            super.onCreate(savedInstanceState)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            val alert = AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("You Sure?")
                .setPositiveButton("Yup") { _, _ ->
                    Log.d("NotificationSys", "Notification Cleared")
                    finish()
                }
                .setOnDismissListener { finish() }
                .create()
            alert.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            alert.show()
        }

    }
}

// XML Modifications:
// Added NotificationActivity to AndroidManifest.xml:
//    - Set android:theme="@android:style/Theme.Translucent.NoTitleBar" for transparency
//    - Set android:exported="false"
