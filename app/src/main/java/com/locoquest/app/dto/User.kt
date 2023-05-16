package com.locoquest.app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.locoquest.app.AppModule
import com.locoquest.app.AppModule.Companion.BOOSTED_DURATION
import com.locoquest.app.AppModule.Companion.BOOSTED_REACH
import com.locoquest.app.AppModule.Companion.DEFAULT_REACH
import com.locoquest.app.AppModule.Companion.db
import com.locoquest.app.Skill
import com.locoquest.app.Upgrade
import kotlin.math.max

@Entity
data class User(
    @PrimaryKey
    val uid: String,
    var displayName: String = "",
    var photoUrl: String = "",
    var balance: Long = 0,
    var experience: Long = 0,
    var level: Long = 1,
    var skillPoints: Long = 0,
    var lastRadiusBoost: Timestamp = Timestamp(0,0),
    val skills: ArrayList<Skill> = ArrayList(),
    val upgrades: ArrayList<Upgrade> = ArrayList(),
    val visited: HashMap<String, Benchmark> = HashMap(),
    val friends: ArrayList<String> = ArrayList()
){

    fun update(){
        Thread{ db?.localUserDAO()?.update(this) }.start()
        push()
    }

    fun push(){
        val skillList = ArrayList<String>()
        skills.forEach { skillList.add(it.name) }

        val upgradeList = ArrayList<String>()
        upgrades.forEach { upgradeList.add(it.name) }

        val visitedList = ArrayList<HashMap<String, Any>>()
        visited.forEach { x -> visitedList.add(hashMapOf(
            "pid" to x.value.pid,
            "name" to x.value.name,
            "location" to GeoPoint(x.value.lat, x.value.lon),
            "lastVisited" to Timestamp(x.value.lastVisited, 0),
            "notify" to x.value.notify
        )) }
        Firebase.firestore.collection("users").document(AppModule.user.uid)
            .set(hashMapOf(
                "name" to displayName,
                "photoUrl" to photoUrl,
                "balance" to balance,
                "experience" to experience,
                "level" to level,
                "skillPoints" to skillPoints,
                "lastRadiusBoost" to lastRadiusBoost,
                "skills" to skillList.toList(),
                "upgrades" to upgradeList.toList(),
                "visited" to visitedList.toList(),
                "friends" to friends.toList()
            ))
    }

    fun isBoosted(): Boolean {
        return AppModule.user.lastRadiusBoost.seconds + BOOSTED_DURATION > Timestamp.now().seconds
    }

    fun getReach(): Double {
        return if (isBoosted()) BOOSTED_REACH else DEFAULT_REACH
    }

    fun dump() : String{
        return "uid: $uid, name:$displayName, photoUrl:$photoUrl, balance:$balance, lastRadiusBoost:${lastRadiusBoost.seconds}, visited:${visited.size}, friends:${friends.size}"
    }

    override fun toString(): String {
        return displayName
    }

    companion object{
        fun pullUser(it: DocumentSnapshot) : User{
            val name =
                if (it["name"] == null) AppModule.user.displayName else it["name"] as String

            val photoUrl = if (it["photoUrl"] == null) {
                AppModule.user.photoUrl = Firebase.auth.currentUser?.photoUrl.toString()
                AppModule.user.photoUrl
            } else it["photoUrl"] as String

            val lastRadiusBoost =
                if (it["lastRadiusBoost"] == null) AppModule.user.lastRadiusBoost
                else {
                    val tmpVal = it["lastRadiusBoost"] as Timestamp
                    if (tmpVal.seconds > AppModule.user.lastRadiusBoost.seconds) tmpVal
                    else AppModule.user.lastRadiusBoost
                }

            val balance = if (it["balance"] == null) AppModule.user.balance
            else max(AppModule.user.balance, it["balance"] as Long)

            val experience = if(it["experience"] == null) AppModule.user.experience
            else max(AppModule.user.experience, it["experience"] as Long)

            val level = if(it["level"] == null) AppModule.user.level
            else max(AppModule.user.level, it["level"] as Long)

            val skillPoints = if(it["skillPoints"] == null) AppModule.user.skillPoints
            else max(AppModule.user.skillPoints, it["skillPoints"] as Long)

            val skills = ArrayList<Skill>()
            val skillList = if(it["skills"] == null) ArrayList() else it["skills"] as ArrayList<String>
            skillList.forEach {
                val skill = Skill.values().find { s -> s.name == it }
                if(skill != null) skills.add(skill)
            }

            val upgrades = ArrayList<Upgrade>()
            val upgradeList = if(it["upgrades"] == null) ArrayList() else it["upgrades"] as ArrayList<String>
            upgradeList.forEach {
                val upgrade = Upgrade.values().find { u -> u.name == it }
                if(upgrade != null) upgrades.add(upgrade)
            }

            var visited = HashMap<String, Benchmark>()
            val visitedList = if (it["visited"] == null) ArrayList() else it["visited"] as ArrayList<HashMap<String, Any>>
            visitedList.forEach { x ->
                val pid = x["pid"] as String
                val lastVisited = if(x["lastVisited"] == null) Timestamp(0,0) else x["lastVisited"] as Timestamp
                val notify = if(x["notify"] == null) false else x["notify"] as Boolean

                visited[pid] = Benchmark.new(
                    pid,
                    x["name"] as String,
                    x["location"] as GeoPoint,
                    lastVisited,
                    notify
                )
            }
            if (visited.isNotEmpty() && AppModule.user.visited.isNotEmpty() &&
                visited.values.sortedByDescending { x -> x.lastVisited }[0].lastVisited < AppModule.user.visited.values.sortedByDescending { x -> x.lastVisited }[0].lastVisited
            )
                visited = AppModule.user.visited

            val friends =
                if (it["friends"] == null) ArrayList() else it["friends"] as ArrayList<String>

            return User(
                it.id,
                name,
                photoUrl,
                balance,
                experience,
                level,
                skillPoints,
                lastRadiusBoost,
                skills,
                upgrades,
                visited,
                friends
            )
        }
        fun pullFriend(it: DocumentSnapshot) : User{
            val name = if(it["name"] == null) "" else it["name"] as String
            val photoUrl = if(it["photoUrl"] == null) "" else it["photoUrl"] as String
            val balance = if(it["balance"] == null) 0L else it["balance"] as Long
            val experience = if(it["experience"] == null) 0 else it["experience"] as Long
            val level = if(it["level"] == null) 1 else it["level"] as Long
            val skillPoints = if(it["skillPoints"] == null) 0 else it["skillPoints"] as Long
            val lastRadiusBoost = if(it["lastRadiusBoost"] == null) Timestamp(0,0)
            else it["lastRadiusBoost"] as Timestamp

            val skills = ArrayList<Skill>()
            val skillList = if(it["skills"] == null) ArrayList() else it["skills"] as ArrayList<String>
            skillList.forEach { s ->
                val skill = Skill.values().find { x -> x.name == s }
                if(skill != null) skills.add(skill)
            }

            val upgrades = ArrayList<Upgrade>()
            val upgradeList = if(it["upgrades"] == null) ArrayList() else it["upgrades"] as ArrayList<String>
            upgradeList.forEach {
                val upgrade = Upgrade.values().find { u -> u.name == it }
                if(upgrade != null) upgrades.add(upgrade)
            }

            val visited = HashMap<String, Benchmark>()
            val visitedList = if(it["visited"] == null) ArrayList() else it["visited"] as ArrayList<HashMap<String, Any>>
            visitedList.forEach { x ->
                val pid = x["pid"] as String
                visited[pid] = Benchmark.new(pid,
                    x["name"] as String,
                    x["location"] as GeoPoint,
                    x["lastVisited"] as Timestamp,
                    false
                )
            }

            val friends = if(it["friends"] == null) ArrayList() else it["friends"] as ArrayList<String>

            return User(it.id, name, photoUrl, balance, experience, level, skillPoints, lastRadiusBoost, skills, upgrades, visited, friends)
        }
    }
}
