package com.volumecontrol.googletv

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread

/**
 * HTTP Server for volume control on port 8888.
 * Accepts GET and POST requests with JSON body containing volume values (0-100).
 */
class HttpServer(
    private val volumeController: VolumeController,
    private val port: Int = 8888
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return

        isRunning = true
        thread(isDaemon = true, name = "HttpServer-Accept") {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "HTTP Server started on port $port")

                while (isRunning) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        thread(isDaemon = true, name = "HttpServer-Request") {
                            try {
                                handleRequest(clientSocket)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error handling request", e)
                            } finally {
                                try {
                                    clientSocket.close()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error closing socket", e)
                                }
                            }
                        }
                    } catch (e: SocketException) {
                        if (isRunning) {
                            Log.e(TAG, "Socket error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server", e)
            } finally {
                try {
                    serverSocket?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing server socket", e)
                }
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    private fun handleRequest(clientSocket: java.net.Socket) {
        val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
        val writer = OutputStreamWriter(clientSocket.outputStream)

        try {
            // Read request line
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 3) return

            val method = parts[0]
            val path = parts[1]

            // Read headers
            var contentLength = 0
            while (true) {
                val header = reader.readLine() ?: break
                if (header.isEmpty()) break
                if (header.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read body if POST
            val body = if (method == "POST" && contentLength > 0) {
                val buffer = CharArray(contentLength)
                reader.read(buffer)
                String(buffer)
            } else {
                ""
            }

            // Route request
            val responseBody = when {
                path == "/volume" && method == "GET" -> handleGetVolume()
                path == "/volume" && method == "POST" -> handlePostVolume(body)
                else -> createJsonResponse(mapOf("error" to "Not found"))
            }

            // Write response with proper CRLF and Content-Length
            val responseWithNewline = responseBody + "\n"
            val responseLength = responseWithNewline.toByteArray(Charsets.UTF_8).size

            val response = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: application/json\r\n")
                append("Content-Length: $responseLength\r\n")
                append("Connection: close\r\n")
                append("\r\n")
                append(responseWithNewline)
            }

            writer.write(response)
            writer.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error in request handling", e)
        }
    }

    private fun handleGetVolume(): String {
        val volume = volumeController.getVolume()
        return createJsonResponse(mapOf("volume" to volume))
    }

    private fun handlePostVolume(body: String): String {
        return try {
            // Simple regex-based JSON parsing for volume value
            val regex = """"volume"\s*:\s*(\d+)""".toRegex()
            val match = regex.find(body)
            if (match != null) {
                val newVolume = match.groupValues[1].toInt()
                volumeController.setVolume(newVolume)
                val currentVolume = volumeController.getVolume()
                createJsonResponse(mapOf("volume" to currentVolume))
            } else {
                createJsonResponse(mapOf("error" to "Invalid JSON"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing volume", e)
            createJsonResponse(mapOf("error" to "Invalid request"))
        }
    }

    private fun createJsonResponse(data: Map<String, Any>): String {
        val entries = data.entries.joinToString(", ") { (key, value) ->
            when (value) {
                is String -> "\"$key\": \"$value\""
                else -> "\"$key\": $value"
            }
        }
        return "{$entries}"
    }

    companion object {
        private const val TAG = "HttpServer"
    }
}
