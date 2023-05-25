package com.locoquest.app

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.locoquest.app.dao.DB
import com.locoquest.app.dto.Coin
import com.locoquest.app.dto.User

class AppModule : Application() {

    companion object{
        /*
        FOR DEBUGGING ONLY
         - Skips proximity check
         - Reduces SECONDS_TO_RECOLLECT to 30
         - 0 cost to reset skills
         */
        const val DEBUG = false
        //////////////////////////////////////

        private const val TAG = "AppModule"
        var db: DB? = null
        val guest: User = User("0","Guest")
        var user: User = guest
        const val DEFAULT_REACH = 150
        val SECONDS_TO_RECOLLECT = if(DEBUG) 30 else 14400 // 4 hrs

        fun scheduleNotification(context: Context, coin: Coin) {
            val extras = PersistableBundle()
            extras.putString("pid", coin.pid)
            extras.putString("name", coin.name)
            val delayMillis = System.currentTimeMillis() - coin.lastVisited * 1000 + SECONDS_TO_RECOLLECT * 1000
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfoBuilder = JobInfo.Builder(coin.pid.hashCode(), ComponentName(context, NotifyService::class.java))
                .setMinimumLatency(delayMillis)
                .setOverrideDeadline(delayMillis + 1000)
                .setPersisted(true)
                .setExtras(extras)

            val result = jobScheduler.schedule(jobInfoBuilder.build())
            if(result == JobScheduler.RESULT_SUCCESS) Log.d(TAG, "job scheduled successfully, delaySeconds:${delayMillis/1000}")
            else Log.d(TAG, "job failed to schedule")
        }

        fun cancelNotification(context: Context, coin: Coin) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(coin.pid.hashCode())
        }

        fun scheduleNotification(context: Context, skill: Skill){
            val extras = PersistableBundle()
            extras.putInt("ordinal", skill.ordinal)
            extras.putString("name", skill.name)
            val (inUse, secondsLeft) = user.isSkillInUse(skill)
            val delayMillis = if(inUse) {
                if(skill == Skill.COMPANION) {
                    var coinsCollected = Skill.COMPANION.effect
                    if(user.upgrades.contains(Upgrade.COMPANION_MOTOR))
                        coinsCollected += Upgrade.COMPANION_MOTOR.effect
                    extras.putInt("coinsCollected", coinsCollected.toInt())
                }
                extras.putBoolean("available", false)
                secondsLeft * 1000
            } else{
                val (available, secondsUntil) = user.isSkillAvailable(skill)
                if(!available) {
                    extras.putBoolean("available", true)
                    secondsUntil * 1000
                }
                else -1
            }
            if (delayMillis < 0) return
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfoBuilder = JobInfo.Builder(skill.ordinal, ComponentName(context, NotifyService::class.java))
                .setMinimumLatency(delayMillis)
                .setOverrideDeadline(delayMillis + 1000)
                .setPersisted(true)
                .setExtras(extras)

            val result = jobScheduler.schedule(jobInfoBuilder.build())
            if(result == JobScheduler.RESULT_SUCCESS) Log.d(TAG, "job scheduled successfully, delaySeconds:${delayMillis/1000}; extras:$extras")
            else Log.d(TAG, "job failed to schedule; extras:$extras")
        }

        fun cancelNotification(context: Context, skill: Skill){
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(skill.ordinal)
        }
    }
}