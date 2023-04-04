import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.locoquest.app.R
import com.locoquest.app.RetrofitClientInstance
import com.locoquest.app.dao.IBenchmarkDAO
import com.locoquest.app.dto.Benchmark
import com.squareup.moshi.Moshi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

interface IBenchmarkService {
    fun getBenchmarkData(pidList: List<String>): List<Benchmark>?
    fun parseBenchmarkData(benchmarkJson: String): Any?
}

open class BenchmarkService : IBenchmarkService {

    override fun getBenchmarkData(pidList: List<String>): List<Benchmark>? {
        val benchmarkDAO = RetrofitClientInstance.retrofitInstance?.create(IBenchmarkDAO::class.java)
        val call = benchmarkDAO?.getBenchmarkByPid(pidList.joinToString(","))
        val response = call?.execute()

        return if (response?.isSuccessful == true && response.body() != null) {
            response.body()!!
        } else {
            null
        }
    }

    override fun parseBenchmarkData(benchmarkJson: String): Benchmark? {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(Benchmark::class.java)
        return adapter.fromJson(benchmarkJson)
    }

    fun main() {
        val benchmarkService: IBenchmarkService = BenchmarkService()
        val pidList = listOf("AB1234", "CD5678")
        val benchmarkList = benchmarkService.getBenchmarkData(pidList)
        if (benchmarkList != null) {
            benchmarkList.forEach {
                println(it)
            }
        } else {
            println("Error: unable to retrieve benchmark data")
        }
    }
}