package com.locoquest.app.dto

import android.content.Context
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.locoquest.app.AppModule
import com.locoquest.app.AppModule.Companion.DEFAULT_REACH
import com.locoquest.app.AppModule.Companion.db
import com.locoquest.app.AppModule.Companion.guest
import com.locoquest.app.Converters
import com.locoquest.app.Prefs
import com.locoquest.app.Skill
import com.locoquest.app.Upgrade
import com.locoquest.app.dao.DB
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
    var lastUsedGiant: Timestamp = Timestamp(0,0),
    var lastUsedDrone: Timestamp = Timestamp(0,0),
    var lastUsedCompanion: Timestamp = Timestamp(0,0),
    var lastUsedTimeTravel: Timestamp = Timestamp(0,0),
    val skills: ArrayList<Skill> = ArrayList(),
    val upgrades: ArrayList<Upgrade> = ArrayList(),
    val visited: HashMap<String, Benchmark> = HashMap(),
    val friends: ArrayList<String> = ArrayList(),
){

    fun update(){
        Thread{ db?.localUserDAO()?.update(this) }.start()
        if(uid != guest.uid) push()
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
                "lastUsedGiant" to lastUsedGiant,
                "lastUsedCompanion" to lastUsedCompanion,
                "lastUsedDrone" to lastUsedDrone,
                "lastUsedTimeTravel" to lastUsedTimeTravel,
                "skills" to skillList.toList(),
                "upgrades" to upgradeList.toList(),
                "visited" to visitedList.toList(),
                "friends" to friends.toList()
            ))
    }

    fun lastUsedSkill(skill: Skill): Long{
        return when(skill){
            Skill.COMPANION -> lastUsedCompanion.seconds
            Skill.TIME -> lastUsedTimeTravel.seconds
            Skill.DRONE -> lastUsedDrone.seconds
            Skill.GIANT -> lastUsedGiant.seconds
        }
    }

    fun isSkillAvailable(skill: Skill): Pair<Boolean, Long>{
        if(!skills.contains(skill)) {
            Log.d(TAG, "${skill.name} is not available because user does not have it")
            return Pair(false, 0)
        }
        var reuseIn = skill.reuseIn
        val upgrade = when(skill){
            Skill.GIANT -> Upgrade.GIANT_CHARGE
            Skill.TIME -> Upgrade.TIME_CHARGE
            Skill.COMPANION -> Upgrade.COMPANION_CHARGE
            Skill.DRONE -> Upgrade.DRONE_CHARGE
        }
        if(upgrades.contains(upgrade)) reuseIn += upgrade.effect
        val availableIn = lastUsedSkill(skill) + skill.duration + reuseIn - Timestamp.now().seconds
        val isAvailable = availableIn < 0
        Log.d(TAG, "${skill.name} available:$isAvailable, available in ${Converters.toCountdownFormat(availableIn)}")
        return Pair(isAvailable, availableIn)
    }

    fun isSkillInUse(skill: Skill): Pair<Boolean, Long>{
        if(!skills.contains(skill)) {
            Log.d(TAG, "${skill.name} is not available because user does not have it")
            return Pair(false, 0)
        }
        var duration = skill.duration
        val upgrade = when(skill){
            Skill.GIANT -> Upgrade.GIANT_BATT
            Skill.DRONE -> Upgrade.DRONE_BATT
            Skill.COMPANION -> Upgrade.COMPANION_BATT
            Skill.TIME -> null
        }
        upgrade?.let { if(upgrades.contains(it)) duration += it.effect }
        val lastUsed = lastUsedSkill(skill)
        val now = Timestamp.now().seconds
        val inUse = now - lastUsed < duration
        val reuseIn = lastUsed + duration - now
        Log.d(TAG, "${skill.name} in use ($now - $lastUsed < $duration):$inUse, finished in:${Converters.toCountdownFormat(reuseIn)}")
        return Pair(inUse, reuseIn)
    }

    fun getReach(): Int {
        return if (isSkillInUse(Skill.GIANT).first) {
            var reach = Skill.GIANT.effect
            if(upgrades.contains(Upgrade.GIANT_REACH)) reach += Upgrade.GIANT_REACH.effect
            reach
        }else DEFAULT_REACH
    }

    fun dump() : String{
        return Gson().toJson(this)
    }

    override fun toString(): String {
        return displayName
    }

    companion object{
        private const val TAG = "User"
        fun load(context: Context, callback: () -> Unit){
            if(AppModule.user.uid == Prefs(context).uid()){
                callback()
                return
            }
            Thread{
                db = Room.databaseBuilder(context, DB::class.java, "db")
                    .fallbackToDestructiveMigration().build()
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null)
                    AppModule.user = User(currentUser.uid)
                Log.d("user", "Database loaded, switching to user:${AppModule.user.uid}")

                val userDao = db!!.localUserDAO()
                val tmpUser = userDao.getUser(AppModule.user.uid)

                if (tmpUser == null) {
                    userDao.insert(AppModule.user)
                } else AppModule.user = tmpUser

                Log.d("user", "user loaded from db; ${AppModule.user.dump()}")

                if (AppModule.user.uid == AppModule.guest.uid) {
                    return@Thread
                }

                Firebase.firestore.collection("users").document(AppModule.user.uid)
                    .get()
                    .addOnSuccessListener {
                        if (it.data == null) {
                            AppModule.user.push()
                            return@addOnSuccessListener
                        }

                        AppModule.user = pullUser(it)
                    }
                    .addOnFailureListener {
                        AppModule.user.push()
                    }.addOnCompleteListener {
                        callback()
                        //todo - update user here?
                    }
            }.start()
        }

        fun pullUser(it: DocumentSnapshot) : User{
            val name =
                if (it["name"] == null) AppModule.user.displayName else it["name"] as String

            val photoUrl = if (it["photoUrl"] == null) {
                AppModule.user.photoUrl = Firebase.auth.currentUser?.photoUrl.toString()
                AppModule.user.photoUrl
            } else it["photoUrl"] as String

            val lastUsedCompanion =
                if (it["lastUsedCompanion"] == null) AppModule.user.lastUsedCompanion
                else {
                    val tmpVal = it["lastUsedCompanion"] as Timestamp
                    if (tmpVal.seconds > AppModule.user.lastUsedCompanion.seconds) tmpVal
                    else AppModule.user.lastUsedCompanion
                }

            val lastUsedDrone =
                if (it["lastUsedDrone"] == null) AppModule.user.lastUsedDrone
                else {
                    val tmpVal = it["lastUsedDrone"] as Timestamp
                    if (tmpVal.seconds > AppModule.user.lastUsedDrone.seconds) tmpVal
                    else AppModule.user.lastUsedDrone
                }

            val lastUsedGiant =
                if (it["lastUsedGiant"] == null) AppModule.user.lastUsedGiant
                else {
                    val tmpVal = it["lastUsedGiant"] as Timestamp
                    if (tmpVal.seconds > AppModule.user.lastUsedGiant.seconds) tmpVal
                    else AppModule.user.lastUsedGiant
                }

            val lastUsedTimeTravel =
                if (it["lastUsedTimeTravel"] == null) AppModule.user.lastUsedTimeTravel
                else {
                    val tmpVal = it["lastUsedTimeTravel"] as Timestamp
                    if (tmpVal.seconds > AppModule.user.lastUsedTimeTravel.seconds) tmpVal
                    else AppModule.user.lastUsedTimeTravel
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
                lastUsedGiant,
                lastUsedDrone,
                lastUsedCompanion,
                lastUsedTimeTravel,
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

            return User(it.id,
                name,
                photoUrl,
                balance,
                experience,
                level,
                0,
                Timestamp(0,0),
                Timestamp(0,0),
                Timestamp(0,0),
                Timestamp(0,0),
                skills,
                upgrades,
                visited,
                friends)
        }
    }
}
