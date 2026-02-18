package com.volumecontrol.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.volumecontrol.android.R
import com.volumecontrol.android.model.Device

@Composable
fun AddEditDeviceDialog(
    device: Device?,
    onDismiss: () -> Unit,
    onSave: (name: String, host: String, port: Int) -> Unit
) {
    var name by remember { mutableStateOf(device?.name ?: "") }
    var host by remember { mutableStateOf(device?.host ?: "") }
    var port by remember { mutableStateOf(device?.port?.toString() ?: "8888") }
    var nameError by remember { mutableStateOf(false) }
    var hostError by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (device == null) stringResource(R.string.add_device) else stringResource(R.string.edit))
        },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.device_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it
                        hostError = false
                    },
                    label = { Text(stringResource(R.string.device_host)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hostError,
                    placeholder = { Text("192.168.1.100") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        portError = false
                    },
                    label = { Text(stringResource(R.string.device_port)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = portError,
                    placeholder = { Text("8888") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = true
                        hasError = true
                    }
                    if (host.isBlank()) {
                        hostError = true
                        hasError = true
                    }
                    val portInt = port.toIntOrNull()
                    if (portInt == null || portInt <= 0 || portInt > 65535) {
                        portError = true
                        hasError = true
                    }

                    if (!hasError && portInt != null) {
                        onSave(name, host, portInt)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
