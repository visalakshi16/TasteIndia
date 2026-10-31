package com.example.tasteindia.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/** TheMealDB V1, test key "1". Base URL: https://www.themealdb.com/api/json/v1/1/ */
interface MealDbApi {
    @GET("filter.php")
    suspend fun filterByArea(@Query("a") area: String): MealSummaryResponse

    @GET("filter.php")
    suspend fun filterByCategory(@Query("c") category: String): MealSummaryResponse

    @GET("filter.php")
    suspend fun filterByIngredient(@Query("i") ingredient: String): MealSummaryResponse

    @GET("lookup.php")
    suspend fun lookupMeal(@Query("i") id: String): MealLookupResponse

    @GET("list.php?c=list")
    suspend fun listCategories(): CategoryListResponse

    @GET("list.php?i=list")
    suspend fun listIngredients(): IngredientListResponse
}

@JsonClass(generateAdapter = true)
data class MealSummaryDto(
    @Json(name = "idMeal") val id: String,
    @Json(name = "strMeal") val name: String?,
    @Json(name = "strMealThumb") val thumbnail: String?,
)

@JsonClass(generateAdapter = true)
data class MealSummaryResponse(val meals: List<MealSummaryDto>?)

/**
 * lookup.php returns ~50 flat string fields (strIngredient1..20, strMeasure1..20, ...).
 * Parsing each record as a string map avoids 40 near-identical properties; all field
 * access and normalisation happens in one place: Mappers.kt.
 */
@JsonClass(generateAdapter = true)
data class MealLookupResponse(val meals: List<Map<String, String?>>?)

@JsonClass(generateAdapter = true)
data class CategoryDto(@Json(name = "strCategory") val name: String?)

@JsonClass(generateAdapter = true)
data class CategoryListResponse(val meals: List<CategoryDto>?)

@JsonClass(generateAdapter = true)
data class IngredientDto(@Json(name = "strIngredient") val name: String?)

@JsonClass(generateAdapter = true)
data class IngredientListResponse(val meals: List<IngredientDto>?)
