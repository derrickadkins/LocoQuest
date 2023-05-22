package com.locoquest.app

enum class Upgrade(val skill: Skill, name: String, val effect: Float) {
    DRONE_BATT(Skill.DRONE, "Battery", 30f),
    DRONE_CHARGE(Skill.DRONE, "Charge", -1800f),
    DRONE_MOTOR(Skill.DRONE, "Motors", .0001f),
    COMPANION_BATT(Skill.COMPANION, "Battery", 300f), //todo - remove?
    COMPANION_CHARGE(Skill.COMPANION, "Charge", -1800f),
    COMPANION_MOTOR(Skill.COMPANION, "Motors", 5f),
    GIANT_BATT(Skill.GIANT, "Battery", 300f),
    GIANT_CHARGE(Skill.GIANT, "Charge", -300f),
    GIANT_REACH(Skill.GIANT, "Reach", 100f),
    TIME_CHARGE(Skill.TIME, "Charge", -7200f)
}