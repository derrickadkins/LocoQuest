package com.locoquest.app

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import com.google.android.gms.maps.GoogleMap

class Prefs(val context: Context) {
    private fun prefs(): SharedPreferences {
        return context.getSharedPreferences("LocoQuest", Context.MODE_PRIVATE)
    }

    fun lastLocation(location: Location){
        with (prefs().edit()) {
            putFloat("lat", location.latitude.toFloat())
            putFloat("lon", location.longitude.toFloat())
            putString("provider", location.provider)
            apply()
        }
    }

    fun lastLocation() : Location {
        val location = Location(prefs().getString("provider", ""))
        location.latitude = prefs().getFloat("lat", 0f).toDouble()
        location.longitude = prefs().getFloat("lon", 0f).toDouble()
        return location
    }

    fun mapType(type: Int){
        with (prefs().edit()) {
            putInt("mapType", type)
            apply()
        }
    }

    fun mapType() : Int {
        return prefs().getInt("mapType", GoogleMap.MAP_TYPE_NORMAL)
    }

    fun uid(uid: String){
        with(prefs().edit()){
            putString("uid", uid)
            apply()
        }
    }

    fun uid() : String {
        return prefs().getString("uid", "").toString()
    }
}