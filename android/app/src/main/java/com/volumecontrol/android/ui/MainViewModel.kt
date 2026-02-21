package com.volumecontrol.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volumecontrol.android.data.ApiResult
import com.volumecontrol.android.data.DeviceRepository
import com.volumecontrol.android.data.VolumeApiClient
import com.volumecontrol.android.model.Device
import com.volumecontrol.android.model.DeviceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val devices: List<DeviceState> = emptyList(),
    val showAddEditDialog: Boolean = false,
    val editingDevice: Device? = null,   // null = adding new
    val showDiscoveryScreen: Boolean = false
)

class MainViewModel(
    private val repository: DeviceRepository,
    private val apiClient: VolumeApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            val devices = repository.getDevices()
            _uiState.value = _uiState.value.copy(
                devices = devices.map { DeviceState(device = it) }
            )
            // Fetch volumes for all devices
            fetchAllVolumes()
        }
    }

    fun fetchAllVolumes() {
        _uiState.value.devices.forEach { deviceState ->
            viewModelScope.launch {
                updateDevice(deviceState.device.id) { it.copy(isLoading = true, error = null) }
                val result = apiClient.getStatus(deviceState.device)
                updateDevice(deviceState.device.id) {
                    when (result) {
                        is ApiResult.Success -> it.copy(
                            volume = result.data.volume,
                            muted = result.data.muted,
                            isLoading = false
                        )
                        is ApiResult.Error -> it.copy(error = result.message, isLoading = false)
                    }
                }
            }
        }
    }

    fun setVolume(device: Device, volume: Int) {
        viewModelScope.launch {
            // Optimistically update state
            updateDevice(device.id) { it.copy(volume = volume, error = null) }

            val result = apiClient.setVolume(device, volume)
            when (result) {
                is ApiResult.Success -> {
                    // Just clear any errors, trust the optimistic update
                    updateDevice(device.id) { it.copy(error = null) }
                }
                is ApiResult.Error -> {
                    // On error, revert to previous value and show error
                    updateDevice(device.id) { it.copy(error = result.message) }
                    // Fetch the actual volume from device
                    fetchAllVolumes()
                }
            }
        }
    }

    fun showAddDeviceDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingDevice = null
        )
    }

    fun showEditDeviceDialog(device: Device) {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingDevice = device
        )
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = false,
            editingDevice = null
        )
    }

    fun addDevice(name: String, host: String, port: Int) {
        viewModelScope.launch {
            val device = Device(name = name, host = host, port = port)
            repository.addDevice(device)
            _uiState.value = _uiState.value.copy(
                devices = _uiState.value.devices + DeviceState(device = device),
                showAddEditDialog = false
            )
            // Fetch volume for the newly added device
            fetchAllVolumes()
        }
    }

    fun editDevice(device: Device, name: String, host: String, port: Int) {
        viewModelScope.launch {
            val updated = device.copy(name = name, host = host, port = port)
            repository.updateDevice(updated)
            _uiState.value = _uiState.value.copy(
                devices = _uiState.value.devices.map {
                    if (it.device.id == device.id) it.copy(device = updated) else it
                },
                showAddEditDialog = false,
                editingDevice = null
            )
        }
    }

    fun deleteDevice(id: String) {
        viewModelScope.launch {
            repository.deleteDevice(id)
            _uiState.value = _uiState.value.copy(
                devices = _uiState.value.devices.filterNot { it.device.id == id }
            )
        }
    }

    fun toggleMute(device: Device) {
        viewModelScope.launch {
            val currentState = _uiState.value.devices.find { it.device.id == device.id } ?: return@launch
            val newMuted = !currentState.muted

            // Optimistically update state
            updateDevice(device.id) { it.copy(muted = newMuted, error = null) }

            val result = apiClient.setMuted(device, newMuted)
            when (result) {
                is ApiResult.Success -> {
                    updateDevice(device.id) { it.copy(muted = result.data.muted, error = null) }
                }
                is ApiResult.Error -> {
                    updateDevice(device.id) { it.copy(error = result.message) }
                    fetchAllVolumes()
                }
            }
        }
    }

    fun muteAll() {
        _uiState.value.devices.forEach { deviceState ->
            viewModelScope.launch {
                val result = apiClient.setMuted(deviceState.device, true)
                when (result) {
                    is ApiResult.Success -> {
                        updateDevice(deviceState.device.id) {
                            it.copy(muted = true, error = null)
                        }
                    }
                    is ApiResult.Error -> {
                        updateDevice(deviceState.device.id) {
                            it.copy(error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun unmuteAll() {
        _uiState.value.devices.forEach { deviceState ->
            viewModelScope.launch {
                val result = apiClient.setMuted(deviceState.device, false)
                when (result) {
                    is ApiResult.Success -> {
                        updateDevice(deviceState.device.id) {
                            it.copy(muted = false, error = null)
                        }
                    }
                    is ApiResult.Error -> {
                        updateDevice(deviceState.device.id) {
                            it.copy(error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun retryAll() {
        fetchAllVolumes()
    }

    fun removeAllDevices() {
        viewModelScope.launch {
            val devicesToDelete = _uiState.value.devices.map { it.device.id }
            devicesToDelete.forEach { id ->
                repository.deleteDevice(id)
            }
            _uiState.value = _uiState.value.copy(devices = emptyList())
        }
    }

    fun showDiscoveryScreen() {
        _uiState.value = _uiState.value.copy(showDiscoveryScreen = true)
    }

    fun closeDiscoveryScreen() {
        _uiState.value = _uiState.value.copy(showDiscoveryScreen = false)
    }

    fun addDiscoveredDevice(name: String, host: String, port: Int) {
        viewModelScope.launch {
            val device = Device(name = name, host = host, port = port)
            repository.addDevice(device)
            _uiState.value = _uiState.value.copy(
                devices = _uiState.value.devices + DeviceState(device = device),
                showDiscoveryScreen = false
            )
            // Fetch volume for the newly added device
            fetchAllVolumes()
        }
    }

    private fun updateDevice(deviceId: String, updater: (DeviceState) -> DeviceState) {
        _uiState.value = _uiState.value.copy(
            devices = _uiState.value.devices.map {
                if (it.device.id == deviceId) updater(it) else it
            }
        )
    }
}
