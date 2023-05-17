package com.locoquest.app

enum class Skill(val id: Int, val text: String, val duration: Int, val reuseIn: Int) {
    DRONE(0, "Drone", 300, 300),
    COMPANION(1, "Companion", 300, 300),
    TIME(2, "Time", 0, 300),
    GIANT(3, "Giant", 300, 300)
}