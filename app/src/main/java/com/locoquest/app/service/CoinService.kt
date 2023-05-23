import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.locoquest.app.RetrofitClientInstance
import com.locoquest.app.dao.ICoinDAO
import com.locoquest.app.dto.Coin
import java.net.SocketTimeoutException

interface ICoinService {
    fun getCoins(bounds: LatLngBounds): List<Coin>?
    fun getCoins(latLng: LatLng, r: Double): List<Coin>?
    fun getCoins(pidList: List<String>): List<Coin>?
}

open class CoinService : ICoinService {
    override fun getCoins(bounds: LatLngBounds): List<Coin>? {
        try {
            val coinDAO =
                RetrofitClientInstance.retrofitInstance?.create(ICoinDAO::class.java)
            val call = coinDAO?.getCoinsByBounds(
                bounds.southwest.latitude.toString(),
                bounds.northeast.latitude.toString(),
                bounds.southwest.longitude.toString(),
                bounds.northeast.longitude.toString()
            )
            val response = call?.execute()

            return if (response?.isSuccessful == true && response.body() != null) {
                response.body()!!
            } else {
                null
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, e.toString())
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, e.toString())
        }

        return null
    }

    override fun getCoins(latLng: LatLng, r: Double): List<Coin>? {
        val coinDAO = RetrofitClientInstance.retrofitInstance?.create(ICoinDAO::class.java)
        val call = coinDAO?.getCoinsByRadius(latLng.latitude, latLng.longitude, r)
        val response = call?.execute()

        return if (response?.isSuccessful == true && response.body() != null) {
            response.body()!!
        }else{
            null
        }
    }
    override fun getCoins(pidList: List<String>): List<Coin>? {
        val coinDAO = RetrofitClientInstance.retrofitInstance?.create(ICoinDAO::class.java)
        val call = coinDAO?.getCoinsByPid(pidList.joinToString(","))
        val response = call?.execute()

        return if (response?.isSuccessful == true && response.body() != null) {
            response.body()!!
        } else {
            null
        }
    }

    companion object{
        private const val TAG = "CoinService"
    }
}