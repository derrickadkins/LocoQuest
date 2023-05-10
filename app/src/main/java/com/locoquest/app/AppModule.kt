package com.locoquest.app

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.locoquest.app.dao.DB
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User

class AppModule {
    companion object{
        var db: DB? = null
        val guest: User = User("0","Guest")
        var user: User = guest
        const val DEFAULT_REACH = 150.0
        const val BOOSTED_REACH = 500.0
        const val BOOSTED_DURATION = 300
        const val DEBUG = false
        val SECONDS_TO_RECOLLECT = if(DEBUG) 30 else 14400 // 4 hrs

        fun scheduleNotification(context: Context, benchmark: Benchmark) {
            val extras = PersistableBundle()
            extras.putString("pid", benchmark.pid)
            extras.putString("name", benchmark.name)
            val delayMillis = System.currentTimeMillis() - benchmark.lastVisited * 1000 + SECONDS_TO_RECOLLECT
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfoBuilder = JobInfo.Builder(benchmark.pid.hashCode(), ComponentName(context, NotifyReceiver::class.java))
                .setMinimumLatency(delayMillis)
                .setOverrideDeadline(delayMillis + 1000)
                .setPersisted(true)
                .setExtras(extras)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                jobInfoBuilder.setExpedited(true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                jobInfoBuilder.setPriority(JobInfo.PRIORITY_MAX)
            }

            jobScheduler.schedule(jobInfoBuilder.build())
        }

        fun cancelNotification(context: Context, benchmark: Benchmark) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(benchmark.pid.hashCode())
        }
    }
}