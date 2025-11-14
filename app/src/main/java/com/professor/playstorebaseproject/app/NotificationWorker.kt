package com.professor.playstorebaseproject.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.ui.screens.MainActivity

class NotificationWorker(
    var context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun setPendingIntent2(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)

        // Ensure proper back stack
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun setPendingIntent(): PendingIntent {
        val intent =
            Intent(/* packageContext = */ context, /* cls = */ MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return pendingIntent

    }

    private fun showNotification() {
        val channelId = "animal_sound_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Animal Sound Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val animalMessages = listOf(
            "Roar! 🦁 Your wild buddies miss you!",
            "Tap to hear the jungle speak again! 🐘🌿",
            "Moo, Meow, Roar – they’re calling you! 🐄🐱🦁",
            "It’s been 3 hours… time to go wild again! 🐾",
            "Who let the sounds out? 🐶🔊 Open and find out!",
            "Ready for another animal sound adventure? 🐍🎧",
            "Your animal orchestra is tuning up! 🐓🎼",
            "Neigh, Woof, Quack! Someone's waiting... 🐴🐕🦆",
            "Shhh... hear that? 🦉 Your sound safari is calling!",
            "Every 3 hours, a roar gets louder! 🔊🐅"
        )

        val randomMessage = animalMessages.random()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.notification_icon
                )
            )
            .setContentTitle("Animal Sound Reminder 🐾")
            .setContentText(randomMessage)
            .setContentIntent(setPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Check POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission or handle denial logic
            return
        }

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}
