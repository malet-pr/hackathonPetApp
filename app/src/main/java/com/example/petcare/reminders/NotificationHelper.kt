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
import java.time.Clock

class NotificationHelper(private val context: Context, private val clock: Clock? = null) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    fun createChannels() {
        manager.createNotificationChannel(NotificationChannel(REAL_CHANNEL, "Pet treatment reminders",
            NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Purchase, treatment and overdue reminders for saved pets." })
        manager.createNotificationChannel(NotificationChannel(DEMO_CHANNEL, "Demo treatment reminders",
            NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Accelerated sample reminders while Demo is visible." })
    }
    fun enabled(channel: String = REAL_CHANNEL): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            manager.getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE

    fun post(pet: Pet, plan: TreatmentPlan, kind: ReminderKind) {
        if (!enabled()) return
        val message = message(plan, kind)
        fun launch(action: String? = null): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setData("petcare://detail/${plan.id}".toUri()).setAction(action)
                .putExtra("pet_id", pet.id).putExtra("revision", plan.revision)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, REAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("${pet.name} — Flea treatment")
            .setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(launch()).setAutoCancel(true)
            .addAction(0, "Record treatment", launch("com.example.petcare.RECORD"))
        val time = clock ?: Clock.systemDefaultZone()
        if (ReminderRules.canPostpone(plan, time.instant(), time.zone)) {
            notification.addAction(0, "Postpone until 4 PM", launch("com.example.petcare.POSTPONE"))
        }
        manager.notify(tag(plan.id, kind), 1, notification.build())
    }

    fun postDemo(sessionId: String, pet: Pet, plan: TreatmentPlan, kind: ReminderKind, time: Clock) {
        if (!enabled(DEMO_CHANNEL)) return
        fun launch(action: String? = null): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setData("petcare://demo/$sessionId/${plan.id}".toUri()).setAction(action)
                .putExtra("pet_id", pet.id).putExtra("revision", plan.revision)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, DEMO_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Demo — ${pet.name} — Flea treatment")
            .setContentText(message(plan, kind)).setStyle(NotificationCompat.BigTextStyle().bigText(message(plan, kind)))
            .setContentIntent(launch()).setAutoCancel(true)
            .addAction(0, "Record treatment", launch("com.example.petcare.RECORD"))
        if (ReminderRules.canPostpone(plan, time.instant(), time.zone)) {
            notification.addAction(0, "Postpone until 4 PM", launch("com.example.petcare.POSTPONE"))
        }
        manager.notify(demoTag(sessionId, plan.id, kind), 1, notification.build())
    }

    private fun message(plan: TreatmentPlan, kind: ReminderKind) = when (kind) {
            ReminderKind.Purchase -> "Buy flea medication for the treatment planned on ${plan.dueDate}."
            ReminderKind.Treatment -> "Flea treatment is planned for today. Open the record to review it."
            ReminderKind.Postponed -> "Your postponed flea treatment reminder."
            ReminderKind.Overdue -> "The treatment planned on ${plan.dueDate} has not been recorded as given."
        }

    fun cancelPlan(planId: String) { ReminderKind.entries.forEach { manager.cancel(tag(planId, it), 1) } }
    fun cancelDemoPlan(sessionId: String, planId: String) {
        ReminderKind.entries.forEach { manager.cancel(demoTag(sessionId, planId, it), 1) }
    }
    fun cancelAllDemo() {
        manager.activeNotifications.filter { it.tag?.startsWith("demo/") == true }
            .forEach { manager.cancel(it.tag, it.id) }
    }
    private fun tag(planId: String, kind: ReminderKind) = "real/$planId/${kind.name}"
    private fun demoTag(sessionId: String, planId: String, kind: ReminderKind) = "demo/$sessionId/$planId/${kind.name}"
    companion object {
        const val REAL_CHANNEL = "real_treatments"
        const val DEMO_CHANNEL = "demo_treatments"
    }
}
