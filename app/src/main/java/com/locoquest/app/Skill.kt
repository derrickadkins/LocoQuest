package com.locoquest.app

enum class Skill(val text: String, val effect: Int, val duration: Int, val reuseIn: Int) {
    DRONE("Drone", 10, 30, 3600),
    COMPANION("Companion", 10, 7200, 7200),
    TIME("Time", 14400,0,  21600),
    GIANT("Giant", 300, 300, 600)
}