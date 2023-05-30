package com.locoquest.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.Timestamp
import com.locoquest.app.AppModule.Companion.cancelNotification
import com.locoquest.app.AppModule.Companion.user
import kotlin.math.pow

class SkillsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skills)
        supportActionBar?.title = "Skills"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val resetCost = findViewById<TextView>(R.id.reset_skill_cost)
        resetCost.text = calcResetCost().toString()

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

        val companionCostLayout = findViewById<LinearLayout>(R.id.companion_cost_layout)
        val droneCostLayout = findViewById<LinearLayout>(R.id.drone_cost_layout)
        val giantCostLayout = findViewById<LinearLayout>(R.id.giant_cost_layout)
        val timeCostLayout = findViewById<LinearLayout>(R.id.time_cost_layout)

        findViewById<TextView>(R.id.companion_cost).text = Skill.COMPANION.cost.toString()
        findViewById<TextView>(R.id.drone_cost).text = Skill.DRONE.cost.toString()
        findViewById<TextView>(R.id.giant_cost).text = Skill.GIANT.cost.toString()
        findViewById<TextView>(R.id.time_cost).text = Skill.TIME.cost.toString()

        companionCostLayout.visibility = if(user.skills.contains(Skill.COMPANION)) View.GONE else View.VISIBLE
        droneCostLayout.visibility = if(user.skills.contains(Skill.DRONE)) View.GONE else View.VISIBLE
        giantCostLayout.visibility = if(user.skills.contains(Skill.GIANT)) View.GONE else View.VISIBLE
        timeCostLayout.visibility = if(user.skills.contains(Skill.TIME)) View.GONE else View.VISIBLE
        
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

        val unlockSkillListener = View.OnClickListener {
            if(user.skillPoints < 1){
                Toast.makeText(this, "You don't have any skill points", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            val (skill, upgrades, costLayout) = when(it.id){
                R.id.unlock_companion -> Triple(Skill.COMPANION, companionUpgrades, companionCostLayout)
                R.id.unlock_drone -> Triple(Skill.DRONE, droneUpgrades, droneCostLayout)
                R.id.unlock_giant -> Triple(Skill.GIANT, giantUpgrades, giantCostLayout)
                R.id.unlock_time -> Triple(Skill.TIME, timeUpgrades, timeCostLayout)
                else -> return@OnClickListener
            }

            if(user.balance < skill.cost){
                Toast.makeText(this, "You don't have enough coins", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            user.balance -= skill.cost
            user.skillPoints--
            user.skills.add(skill)
            user.update()

            it.visibility = View.GONE
            costLayout.visibility = View.GONE
            upgrades.visibility = View.VISIBLE
            skillPts.text = "Skill Points: ${user.skillPoints}"
            resetCost.text = calcResetCost().toString()
        }

        val unlockUpgradeListener = View.OnClickListener {
            if(user.skillPoints < 1){
                Toast.makeText(this, "You don't have any skill points", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            when(it.id){
                R.id.unlock_companion_batt -> {
                    user.upgrades.add(Upgrade.COMPANION_BATT)
                }
                R.id.unlock_companion_charger -> {
                    user.upgrades.add(Upgrade.COMPANION_CHARGE)
                }
                R.id.unlock_companion_motors -> {
                    user.upgrades.add(Upgrade.COMPANION_MOTOR)
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

                R.id.unlock_giant_batt -> {
                    user.upgrades.add(Upgrade.GIANT_BATT)
                }
                R.id.unlock_giant_charger -> {
                    user.upgrades.add(Upgrade.GIANT_CHARGE)
                }
                R.id.unlock_giant_reach -> {
                    user.upgrades.add(Upgrade.GIANT_REACH)
                }

                R.id.unlock_time_charger -> {
                    user.upgrades.add(Upgrade.TIME_CHARGE)
                }
            }
            it.visibility = View.GONE
            user.skillPoints--
            skillPts.text = "Skill Points: ${user.skillPoints}"
            user.update()

            resetCost.text = calcResetCost().toString()
        }

        companion.setOnClickListener(unlockSkillListener)
        drone.setOnClickListener(unlockSkillListener)
        giant.setOnClickListener(unlockSkillListener)
        time.setOnClickListener(unlockSkillListener)

        companionBatt.setOnClickListener(unlockUpgradeListener)
        companionCharge.setOnClickListener(unlockUpgradeListener)
        companionMotor.setOnClickListener(unlockUpgradeListener)

        droneBatt.setOnClickListener(unlockUpgradeListener)
        droneCharge.setOnClickListener(unlockUpgradeListener)
        droneMotor.setOnClickListener(unlockUpgradeListener)

        giantBatt.setOnClickListener(unlockUpgradeListener)
        giantCharge.setOnClickListener(unlockUpgradeListener)
        giantReach.setOnClickListener(unlockUpgradeListener)

        timeCharge.setOnClickListener(unlockUpgradeListener)

        val reset = findViewById<Button>(R.id.reset_skills)
        reset.setOnClickListener{
            if(calcResetCost() > user.balance){
                Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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

            companionCostLayout.visibility = View.VISIBLE
            droneCostLayout.visibility = View.VISIBLE
            giantCostLayout.visibility = View.VISIBLE
            timeCostLayout.visibility = View.VISIBLE

            Skill.values().forEach { cancelNotification(this, it) }
        }
    }

    private fun calcResetCost(): Int {
        //return if (AppModule.DEBUG) 0 else (user.balance * .2).toInt()
        if (AppModule.DEBUG) return 0

        val baseCostPerPoint = 10 // Adjust this value based on your game's balance
        val scalingFactor = 1.5 // Adjust this value as needed
        val skillPointsSpent = user.skills.size + user.upgrades.size
        val costPerPoint =
            (baseCostPerPoint * scalingFactor.pow(skillPointsSpent.toDouble())).toInt()
        return ((user.balance / Level.values().size) * skillPointsSpent).toInt() + costPerPoint * skillPointsSpent
    }
}