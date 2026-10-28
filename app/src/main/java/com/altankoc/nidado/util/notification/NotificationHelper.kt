package com.altankoc.nidado.util.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.altankoc.nidado.MainActivity
import com.altankoc.nidado.R
import com.altankoc.nidado.ui.theme.DeepOcean
import com.altankoc.nidado.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_NAME = "Görev Hatırlatıcıları"
        private val VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300, 150, 300)
    }

    init {
        createAllNotificationChannels()
    }

    // NotificationSounds.ALL listesindeki her ses için ayrı bir kanal
    // oluşturuyoruz. Kanal sesi bir kere kurulunca değiştirilemediği için,
    // her ses kendi sabit kanalında kalıyor, yeni ses eklenince sadece
    // listeye ekleme yapmak yeterli olur, kod tekrarı gerekmez.
    private fun createAllNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            NotificationSounds.ALL.forEach { sound ->
                val channelId = NotificationSounds.channelIdFor(sound.id)
                val soundUri = Uri.parse("android.resource://${context.packageName}/${sound.resId}")
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    channelId,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Görev ve liste hatırlatıcı bildirimleri"
                    setSound(soundUri, audioAttributes)
                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                    enableLights(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun showTaskReminder(
        notificationId: Int,
        listTitle: String,
        taskTitle: String,
        listId: Int,
        avatarId: Int,
        soundId: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("listId", listId)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val avatarRes = Constants.AVATARS.getOrElse(avatarId - 1) { Constants.AVATARS[0] }
        val largeIconBitmap = BitmapFactory.decodeResource(context.resources, avatarRes)

        val message = "$listTitle - Görev zamanı geldi: $taskTitle"
        val sound = NotificationSounds.find(soundId)
        val channelId = NotificationSounds.channelIdFor(sound.id)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIconBitmap)
            .setColor(DeepOcean.toArgb())
            .setContentTitle("NinaDo")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://${context.packageName}/${sound.resId}")
            notificationBuilder
                .setSound(soundUri)
                .setVibrate(VIBRATION_PATTERN)
        }

        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}