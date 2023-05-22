package com.locoquest.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.Timestamp
import com.locoquest.app.AppModule.Companion.cancelNotification
import com.locoquest.app.AppModule.Companion.user

class SkillsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skills)
        supportActionBar?.title = "Skills"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val skillPts = findViewById<TextView>(R.id.skill_pts)
        skillPts.text = "Skill Points: ${user.skillPoints}"

        val companionUpgrades = findViewById<LinearLayoutCompat>(R.id.companion_upgrades_layout)
        val droneUpgrades = findViewById<LinearLayoutCompat>(R.id.drone_upgrades_layout)
        val giantUpgrades = findViewById<LinearLayoutCompat>(R.id.giant_upgrades_layout)
        val timeUpgrades = findViewById<ConstraintLayout>(R.id.time_upgrade_layout)
        
        companionUpgrades.visibility = if(user.skills.contains(Skill.COMPANION)) View.VISIBLE else View.GONE
        droneUpgrades.visibility = if(user.skills.contains(Skill.DRONE)) View.VISIBLE else View.GONE
        giantUpgrades.visibility = if(user.skills.contains(Skill.GIANT)) View.VISIBLE else View.GONE
        timeUpgrades.visibility = if(user.skills.contains(Skill.TIME)) View.VISIBLE else View.GONE
        
        val companion = findViewById<Button>(R.id.unlock_companion)
        val drone = findViewById<Button>(R.id.unlock_drone)
        val giant = findViewById<Button>(R.id.unlock_giant)
        val time = findViewById<Button>(R.id.unlock_time)
        
        val companionBatt = findViewById<Button>(R.id.unlock_companion_batt)
        val companionCharge = findViewById<Button>(R.id.unlock_companion_charger)
        val companionMotor = findViewById<Button>(R.id.unlock_companion_motors)
        
        val droneBatt = findViewById<Button>(R.id.unlock_drone_batt)
        val droneCharge = findViewById<Button>(R.id.unlock_drone_charger)
        val droneMotor = findViewById<Button>(R.id.unlock_drone_motors)
        
        val giantBatt = findViewById<Button>(R.id.unlock_giant_batt)
        val giantCharge = findViewById<Button>(R.id.unlock_giant_charger)
        val giantReach = findViewById<Button>(R.id.unlock_giant_reach)
        
        val timeCharge = findViewById<Button>(R.id.unlock_time_charger)
        
        companion.visibility = if(user.skills.contains(Skill.COMPANION)) View.GONE else View.VISIBLE
        drone.visibility = if(user.skills.contains(Skill.DRONE)) View.GONE else View.VISIBLE
        giant.visibility = if(user.skills.contains(Skill.GIANT)) View.GONE else View.VISIBLE
        time.visibility = if(user.skills.contains(Skill.TIME)) View.GONE else View.VISIBLE
        
        companionBatt.visibility = if(user.upgrades.contains(Upgrade.COMPANION_BATT)) View.GONE else View.VISIBLE
        companionCharge.visibility = if(user.upgrades.contains(Upgrade.COMPANION_CHARGE)) View.GONE else View.VISIBLE
        companionMotor.visibility = if(user.upgrades.contains(Upgrade.COMPANION_MOTOR)) View.GONE else View.VISIBLE

        droneBatt.visibility = if(user.upgrades.contains(Upgrade.DRONE_BATT)) View.GONE else View.VISIBLE
        droneCharge.visibility = if(user.upgrades.contains(Upgrade.DRONE_CHARGE)) View.GONE else View.VISIBLE
        droneMotor.visibility = if(user.upgrades.contains(Upgrade.DRONE_MOTOR)) View.GONE else View.VISIBLE

        giantBatt.visibility = if(user.upgrades.contains(Upgrade.GIANT_BATT)) View.GONE else View.VISIBLE
        giantCharge.visibility = if(user.upgrades.contains(Upgrade.GIANT_CHARGE)) View.GONE else View.VISIBLE
        giantReach.visibility = if(user.upgrades.contains(Upgrade.GIANT_REACH)) View.GONE else View.VISIBLE
        
        timeCharge.visibility = if(user.upgrades.contains(Upgrade.TIME_CHARGE)) View.GONE else View.VISIBLE

        val unlockListener = View.OnClickListener {
            if(user.skillPoints < 1){
                Toast.makeText(this, "You don't have any skill points", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            when(it.id){
                R.id.unlock_companion -> {
                    user.skills.add(Skill.COMPANION)
                    companionUpgrades.visibility = View.VISIBLE
                }
                R.id.unlock_companion_batt -> {
                    user.upgrades.add(Upgrade.COMPANION_BATT)
                }
                R.id.unlock_companion_charger -> {
                    user.upgrades.add(Upgrade.COMPANION_CHARGE)
                }
                R.id.unlock_companion_motors -> {
                    user.upgrades.add(Upgrade.COMPANION_MOTOR)
                }

                R.id.unlock_drone -> {
                    user.skills.add(Skill.DRONE)
                    droneUpgrades.visibility = View.VISIBLE
                }
                R.id.unlock_drone_batt -> {
                    user.upgrades.add(Upgrade.DRONE_BATT)
                }
                R.id.unlock_drone_charger -> {
                    user.upgrades.add(Upgrade.DRONE_CHARGE)
                }
                R.id.unlock_drone_motors -> {
                    user.upgrades.add(Upgrade.DRONE_MOTOR)
                }

                R.id.unlock_giant -> {
                    user.skills.add(Skill.GIANT)
                    giantUpgrades.visibility = View.VISIBLE
                }
                R.id.unlock_giant_batt -> {
                    user.upgrades.add(Upgrade.GIANT_BATT)
                }
                R.id.unlock_giant_charger -> {
                    user.upgrades.add(Upgrade.GIANT_CHARGE)
                }
                R.id.unlock_giant_reach -> {
                    user.upgrades.add(Upgrade.GIANT_REACH)
                }

                R.id.unlock_time -> {
                    user.skills.add(Skill.TIME)
                    timeUpgrades.visibility = View.VISIBLE
                }
                R.id.unlock_time_charger -> {
                    user.upgrades.add(Upgrade.TIME_CHARGE)
                }
            }
            it.visibility = View.GONE
            user.skillPoints--
            skillPts.text = "Skill Points: ${user.skillPoints}"
            user.update()
        }

        companion.setOnClickListener(unlockListener)
        drone.setOnClickListener(unlockListener)
        giant.setOnClickListener(unlockListener)
        time.setOnClickListener(unlockListener)

        companionBatt.setOnClickListener(unlockListener)
        companionCharge.setOnClickListener(unlockListener)
        companionMotor.setOnClickListener(unlockListener)

        droneBatt.setOnClickListener(unlockListener)
        droneCharge.setOnClickListener(unlockListener)
        droneMotor.setOnClickListener(unlockListener)

        giantBatt.setOnClickListener(unlockListener)
        giantCharge.setOnClickListener(unlockListener)
        giantReach.setOnClickListener(unlockListener)

        timeCharge.setOnClickListener(unlockListener)

        fun calcResetCost() : Int {
            return if (AppModule.DEBUG) 0 else (user.balance * .2).toInt()
        }

        val resetCost = findViewById<TextView>(R.id.reset_skill_cost)
        resetCost.text = calcResetCost().toString()

        val reset = findViewById<Button>(R.id.reset_skills)
        reset.setOnClickListener{
            user.lastUsedGiant = Timestamp(0,0)
            user.lastUsedCompanion = Timestamp(0,0)
            user.lastUsedDrone = Timestamp(0,0)
            user.lastUsedTimeTravel = Timestamp(0,0)
            user.skillPoints += user.skills.size + user.upgrades.size
            user.balance -= calcResetCost()
            user.skills.clear()
            user.upgrades.clear()
            user.update()

            skillPts.text = "Skill Points: ${user.skillPoints}"
            resetCost.text = calcResetCost().toString()

            companionUpgrades.visibility = View.GONE
            droneUpgrades.visibility = View.GONE
            giantUpgrades.visibility = View.GONE
            timeUpgrades.visibility = View.GONE

            companionBatt.visibility = View.VISIBLE
            companionCharge.visibility = View.VISIBLE
            companionMotor.visibility = View.VISIBLE
            droneBatt.visibility = View.VISIBLE
            droneCharge.visibility = View.VISIBLE
            droneMotor.visibility = View.VISIBLE
            giantBatt.visibility = View.VISIBLE
            giantCharge.visibility = View.VISIBLE
            giantReach.visibility = View.VISIBLE
            timeCharge.visibility = View.VISIBLE

            companion.visibility = View.VISIBLE
            drone.visibility = View.VISIBLE
            giant.visibility = View.VISIBLE
            time.visibility = View.VISIBLE

            Skill.values().forEach { cancelNotification(this, it) }
        }
    }
}