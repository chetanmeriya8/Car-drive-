package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    data class NetworkInfo(
        val ipAddress: String,
        val isHotspotOrWifi: Boolean,
        val connectionType: String,
        val broadcastAddress: String
    )

    fun getLocalNetworkInfo(context: Context): NetworkInfo {
        var detectedIp = "127.0.0.1"
        var isHotspotOrWifi = false
        var connType = "Offline"
        var broadcast = "255.255.255.255"

        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            // Look for softap (hotspot) interfaces first, then wlan (wifi), then others
            val sortedInterfaces = interfaces.sortedByDescending { netIf ->
                val name = netIf.name.lowercase()
                when {
                    name.contains("ap") -> 3
                    name.contains("wlan") -> 2
                    name.contains("eth") -> 1
                    else -> 0
                }
            }

            for (netIf in sortedInterfaces) {
                if (!netIf.isUp || netIf.isLoopback) continue

                val ifName = netIf.name.lowercase()
                for (addr in netIf.interfaceAddresses) {
                    val inetAddr = addr.address
                    if (inetAddr is Inet4Address && !inetAddr.isLoopbackAddress) {
                        val ip = inetAddr.hostAddress ?: continue

                        // Check if it's a private network address (typical for hotspot or LAN wifi)
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            detectedIp = ip
                            isHotspotOrWifi = true

                            connType = when {
                                ifName.contains("ap") || ip.startsWith("192.168.43.1") -> "Hotspot Host (AP)"
                                ifName.contains("wlan") && ip.startsWith("192.168.43.") -> "Hotspot Client"
                                ifName.contains("wlan") -> "Local Wi-Fi"
                                else -> "Local Network"
                            }

                            addr.broadcast?.let { b ->
                                broadcast = b.hostAddress ?: "255.255.255.255"
                            }
                            return NetworkInfo(detectedIp, isHotspotOrWifi, connType, broadcast)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // fallback
        }

        // Check android system connectivity
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            isHotspotOrWifi = true
            connType = "Local Wi-Fi"
        }

        return NetworkInfo(detectedIp, isHotspotOrWifi, connType, broadcast)
    }

    fun getSuggestedHostIp(): String {
        return "192.168.43.1" // Standard default IP when connecting to an Android mobile hotspot
    }
}
