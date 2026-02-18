package com.volumecontrol.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.volumecontrol.android.model.Device
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "devices")

class DeviceRepository(private val context: Context) {
    private val gson = Gson()
    private val devicesKey = stringPreferencesKey("devices_list")

    suspend fun getDevices(): List<Device> {
        return try {
            val preferences = context.dataStore.data.first()
            val devicesJson = preferences[devicesKey] ?: return emptyList()
            val type = object : TypeToken<List<Device>>() {}.type
            gson.fromJson(devicesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveDevices(devices: List<Device>) {
        val devicesJson = gson.toJson(devices)
        context.dataStore.edit { preferences ->
            preferences[devicesKey] = devicesJson
        }
    }

    suspend fun addDevice(device: Device) {
        val devices = getDevices().toMutableList()
        devices.add(device)
        saveDevices(devices)
    }

    suspend fun updateDevice(device: Device) {
        val devices = getDevices().toMutableList()
        val index = devices.indexOfFirst { it.id == device.id }
        if (index >= 0) {
            devices[index] = device
            saveDevices(devices)
        }
    }

    suspend fun deleteDevice(id: String) {
        val devices = getDevices().toMutableList()
        devices.removeAll { it.id == id }
        saveDevices(devices)
    }
}
