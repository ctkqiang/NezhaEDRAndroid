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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket

class NezhaVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var dnsSocket: DatagramSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_VPN) {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = 2000
            dnsSocket = socket

            val builder = Builder()
                .setSession("NezhaEDR-Sentinel")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addDnsServer("10.0.0.2")
                .addRoute("10.0.0.2", 32)

            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            vpnInterface = builder.establish()
            _vpnState.value = true

            startPacketInspectionAndDnsFirewall()
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun startPacketInspectionAndDnsFirewall() {
        val descriptor = vpnInterface?.fileDescriptor ?: return
        serviceScope.launch {
            val input = FileInputStream(descriptor)
            val output = FileOutputStream(descriptor)
            val buffer = ByteArray(4096)

            while (vpnInterface != null) {
                val bytesRead = try {
                    input.read(buffer)
                } catch (e: Exception) {
                    break
                }

                if (bytesRead > 0) {
                    val queriedDomain = parseDnsQueryDomain(buffer, bytesRead)
                    val destIp = parseDestinationIp(buffer, bytesRead)

                    if (queriedDomain != null) {
                        _telemetryFlow.emit(
                            TelemetryEvent.Network(
                                destinationIp = queriedDomain,
                                destinationPort = 53,
                                protocol = "DNS/UDP"
                            )
                        )

                        if (isBlockedDomain(queriedDomain)) {
                            // Synthesize DNS NXDOMAIN (Refused) packet to block threat site immediately in browser
                            val blockResponse = buildDnsNxDomainResponse(buffer, bytesRead)
                            try {
                                output.write(blockResponse)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            // Forward clean domain DNS query to upstream Google DNS 8.8.8.8 over protected socket & return answer
                            forwardCleanDnsQuery(buffer, bytesRead, output)
                        }
                    } else if (destIp != null) {
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

    private fun isBlockedDomain(domain: String): Boolean {
        val lower = domain.lowercase()
        return lower.contains("toppornsites.com") ||
                lower.contains("91porna.com") ||
                lower.contains("pornhub.com") ||
                lower.contains("xvideos.com") ||
                lower.contains("xnxx.com") ||
                lower.contains("xhamster.com") ||
                lower.contains("redtube.com") ||
                lower.contains("youporn.com") ||
                lower.contains("spankbang.com") ||
                lower.contains("eporner.com") ||
                lower.contains("brazzers.com") ||
                lower.contains("stripchat.com") ||
                lower.contains("chaturbate.com") ||
                lower.contains("malicious-c2") ||
                lower.contains("darkransom")
    }

    private fun forwardCleanDnsQuery(requestBuffer: ByteArray, length: Int, outputStream: FileOutputStream) {
        val socket = dnsSocket ?: return
        try {
            val ipHeaderLength = (requestBuffer[0].toInt() and 0x0F) * 4
            val dnsPayloadOffset = ipHeaderLength + 8
            val dnsPayloadLen = length - dnsPayloadOffset
            if (dnsPayloadLen <= 0) return

            val upstreamIp = InetAddress.getByName("8.8.8.8")
            val sendPacket = DatagramPacket(requestBuffer, dnsPayloadOffset, dnsPayloadLen, upstreamIp, 53)
            socket.send(sendPacket)

            val receiveBuffer = ByteArray(2048)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)

            val dnsResponseLen = receivePacket.length
            if (dnsResponseLen > 0) {
                val responsePacket = buildUdpIpResponsePacket(requestBuffer, receiveBuffer, dnsResponseLen)
                outputStream.write(responsePacket)
            }
        } catch (e: Exception) {
            // Socket timeout or DNS forward exception
        }
    }

    private fun buildDnsNxDomainResponse(requestBuffer: ByteArray, length: Int): ByteArray {
        val packet = ByteArray(length)
        System.arraycopy(requestBuffer, 0, packet, 0, length)

        // Swap IPv4 Source and Destination IPs
        System.arraycopy(requestBuffer, 16, packet, 12, 4)
        System.arraycopy(requestBuffer, 12, packet, 16, 4)

        // Swap UDP Ports
        val ipHeaderLength = (requestBuffer[0].toInt() and 0x0F) * 4
        val udpOffset = ipHeaderLength
        packet[udpOffset] = requestBuffer[udpOffset + 2]
        packet[udpOffset + 1] = requestBuffer[udpOffset + 3]
        packet[udpOffset + 2] = requestBuffer[udpOffset]
        packet[udpOffset + 3] = requestBuffer[udpOffset + 1]

        // Set DNS Flags to Response + NXDOMAIN (0x8183)
        val dnsOffset = udpOffset + 8
        if (length >= dnsOffset + 4) {
            packet[dnsOffset + 2] = 0x81.toByte()
            packet[dnsOffset + 3] = 0x83.toByte() // RCODE = 3 (Name Error / NXDOMAIN)
        }

        return packet
    }

    private fun buildUdpIpResponsePacket(requestBuffer: ByteArray, dnsResponseBytes: ByteArray, dnsResponseLen: Int): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + dnsResponseLen
        val packet = ByteArray(totalLen)

        // Construct IPv4 Header
        packet[0] = 0x45.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x00.toByte()
        packet[6] = 0x00.toByte()
        packet[7] = 0x00.toByte()
        packet[8] = 64.toByte()
        packet[9] = 17.toByte() // UDP

        // Swap IPs
        System.arraycopy(requestBuffer, 16, packet, 12, 4)
        System.arraycopy(requestBuffer, 12, packet, 16, 4)

        // Swap Ports
        val ipHeaderLength = (requestBuffer[0].toInt() and 0x0F) * 4
        val udpOffset = ipHeaderLength
        System.arraycopy(requestBuffer, udpOffset + 2, packet, ipHeaderLen, 2)
        System.arraycopy(requestBuffer, udpOffset, packet, ipHeaderLen + 2, 2)

        val udpLen = udpHeaderLen + dnsResponseLen
        packet[ipHeaderLen + 4] = ((udpLen shr 8) and 0xFF).toByte()
        packet[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        packet[ipHeaderLen + 6] = 0.toByte()
        packet[ipHeaderLen + 7] = 0.toByte()

        // Copy DNS Response Bytes
        System.arraycopy(dnsResponseBytes, 0, packet, ipHeaderLen + udpHeaderLen, dnsResponseLen)

        return packet
    }

    private fun parseDnsQueryDomain(buffer: ByteArray, length: Int): String? {
        if (length < 28) return null

        val ipProtocol = buffer[9].toInt() and 0xFF
        if (ipProtocol != 17) return null

        val ipHeaderLength = (buffer[0].toInt() and 0x0F) * 4
        val udpHeaderOffset = ipHeaderLength
        if (length < udpHeaderOffset + 8) return null

        val destPort = ((buffer[udpHeaderOffset + 2].toInt() and 0xFF) shl 8) or (buffer[udpHeaderOffset + 3].toInt() and 0xFF)
        if (destPort != 53) return null

        val dnsOffset = udpHeaderOffset + 8
        if (length < dnsOffset + 12) return null

        var pos = dnsOffset + 12
        val domainBuilder = StringBuilder()

        while (pos < length) {
            val labelLen = buffer[pos].toInt() and 0xFF
            if (labelLen == 0) break
            if (labelLen > 63) return null
            pos++

            if (pos + labelLen > length) return null

            if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
            domainBuilder.append(String(buffer, pos, labelLen, Charsets.US_ASCII))
            pos += labelLen
        }

        return if (domainBuilder.isNotEmpty()) domainBuilder.toString() else null
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
        dnsSocket?.close()
        dnsSocket = null
        vpnInterface?.close()
        vpnInterface = null
        _vpnState.value = false
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        stopVpn()
    }

    companion object {
        const val ACTION_START_VPN = "xin.ctkqiang.nezhaedr.START_VPN"
        const val ACTION_STOP_VPN = "xin.ctkqiang.nezhaedr.STOP_VPN"

        private val _vpnState = MutableStateFlow(false)
        val vpnState: StateFlow<Boolean> = _vpnState.asStateFlow()

        private val _telemetryFlow = MutableSharedFlow<TelemetryEvent.Network>(extraBufferCapacity = 64)
        val telemetryFlow: SharedFlow<TelemetryEvent.Network> = _telemetryFlow.asSharedFlow()
    }
}
