package com.volumecontrol.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
    onEditDevice: (Device) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onVolumeChange: (Device, Int) -> Unit,
    onRetryAll: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_device)) },
                            onClick = {
                                showMenu = false
                                onAddDevice()
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
