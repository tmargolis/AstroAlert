package com.toddmargolis.astroalert.data.api

import com.toddmargolis.astroalert.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Astrospheric API
    private val astrosphericRetrofit = Retrofit.Builder()
        .baseUrl("https://astrosphericpublicaccess.azurewebsites.net/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val astrosphericApi: AstrosphericApi = astrosphericRetrofit.create(AstrosphericApi::class.java)
    val astrosphericApiKey: String = BuildConfig.ASTROSPHERIC_API_KEY

    // Telescopius API
    private val telescopiusRetrofit = Retrofit.Builder()
        .baseUrl("https://api.telescopius.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val telescopiusApi: TelescopiusApi = telescopiusRetrofit.create(TelescopiusApi::class.java)
    val telescopiusApiKey: String = "Key ${BuildConfig.TELESCOPIUS_API_KEY}"

    // Sunrise-Sunset API (no auth needed)
    private val sunriseSunsetRetrofit = Retrofit.Builder()
        .baseUrl("https://api.sunrise-sunset.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val sunriseSunsetApi: SunriseSunsetApi = sunriseSunsetRetrofit.create(SunriseSunsetApi::class.java)
}