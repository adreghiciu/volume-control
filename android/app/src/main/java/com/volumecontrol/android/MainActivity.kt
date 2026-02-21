package com.volumecontrol.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volumecontrol.android.data.DeviceDiscovery
import com.volumecontrol.android.data.DeviceRepository
import com.volumecontrol.android.data.VolumeApiClient
import com.volumecontrol.android.ui.AddEditDeviceDialog
import com.volumecontrol.android.ui.DeviceListScreen
import com.volumecontrol.android.ui.DiscoveryScreen
import com.volumecontrol.android.ui.MainViewModel
import com.volumecontrol.android.ui.theme.VolumeControlTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModelInstance: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = DeviceRepository(applicationContext)
        val apiClient = VolumeApiClient()

        setContent {
            VolumeControlTheme {
                val discovery = remember { DeviceDiscovery(applicationContext) }
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(repository, apiClient)
                )
                viewModelInstance = viewModel

                val uiState by viewModel.uiState.collectAsState()

                if (uiState.showDiscoveryScreen) {
                    DiscoveryScreen(
                        discovery = discovery,
                        existingDevices = uiState.devices,
                        onDeviceSelected = { discovered ->
                            viewModel.addDiscoveredDevice(discovered.name, discovered.host, discovered.port)
                        },
                        onBack = { viewModel.closeDiscoveryScreen() }
                    )
                } else {
                    DeviceListScreen(
                        uiState = uiState,
                        onAddDevice = { viewModel.showAddDeviceDialog() },
                        onDiscoverDevices = { viewModel.showDiscoveryScreen() },
                        onEditDevice = { device -> viewModel.showEditDeviceDialog(device) },
                        onDeleteDevice = { id -> viewModel.deleteDevice(id) },
                        onVolumeChange = { device, volume -> viewModel.setVolume(device, volume) },
                        onMuteToggle = { device -> viewModel.toggleMute(device) },
                        onMuteAll = { viewModel.muteAll() },
                        onUnmuteAll = { viewModel.unmuteAll() },
                        onRetryAll = { viewModel.retryAll() },
                        onRemoveAll = { viewModel.removeAllDevices() }
                    )

                    if (uiState.showAddEditDialog) {
                        AddEditDeviceDialog(
                            device = uiState.editingDevice,
                            onDismiss = { viewModel.closeDialog() },
                            onSave = { name, host, port ->
                                val device = uiState.editingDevice
                                if (device == null) {
                                    viewModel.addDevice(name, host, port)
                                } else {
                                    viewModel.editDevice(device, name, host, port)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModelInstance.isInitialized) {
            viewModelInstance.fetchAllVolumes()
        }
    }
}

class MainViewModelFactory(
    private val repository: DeviceRepository,
    private val apiClient: VolumeApiClient
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, apiClient) as T
    }
}
