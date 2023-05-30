package com.locoquest.app

enum class Level(val level: Long, val upperLimit: Long) {
    LEVEL_1(1, 100),
    LEVEL_2(2, 300),
    LEVEL_3(3, 600),
    LEVEL_4(4, 1000),
    LEVEL_5(5, 1500),
    LEVEL_6(6, 2100),
    LEVEL_7(7, 2800),
    LEVEL_8(8, 3600),
    LEVEL_9(9, 4500),
    LEVEL_10(10, 5500),
    LEVEL_11(11, 6600),
    LEVEL_12(12, 7800),
    LEVEL_13(13, 9100),
    LEVEL_14(14, 10500),
    LEVEL_15(15, 12000);

    companion object {
        fun getLimits(levelNumber: Long): Pair<Long, Long> {
            val lowerLimit = if (levelNumber == 1L) 0 else getLimits(levelNumber - 1).second
            val upperLimit = values().find { it.level == levelNumber }?.upperLimit ?: -1
            return Pair(lowerLimit + 1, upperLimit)
        }

        private fun getPointSpread(levelNumber: Long): Long {
            val (lowerLimit, upperLimit) = getLimits(levelNumber)
            return upperLimit - lowerLimit + 1L
        }

        fun getProgress(levelNumber: Long, experiencePoints: Long): Int {
            val (_, upperLimit) = Level.getLimits(levelNumber)
            val pointsToNextLevel = (upperLimit - experiencePoints).toDouble()
            val pointSpread = Level.getPointSpread(levelNumber).toDouble()

            return (((pointSpread - pointsToNextLevel) / pointSpread) * 100).toInt()
        }
    }
}
