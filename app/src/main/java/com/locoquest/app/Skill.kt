package com.locoquest.app

enum class Skill(val text: String, val effect: Int, val duration: Int, val reuseIn: Int) {
    DRONE("Drone", 0, 300, 300),
    COMPANION("Companion", 10, 300, 300),
    TIME("Time", 14400,0,  300),
    GIANT("Giant", 300, 300, 300)
}