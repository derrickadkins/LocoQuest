package com.locoquest.app

enum class Upgrade(val skill: Skill, name: String, val effect: Int) {
    DRONE_BATT(Skill.DRONE, "Battery", 30),
    DRONE_CHARGE(Skill.DRONE, "Charge", -1800),
    DRONE_MOTOR(Skill.DRONE, "Motors", 5),
    COMPANION_BATT(Skill.COMPANION, "Battery", 300), //todo - remove?
    COMPANION_CHARGE(Skill.COMPANION, "Charge", -1800),
    COMPANION_MOTOR(Skill.COMPANION, "Motors", 5),
    GIANT_BATT(Skill.GIANT, "Battery", 300),
    GIANT_CHARGE(Skill.GIANT, "Charge", -300),
    GIANT_REACH(Skill.GIANT, "Reach", 100),
    TIME_CHARGE(Skill.TIME, "Charge", -7200)
}