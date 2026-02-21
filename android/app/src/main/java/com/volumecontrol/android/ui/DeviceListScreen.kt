package com.volumecontrol.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.volumecontrol.android.R
import com.volumecontrol.android.model.Device

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    uiState: MainUiState,
    onAddDevice: () -> Unit,
    onDiscoverDevices: () -> Unit,
    onEditDevice: (Device) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onVolumeChange: (Device, Int) -> Unit,
    onMuteToggle: (Device) -> Unit,
    onMuteAll: () -> Unit,
    onUnmuteAll: () -> Unit,
    onRetryAll: () -> Unit,
    onRemoveAll: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRemoveAllDialog by remember { mutableStateOf(false) }

    // Determine if all devices are muted
    val allDevicesMuted = uiState.devices.isNotEmpty() && uiState.devices.all { it.muted }

    if (showRemoveAllDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveAllDialog = false },
            title = { Text(stringResource(R.string.remove_all)) },
            text = { Text(stringResource(R.string.remove_all_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveAllDialog = false
                        onRemoveAll()
                    }
                ) {
                    Text(stringResource(R.string.remove_all))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRemoveAllDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volume Control") },
                actions = {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.retry_all)) },
                            onClick = {
                                showMenu = false
                                onRetryAll()
                            }
                        )
                        if (allDevicesMuted) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.unmute_all)) },
                                onClick = {
                                    showMenu = false
                                    onUnmuteAll()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mute_all)) },
                                onClick = {
                                    showMenu = false
                                    onMuteAll()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.discover_devices)) },
                            onClick = {
                                showMenu = false
                                onDiscoverDevices()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_device)) },
                            onClick = {
                                showMenu = false
                                onAddDevice()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_all)) },
                            onClick = {
                                showMenu = false
                                showRemoveAllDialog = true
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDevice
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(uiState.devices) { deviceState ->
                DeviceCard(
                    deviceState = deviceState,
                    onVolumeChange = { volume ->
                        onVolumeChange(deviceState.device, volume)
                    },
                    onMuteToggle = {
                        onMuteToggle(deviceState.device)
                    },
                    onEdit = {
                        onEditDevice(deviceState.device)
                    },
                    onDelete = {
                        onDeleteDevice(deviceState.device.id)
                    }
                )
            }
        }
    }
}
