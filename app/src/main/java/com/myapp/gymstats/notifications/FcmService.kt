package com.myapp.gymstats.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.myapp.gymstats.MainActivity
import com.myapp.gymstats.R
import com.myapp.gymstats.domain.repository.WorkoutRepository
import com.myapp.gymstats.widget.WidgetEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var repository: WorkoutRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = WidgetEntryPoint.getCurrentUserId(applicationContext)
        if (userId.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                repository.saveDeviceToken(userId, token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "GymStats"
        val body = message.notification?.body ?: "¡Es hora de entrenar!"
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "gymstats_social"
        val manager =  getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Notificaciones sociales",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}