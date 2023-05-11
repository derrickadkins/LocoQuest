package com.locoquest.app

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.locoquest.app.dao.DB
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User

class AppModule : Application() {

    companion object{
        /*
        FOR DEBUGGING ONLY
         - Skips proximity check
         - Reduces SECONDS_TO_RECOLLECT to 30
         */
        const val DEBUG = false
        //////////////////////////////////////

        var db: DB? = null
        val guest: User = User("0","Guest")
        var user: User = guest
        const val DEFAULT_REACH = 150.0
        const val BOOSTED_REACH = 500.0
        const val BOOSTED_DURATION = 300
        val SECONDS_TO_RECOLLECT = if(DEBUG) 30 else 14400 // 4 hrs

        fun scheduleNotification(context: Context, benchmark: Benchmark) {
            val extras = PersistableBundle()
            extras.putString("pid", benchmark.pid)
            extras.putString("name", benchmark.name)
            val delayMillis = System.currentTimeMillis() - benchmark.lastVisited * 1000 + SECONDS_TO_RECOLLECT * 1000
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfoBuilder = JobInfo.Builder(benchmark.pid.hashCode(), ComponentName(context, NotifyService::class.java))
                .setMinimumLatency(delayMillis)
                .setOverrideDeadline(delayMillis + 1000)
                .setPersisted(true)
                .setExtras(extras)

            jobScheduler.schedule(jobInfoBuilder.build())
        }

        fun cancelNotification(context: Context, benchmark: Benchmark) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(benchmark.pid.hashCode())
        }
    }
}