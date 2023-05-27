package com.locoquest.app

enum class Skill(val text: String, val effect: Float, val duration: Int, val reuseIn: Int, val cost: Int) {
    DRONE("Drone", .0002f, 30, 3600, 10000),
    COMPANION("Companion", 10f, 7200, 7200, 1000),
    TIME("Time", 14400f,0,  21600, 3000),
    GIANT("Giant", 300f, 300, 600, 6000)
}