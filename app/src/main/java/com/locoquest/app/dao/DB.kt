package com.locoquest.app.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.locoquest.app.Converters
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User

@Database(entities = [Benchmark::class, User::class], version = 2)
@TypeConverters(Converters::class)
abstract class BenchmarkDatabase : RoomDatabase() {
    abstract fun localBenchmarkDAO() : ILocalBenchmarkDAO
    abstract fun localUserDAO() : ILocalUserDAO
}