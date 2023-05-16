package com.locoquest.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat

class SkillsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skills)
        supportActionBar?.title = "Skills"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val companionUpgrades = findViewById<LinearLayoutCompat>(R.id.companion_upgrades_layout)
        val droneUpgrades = findViewById<LinearLayoutCompat>(R.id.drone_upgrades_layout)
        val giantUpgrades = findViewById<LinearLayoutCompat>(R.id.giant_upgrades_layout)
        val timeUpgrades = findViewById<LinearLayoutCompat>(R.id.time_upgrade_layout)

        val unlockClickListener = View.OnClickListener {
            when(it.id){
                R.id.unlock_companion -> {

                }
                R.id.unlock_companion_batt -> {

                }
                R.id.unlock_companion_charger -> {

                }
                R.id.unlock_companion_motors -> {

                }

                R.id.unlock_drone -> {

                }
                R.id.unlock_drone_batt -> {

                }
                R.id.unlock_drone_charger -> {

                }
                R.id.unlock_drone_motors -> {

                }

                R.id.unlock_giant -> {

                }
                R.id.unlock_giant_batt -> {

                }
                R.id.unlock_giant_charger -> {

                }
                R.id.unlock_giant_reach -> {

                }

                R.id.unlock_time -> {

                }
                R.id.unlock_time_charger -> {

                }
            }
        }
    }
}