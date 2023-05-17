package com.locoquest.app

enum class Upgrade(val skill: Skill, name: String, val effect: Int) {
    DRONE_BATT(Skill.DRONE, "Battery", 300),
    DRONE_CHARGE(Skill.DRONE, "Charger", -300),
    DRONE_MOTOR(Skill.DRONE, "Motors", 5),
    COMPANION_BATT(Skill.COMPANION, "Battery", 300),
    COMPANION_CHARGE(Skill.COMPANION, "Charger", -300),
    COMPANION_MOTOR(Skill.COMPANION, "Motors", 5),
    GIANT_BATT(Skill.GIANT, "Battery", 300),
    GIANT_CHARGE(Skill.GIANT, "Charger", -300),
    GIANT_REACH(Skill.GIANT, "Reach", 300),
    TIME_CHARGE(Skill.TIME, "Charge", -300)
}