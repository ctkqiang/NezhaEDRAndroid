package xin.ctkqiang.nezhaedr.service.telemetry

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.io.FileInputStream
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket

class NezhaVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        EDRLogger.i(TAG, "onStartCommand: Received action: $action")
        if (action == ACTION_STOP_VPN) {
            EDRLogger.i(TAG, "Stopping NezhaVpnService via ACTION_STOP_VPN")
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            EDRLogger.w(TAG, "startVpn: VPN interface already established")
            return
        }

        serviceScope.launch {
            EDRLogger.i(TAG, "startVpn: Resolving threat domains to captive IP routes")

            val builder = Builder()
                .setSession("NezhaEDR-Sentinel")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)

            // Direct static threat IP routes
            val threatIps = mutableSetOf(
                "192.0.2.1",
                "198.51.100.14",
                "203.0.113.55"
            )

            // Threat domains from rulesets to resolve into captive routes
            val threatDomains = listOf(
                "toppornsites.com",
                "91porna.com",
                "pornhub.com",
                "xvideos.com",
                "xnxx.com",
                "xhamster.com",
                "redtube.com",
                "youporn.com",
                "spankbang.com",
                "eporner.com",
                "brazzers.com",
                "stripchat.com",
                "chaturbate.com",
                "beeg.com",
                "4tube.com",
                "tnaflix.com",
                "jav.guru",
                "rat.xxx",
                "youjizz.com",
                "redgifs.com",
                "beacon.darkransom.net",
                "botnet.cc-server.io",
                "malicious-c2.example.com"
            )

            // Resolve threat domain IPs asynchronously
            for (domain in threatDomains) {
                try {
                    val addresses = withContext(Dispatchers.IO) {
                        InetAddress.getAllByName(domain)
                    }
                    for (addr in addresses) {
                        addr.hostAddress?.let { ip ->
                            if (!ip.contains(":")) { // IPv4
                                threatIps.add(ip)
                                EDRLogger.d(TAG, "Resolved threat domain '$domain' -> IP $ip")
                            }
                        }
                    }
                } catch (e: Exception) {
                    EDRLogger.v(TAG, "Domain resolution for '$domain' skipped or unresolvable: ${e.message}")
                }
            }

            // Add captive routes ONLY for target threat IPs
            // All regular public internet traffic (google.com, youtube.com, etc.) bypasses TUN interface completely!
            for (ip in threatIps) {
                try {
                    builder.addRoute(ip, 32)
                    EDRLogger.d(TAG, "Added captive threat route: $ip/32")
                } catch (e: Exception) {
                    EDRLogger.w(TAG, "Failed to add route for IP $ip", e)
                }
            }

            try {
                builder.addDisallowedApplication(packageName)
                EDRLogger.d(TAG, "Excluded package '$packageName' from VPN loopback")
            } catch (e: Exception) {
                EDRLogger.w(TAG, "Failed to exclude package '$packageName' from VPN builder", e)
            }

            try {
                vpnInterface = builder.establish()
                _vpnState.value = true
                EDRLogger.i(TAG, "NezhaVpnService established successfully with ${threatIps.size} captive threat routes")
                startPacketInspection()
            } catch (e: Exception) {
                EDRLogger.fatal(TAG, "Fatal failure establishing NezhaVpnService builder interface", e)
                stopVpn()
            }
        }
    }

    private fun startPacketInspection() {
        val descriptor = vpnInterface?.fileDescriptor
        if (descriptor == null) {
            EDRLogger.e(TAG, "startPacketInspection: File descriptor is null")
            return
        }

        EDRLogger.i(TAG, "Starting captive threat packet inspection loop on TUN descriptor")
        serviceScope.launch {
            val input = FileInputStream(descriptor)
            val buffer = ByteArray(4096)

            while (vpnInterface != null) {
                val bytesRead = try {
                    input.read(buffer)
                } catch (e: Exception) {
                    EDRLogger.v(TAG, "TUN input read stream closed or interrupted")
                    break
                }

                if (bytesRead > 0) {
                    val destIp = parseDestinationIp(buffer, bytesRead)
                    if (destIp != null) {
                        EDRLogger.w(TAG, "CAPTURED THREAT PACKET -> Target IP: '$destIp'")
                        _telemetryFlow.emit(
                            TelemetryEvent.Network(
                                destinationIp = destIp,
                                destinationPort = 80,
                                protocol = "IPv4"
                            )
                        )
                    }
                }
            }
        }
    }

    fun protectSocket(socket: Socket): Boolean {
        return protect(socket)
    }

    fun protectDatagramSocket(socket: DatagramSocket): Boolean {
        return protect(socket)
    }

    private fun parseDestinationIp(buffer: ByteArray, length: Int): String? {
        if (length < 20) return null
        val versionAndIhl = buffer[0].toInt() and 0xFF
        val ipVersion = versionAndIhl shr 4
        if (ipVersion != 4) return null

        val destIp1 = buffer[16].toInt() and 0xFF
        val destIp2 = buffer[17].toInt() and 0xFF
        val destIp3 = buffer[18].toInt() and 0xFF
        val destIp4 = buffer[19].toInt() and 0xFF
        return "$destIp1.$destIp2.$destIp3.$destIp4"
    }

    private fun stopVpn() {
        EDRLogger.i(TAG, "stopVpn: Shutting down NezhaVpnService")
        vpnInterface?.close()
        vpnInterface = null
        _vpnState.value = false
        stopSelf()
        EDRLogger.i(TAG, "stopVpn: Service stopped cleanly")
    }

    override fun onDestroy() {
        super.onDestroy()
        EDRLogger.d(TAG, "onDestroy: Cancelling service coroutine scope")
        serviceJob.cancel()
        stopVpn()
    }

    companion object {
        private const val TAG = "NezhaVpnService"
        const val ACTION_START_VPN = "xin.ctkqiang.nezhaedr.START_VPN"
        const val ACTION_STOP_VPN = "xin.ctkqiang.nezhaedr.STOP_VPN"

        private val _vpnState = MutableStateFlow(false)
        val vpnState: StateFlow<Boolean> = _vpnState.asStateFlow()

        private val _telemetryFlow = MutableSharedFlow<TelemetryEvent.Network>(extraBufferCapacity = 64)
        val telemetryFlow: SharedFlow<TelemetryEvent.Network> = _telemetryFlow.asSharedFlow()
    }
}
