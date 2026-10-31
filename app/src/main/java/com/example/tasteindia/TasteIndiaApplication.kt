package com.example.tasteindia

import android.app.Application
import android.content.Context
import com.example.tasteindia.data.DefaultMealRepository
import com.example.tasteindia.data.FavoritesRepository
import com.example.tasteindia.data.MealRepository
import com.example.tasteindia.data.RoomFavoritesRepository
import com.example.tasteindia.data.local.AppDatabase
import com.example.tasteindia.data.remote.MealDbApi
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency injection: tests build view models with fakes instead of this container. */
class AppContainer(context: Context) {

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val api: MealDbApi = Retrofit.Builder()
        .baseUrl("https://www.themealdb.com/api/json/v1/1/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
        .build()
        .create(MealDbApi::class.java)

    val mealRepository: MealRepository = DefaultMealRepository(api)

    val favouritesRepository: FavoritesRepository =
        RoomFavoritesRepository(AppDatabase.create(context).favoriteDao())
}

class TasteIndiaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
