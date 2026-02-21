package com.volumecontrol.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.volumecontrol.android.R
import com.volumecontrol.android.data.DeviceDiscovery
import com.volumecontrol.android.data.DiscoveredDevice
import com.volumecontrol.android.model.DeviceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    discovery: DeviceDiscovery,
    existingDevices: List<DeviceState> = emptyList(),
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onBack: () -> Unit
) {
    val discoveredDevices by discovery.discoveredDevices.collectAsState()
    val isDiscovering by discovery.isDiscovering.collectAsState()

    LaunchedEffect(Unit) {
        discovery.startDiscovery()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover Devices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Discover button and status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isDiscovering) "Searching..." else "Tap to search",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isDiscovering) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { discovery.stopDiscovery() },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Stop")
                        }
                    }
                } else {
                    Button(
                        onClick = { discovery.startDiscovery() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search")
                    }
                }
            }

            // Device list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                if (discoveredDevices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isDiscovering) "Searching for devices..." else "No devices found",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(discoveredDevices) { device ->
                        val isAlreadyAdded = existingDevices.any {
                            it.device.host == device.host && it.device.port == device.port
                        }
                        DeviceDiscoveryCard(
                            device = device,
                            isAlreadyAdded = isAlreadyAdded,
                            onAdd = { onDeviceSelected(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceDiscoveryCard(
    device: DiscoveredDevice,
    isAlreadyAdded: Boolean = false,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A212C)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${device.host}:${device.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!isAlreadyAdded) {
                Button(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add device")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_button))
                }
            }
        }
    }
}
