package com.example.network_logger_lib

import android.app.Application
import com.example.network_logger_lib.api.DemoApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DemoApplication : Application() {

    lateinit var demoApi: DemoApi
        private set

    override fun onCreate() {
        super.onCreate()

        NetworkLogger.init(this)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(NetworkLogger.interceptor())
            .build()

        demoApi = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DemoApi::class.java)
    }

    companion object {
        fun get(application: Application): DemoApplication {
            return application as DemoApplication
        }
    }
}
