package com.locoquest.app.dao

import com.locoquest.app.dto.Coin
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ICoinDAO {
    @GET("pid")
    fun getCoinsByPid(@Query("pid") pid: String): Call<List<Coin>>

    @GET("radial")
    fun getCoinsByRadius(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("radius") r: Double): Call<List<Coin>>

    @GET("bounds")
    fun getCoinsByBounds(@Query("minlat") minLat: String, @Query("maxlat") maxLat: String, @Query("minlon") minLon: String, @Query("maxlon") maxLon: String): Call<List<Coin>>
}

