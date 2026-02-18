package com.volumecontrol.android.data

import com.google.gson.Gson
import com.volumecontrol.android.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String) : ApiResult<T>()
}

class VolumeApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getVolume(device: Device): ApiResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.host}:${device.port}/volume"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    ApiResult.Error("Failed to get volume: ${response.code}")
                } else {
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val volume = (json["volume"] as? Number)?.toInt()
                    if (volume != null) {
                        ApiResult.Success(volume)
                    } else {
                        ApiResult.Error("Invalid volume response")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApiResult.Error("${e::class.simpleName}: ${e.message ?: e.toString()}")
            }
        }
    }

    suspend fun setVolume(device: Device, volume: Int): ApiResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.host}:${device.port}/volume"
                val json = """{"volume":$volume}"""
                val requestBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    ApiResult.Error("Failed to set volume: ${response.code}")
                } else {
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val resultVolume = (json["volume"] as? Number)?.toInt()
                    if (resultVolume != null) {
                        ApiResult.Success(resultVolume)
                    } else {
                        ApiResult.Error("Invalid response")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApiResult.Error("${e::class.simpleName}: ${e.message ?: e.toString()}")
            }
        }
    }
}
