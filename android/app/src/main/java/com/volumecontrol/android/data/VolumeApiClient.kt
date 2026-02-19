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

data class VolumeStatus(
    val volume: Int,
    val muted: Boolean
)

class VolumeApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getStatus(device: Device): ApiResult<VolumeStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.host}:${device.port}/"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    ApiResult.Error("Failed to get status: ${response.code}")
                } else {
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val volume = (json["volume"] as? Number)?.toInt()
                    val muted = (json["muted"] as? Boolean) ?: false
                    if (volume != null) {
                        ApiResult.Success(VolumeStatus(volume, muted))
                    } else {
                        ApiResult.Error("Invalid status response")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApiResult.Error("${e::class.simpleName}: ${e.message ?: e.toString()}")
            }
        }
    }

    suspend fun getVolume(device: Device): ApiResult<Int> {
        return when (val result = getStatus(device)) {
            is ApiResult.Success -> ApiResult.Success(result.data.volume)
            is ApiResult.Error -> ApiResult.Error(result.message)
        }
    }

    suspend fun setStatus(device: Device, volume: Int? = null, muted: Boolean? = null): ApiResult<VolumeStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.host}:${device.port}/"
                val jsonParts = mutableListOf<String>()
                if (volume != null) jsonParts.add("\"volume\":$volume")
                if (muted != null) jsonParts.add("\"muted\":$muted")

                val jsonBody = "{${jsonParts.joinToString(",")}}"
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    ApiResult.Error("Failed to set status: ${response.code}")
                } else {
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val resultVolume = (json["volume"] as? Number)?.toInt()
                    val resultMuted = (json["muted"] as? Boolean) ?: false
                    if (resultVolume != null) {
                        ApiResult.Success(VolumeStatus(resultVolume, resultMuted))
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

    suspend fun setVolume(device: Device, volume: Int): ApiResult<Int> {
        return when (val result = setStatus(device, volume = volume)) {
            is ApiResult.Success -> ApiResult.Success(result.data.volume)
            is ApiResult.Error -> ApiResult.Error(result.message)
        }
    }

    suspend fun setMuted(device: Device, muted: Boolean): ApiResult<VolumeStatus> {
        return setStatus(device, muted = muted)
    }
}
