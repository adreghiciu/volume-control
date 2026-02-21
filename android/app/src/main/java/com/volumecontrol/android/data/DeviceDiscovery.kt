package com.volumecontrol.android.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int
)

class DeviceDiscovery(private val context: Context) {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (_isDiscovering.value) return

        _isDiscovering.value = true
        _discoveredDevices.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(p0: String?, p1: Int) {
                Log.e(TAG, "Discovery start failed: $p1")
                _isDiscovering.value = false
            }

            override fun onStopDiscoveryFailed(p0: String?, p1: Int) {
                Log.e(TAG, "Discovery stop failed: $p1")
            }

            override fun onDiscoveryStarted(p0: String?) {
                Log.d(TAG, "Discovery started")
            }

            override fun onDiscoveryStopped(p0: String?) {
                Log.d(TAG, "Discovery stopped")
                _isDiscovering.value = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo != null) {
                    Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                    nsdManager?.resolveService(serviceInfo, DiscoveryResolveListener())
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo != null) {
                    Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                    _discoveredDevices.value = _discoveredDevices.value.filter {
                        it.name != serviceInfo.serviceName
                    }
                }
            }
        }

        nsdManager?.discoverServices("_volumecontrol._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener!!)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery: ${e.message}")
            }
            discoveryListener = null
        }
        _isDiscovering.value = false
    }

    private inner class DiscoveryResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            if (serviceInfo != null) {
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port

                val discovered = DiscoveredDevice(
                    name = serviceInfo.serviceName,
                    host = host,
                    port = port
                )

                // Add if not already present
                val current = _discoveredDevices.value.toMutableList()
                if (!current.any { it.host == host && it.port == port }) {
                    current.add(discovered)
                    _discoveredDevices.value = current
                    Log.d(TAG, "Added device: ${discovered.name} at ${discovered.host}:${discovered.port}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "DeviceDiscovery"
    }
}
