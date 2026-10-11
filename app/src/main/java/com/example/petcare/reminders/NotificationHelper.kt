package com.example.petcare.reminders

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.petcare.MainActivity
import com.example.petcare.R
import com.example.petcare.data.Pet
import com.example.petcare.data.TreatmentPlan

class NotificationHelper(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    fun createChannels() {
        manager.createNotificationChannel(NotificationChannel(REAL_CHANNEL, "Pet treatment reminders",
            NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Purchase, treatment and overdue reminders for saved pets." })
    }
    fun enabled(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            manager.getNotificationChannel(REAL_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun post(pet: Pet, plan: TreatmentPlan, kind: ReminderKind) {
        if (!enabled()) return
        val message = when (kind) {
            ReminderKind.Purchase -> "Buy flea medication for the treatment planned on ${plan.dueDate}."
            ReminderKind.Treatment -> "Flea treatment is planned for today. Open the record to review it."
            ReminderKind.Postponed -> "Your postponed flea treatment reminder."
            ReminderKind.Overdue -> "The treatment planned on ${plan.dueDate} has not been recorded as given."
        }
        val launch = Intent(context, MainActivity::class.java)
            .setData("petcare://detail/${plan.id}".toUri())
            .putExtra("pet_id", pet.id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(tag(plan.id, kind), 1, NotificationCompat.Builder(context, REAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("${pet.name} — Flea treatment")
            .setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending).setAutoCancel(true).build())
    }
    fun cancelPlan(planId: String) { ReminderKind.entries.forEach { manager.cancel(tag(planId, it), 1) } }
    private fun tag(planId: String, kind: ReminderKind) = "real/$planId/${kind.name}"
    companion object { const val REAL_CHANNEL = "real_treatments" }
}
