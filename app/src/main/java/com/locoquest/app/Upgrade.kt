package com.locoquest.app

enum class Upgrade(skill: Skill, name: String) {
    DRONE_BATT(Skill.DRONE, "Battery"),
    DRONE_CHARGE(Skill.DRONE, "Charger"),
    DRONE_MOTOR(Skill.DRONE, "Motors"),
    COMPANION_BATT(Skill.COMPANION, "Battery"),
    COMPANION_CHARGE(Skill.COMPANION, "Charger"),
    COMPANION_MOTOR(Skill.COMPANION, "Motors"),
    GIANT_BATT(Skill.GIANT, "Battery"),
    GIANT_CHARGE(Skill.GIANT, "Charger"),
    GIANT_REACH(Skill.GIANT, "Reach"),
    TIME_CHARGE(Skill.TIME, "Charge")
}