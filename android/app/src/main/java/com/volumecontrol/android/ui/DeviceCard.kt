package com.volumecontrol.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volumecontrol.android.R
import com.volumecontrol.android.model.DeviceState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceCard(
    deviceState: DeviceState,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A212C)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Device name + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceState.device.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = deviceState.device.host,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A92A6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box {
                    // Status indicator
                    if (deviceState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .height(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (deviceState.error != null) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .height(24.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Connected",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .height(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Error message (if any)
            if (deviceState.error != null) {
                Text(
                    text = deviceState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume slider
            val volume = deviceState.volume ?: 0
            var pendingVolume by remember { mutableStateOf(volume) }
            var debounceJob by remember { mutableStateOf<Job?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(volume) {
                pendingVolume = volume
            }

            MuteableVolumeSlider(
                volume = pendingVolume,
                muted = deviceState.muted,
                onVolumeChange = { newValue ->
                    pendingVolume = newValue

                    debounceJob?.cancel()
                    debounceJob = scope.launch {
                        delay(500)
                        onVolumeChange(newValue)
                    }
                },
                onMuteToggle = onMuteToggle,
                enabled = deviceState.error == null && !deviceState.isLoading
            )
        }
    }
}
