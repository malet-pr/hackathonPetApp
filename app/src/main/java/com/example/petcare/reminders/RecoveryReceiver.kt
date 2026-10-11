package com.example.petcare.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.petcare.PetCareApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        val app = context.applicationContext as PetCareApp
        app.applicationScope.launch {
            try { withTimeout(8_000) { app.scheduler.reconcile() } }
            catch (error: Exception) { Log.e("PetCareRecovery", "Reminder recovery will retry on launch", error) }
            finally { pending.finish() }
        }
    }
}
