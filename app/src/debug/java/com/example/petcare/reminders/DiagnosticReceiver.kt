package com.example.petcare.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import com.example.petcare.PetCareApp
import com.example.petcare.R

class DiagnosticReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PetCareApp
        when (intent.action) {
            "com.example.petcare.DEBUG_SCHEDULE" -> {
                val requested = System.currentTimeMillis() + 20_000
                val delivery = Intent(context, DiagnosticReceiver::class.java)
                    .setAction("com.example.petcare.DEBUG_DELIVER").setData("petcare://diagnostic/background".toUri())
                    .putExtra("requested_at", requested)
                app.scheduler.setAlarm(requested, delivery)
                Log.i("PetCareDiagnostic", "Scheduled background diagnostic for $requested")
            }
            "com.example.petcare.DEBUG_DELIVER" -> {
                val requested = intent.getLongExtra("requested_at", 0)
                Log.i("PetCareDiagnostic", "Delivered at ${System.currentTimeMillis()}; requested $requested; enabled=${app.notifications.enabled()}")
                if (app.notifications.enabled()) context.getSystemService(NotificationManager::class.java).notify("diagnostic", 1,
                    NotificationCompat.Builder(context, NotificationHelper.REAL_CHANNEL)
                        .setSmallIcon(R.drawable.ic_notification).setContentTitle("Diagnostic — background reminder")
                        .setContentText("Short test alarm. Your pets and treatment dates are unchanged.")
                        .setAutoCancel(true).build())
            }
        }
    }
}
