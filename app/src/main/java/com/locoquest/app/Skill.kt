package com.locoquest.app

enum class Skill(val text: String, val effect: Float, val duration: Int, val reuseIn: Int) {
    DRONE("Drone", .0002f, 30, 3600),
    COMPANION("Companion", 10f, 7200, 7200),
    TIME("Time", 14400f,0,  21600),
    GIANT("Giant", 300f, 300, 600)
}