package com.locoquest.app

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.locoquest.app.dto.Coin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromArrayListString(value: ArrayList<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toArrayListString(value: String): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromCoins(value: HashMap<String, Coin>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCoins(value: String): HashMap<String, Coin> {
        val listType = object : TypeToken<HashMap<String, Coin>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toTimestamp(s: String): Timestamp{
        val p = s.split(',')
        return Timestamp(p[0].toLong(), p[1].toInt())
    }

    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp): String{
        return "${timestamp.seconds},${timestamp.nanoseconds}"
    }

    @TypeConverter
    fun fromSkills(skills: ArrayList<Skill>): String{
        return gson.toJson(skills)
    }

    @TypeConverter
    fun toSkills(json: String): ArrayList<Skill>{
        val type = object : TypeToken<ArrayList<Skill>>(){}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromUpgrades(upgrades: ArrayList<Upgrade>): String{
        return gson.toJson(upgrades)
    }

    @TypeConverter
    fun toUpgrades(json: String): ArrayList<Upgrade>{
        val type = object : TypeToken<ArrayList<Upgrade>>(){}.type
        return gson.fromJson(json, type)
    }

    companion object {
        fun toMarkerOptions(coin: Coin): MarkerOptions {
            return MarkerOptions()
                .position(
                    LatLng(
                        coin.lat,
                        coin.lon
                    )
                )
                .title(coin.name)
        }

        fun formatSeconds(seconds: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = Date(seconds * 1000)
            return sdf.format(date)
        }

        fun toCountdownFormat(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secondsRemaining = seconds % 60

            return if(hours > 0) String.format("%02d:%02d:%02d", hours, minutes, secondsRemaining)
            else if(minutes > 0) String.format("%02d:%02d", minutes, secondsRemaining)
            else String.format("%02d", secondsRemaining)
        }
    }
}