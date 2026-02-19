package com.volumecontrol.android.model

import java.util.UUID

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 8888
)

data class DeviceState(
    val device: Device,
    val volume: Int? = null,        // null = not yet fetched
    val muted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
