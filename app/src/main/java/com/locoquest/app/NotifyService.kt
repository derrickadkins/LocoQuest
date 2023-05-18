package com.locoquest.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import com.locoquest.app.dto.User

class NotifyService: JobService() {

    var builder: NotificationCompat.Builder? = null
    var notificationManagerCompat: NotificationManagerCompat? = null

    override fun onStartJob(p0: JobParameters?): Boolean {
        Log.d(TAG, "onStartCommand:$p0")
        if(p0 == null) return false
        if(p0.extras.size() == 0) return false

        val config = Configuration.Builder()
        config.setJobSchedulerJobIdRange(0, 1000)

        if(p0.extras.containsKey("pid")) {
            val id = p0.extras.getString("pid", "")
            val name = p0.extras.getString("name", "")
            val contentText = if (name == null) "Coin is available to be collected again"
            else "Coin ($name) is available to be collected again"

            Log.d("notify receiver", "intent received for $name")

            createNotificationChannel(channels[0])
            notificationManagerCompat = NotificationManagerCompat.from(this)

            val contentIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                id.hashCode(),
                contentIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            builder = NotificationCompat.Builder(this, channels[0])
                .setSmallIcon(R.drawable.locoquest_notification_icon)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        this.resources,
                        R.drawable.coin
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLights(this.getColor(R.color.blue), 1000, 1000)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setContentTitle("LocoQuest")
                .setContentText(contentText)

            val notification = builder!!.build()
            notification.flags = notification.flags or Notification.FLAG_SHOW_LIGHTS
            notificationManagerCompat!!.notify(id.hashCode(), notification)
        }else if (p0.extras.containsKey("ordinal")){
            val ordinal = p0.extras.getInt("ordinal", -1)
            val name = p0.extras.getString("name", "")
            val available = p0.extras.getBoolean("available", false)
            val skill = Skill.values()[ordinal]
            var contentText = "$name is available to be used again"

            if(skill == Skill.COMPANION && !available) {
                val coinsCollected = p0.extras.getInt("coinsCollected")
                User.load(this){
                    AppModule.user.balance += coinsCollected
                    AppModule.user.update()
                    AppModule.scheduleNotification(this, skill)
                }
                contentText = "Companion collected ($coinsCollected) coins"
            }else if(!available) User.load(this){
                AppModule.scheduleNotification(this, skill)
            }

            Log.d("notify receiver", "intent received for $name")

            createNotificationChannel(channels[0])
            notificationManagerCompat = NotificationManagerCompat.from(this)

            val contentIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                ordinal,
                contentIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val resource = when(skill){
                Skill.COMPANION -> R.drawable.spot
                Skill.TIME -> R.drawable.time_travel
                Skill.DRONE -> R.drawable.drone2
                Skill.GIANT -> R.drawable.iron_giant
            }

            builder = NotificationCompat.Builder(this, channels[0])
                .setSmallIcon(R.drawable.locoquest_notification_icon)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        this.resources,
                        resource
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLights(this.getColor(R.color.blue), 1000, 1000)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setContentTitle("LocoQuest")
                .setContentText(contentText)

            val notification = builder!!.build()
            notification.flags = notification.flags or Notification.FLAG_SHOW_LIGHTS
            notificationManagerCompat!!.notify(ordinal, notification)
        }

        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        return false
    }

    private fun createNotificationChannel(channelInfo: String) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannelCompat.Builder(channelInfo, NotificationManager.IMPORTANCE_HIGH)
                    .setName(channelInfo)
                    .setDescription(channelInfo) //.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                    .setVibrationEnabled(true)
                    .setLightsEnabled(true)
                    .setVibrationPattern(longArrayOf(1000, 1000, 1000, 1000, 1000))
                    .setLightColor(this.getColor(R.color.blue))
                    .build()
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    companion object{
        private const val TAG = "NotifyService"
        private val channels = arrayOf("Coin", "Skill")
    }
}