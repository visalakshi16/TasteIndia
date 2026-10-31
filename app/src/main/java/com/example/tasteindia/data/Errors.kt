package com.example.tasteindia.data

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

class MealNotFoundException(val mealId: String) : Exception("Meal $mealId was not found")

/** Like runCatching, but never swallows coroutine cancellation. */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/** Human-readable message for every failure the UI can show. */
fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException, is ConnectException ->
        "You appear to be offline. Check your connection and try again."
    is InterruptedIOException ->
        "The request timed out. Please try again."
    is HttpException -> when (code()) {
        404 -> "That item could not be found."
        429 -> "Too many requests. Please wait a moment and try again."
        in 500..599 -> "TheMealDB is having trouble (HTTP ${code()}). Please try again later."
        else -> "The request failed (HTTP ${code()})."
    }
    is JsonDataException, is JsonEncodingException ->
        "TheMealDB returned data the app couldn't read."
    is MealNotFoundException -> "That recipe could not be found."
    is IOException -> "Network problem. Check your connection and try again."
    else -> "Something went wrong. Please try again."
}
