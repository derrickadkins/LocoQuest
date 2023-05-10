package com.locoquest.app

enum class Level(val level: Int, val upperLimit: Int) {
    LEVEL_1(1, 100),
    LEVEL_2(2, 200),
    LEVEL_3(3, 300),
    LEVEL_4(4, 400),
    LEVEL_5(5, 500),
    LEVEL_6(6, 600),
    LEVEL_7(7, 700),
    LEVEL_8(8, 800),
    LEVEL_9(9, 900),
    LEVEL_10(10, 1000);

    companion object {
        fun getLimits(levelNumber: Int): Pair<Int, Int> {
            val lowerLimit = if (levelNumber == 1) 0 else getLimits(levelNumber - 1).second
            val upperLimit = values().find { it.level == levelNumber }?.upperLimit ?: -1
            return Pair(lowerLimit + 1, upperLimit)
        }

        fun getPointSpread(levelNumber: Int): Int {
            val (lowerLimit, upperLimit) = getLimits(levelNumber)
            return upperLimit - lowerLimit + 1
        }

        fun getProgress(levelNumber: Int, experiencePoints: Int): Int {
            val (_, upperLimit) = Level.getLimits(levelNumber)
            val pointsToNextLevel = upperLimit - experiencePoints
            val pointSpread = Level.getPointSpread(levelNumber)

            return ((pointSpread - pointsToNextLevel) / pointSpread) * 100
        }
    }
}
