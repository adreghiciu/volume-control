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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volumecontrol.android.R
import com.volumecontrol.android.model.DeviceState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceCard(
    deviceState: DeviceState,
    onVolumeChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deviceState.device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    if (deviceState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.then(
                                Modifier
                                    .align(Alignment.Center)
                                    .height(20.dp)
                            ),
                            strokeWidth = 2.dp
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

            if (deviceState.error != null) {
                Text(
                    text = deviceState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val volume = deviceState.volume ?: 0
            var pendingVolume by remember { mutableStateOf(volume) }
            var debounceJob by remember { mutableStateOf<Job?>(null) }
            val scope = rememberCoroutineScope()

            // Update pendingVolume when the actual volume is fetched
            LaunchedEffect(volume) {
                pendingVolume = volume
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                    value = pendingVolume.toFloat(),
                    onValueChange = { newValue ->
                        val intValue = newValue.toInt()
                        pendingVolume = intValue

                        debounceJob?.cancel()
                        debounceJob = scope.launch {
                            delay(500) // Wait 500ms after user stops moving
                            onVolumeChange(intValue)
                        }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    enabled = deviceState.error == null && !deviceState.isLoading,
                    steps = 0
                )
                Text(
                    text = "$pendingVolume%",
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
