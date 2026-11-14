package xin.ctkqiang.nezhaedr.service.telemetry

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
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
import xin.ctkqiang.nezhaedr.R
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

        startForegroundSentinel()
        startVpn()
        return START_STICKY
    }

    private fun startForegroundSentinel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, NezhaVpnService::class.java).apply {
            action = ACTION_STOP_VPN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val customLayout = RemoteViews(packageName, R.layout.notification_foreground_service).apply {
            setTextViewText(R.id.notif_service_title, getString(R.string.app_name))
            setTextViewText(R.id.notif_service_desc, getString(R.string.vpn_status_active))
        }

        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_power)
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_power,
                getString(R.string.btn_stop_protection),
                stopPendingIntent
            )
            .build()

        notification.flags = notification.flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        EDRLogger.i(TAG, "startForegroundSentinel: EDR background service started with ongoing notification & stop action")
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            EDRLogger.w(TAG, "startVpn: VPN interface already established")
            return
        }

        serviceScope.launch {
            EDRLogger.i(TAG, "startVpn: Configuring captive threat subnet routing interface")

            val builder = Builder()
                .setSession("NezhaCyberEDR")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)

            // Direct static threat IP routes from rulesets
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

            for (domain in threatDomains) {
                try {
                    val addresses = withContext(Dispatchers.IO) {
                        InetAddress.getAllByName(domain)
                    }
                    for (addr in addresses) {
                        addr.hostAddress?.let { ip ->
                            if (!ip.contains(":")) {
                                threatIps.add(ip)
                                EDRLogger.d(TAG, "Resolved threat domain '$domain' -> captive IP $ip")
                            }
                        }
                    }
                } catch (e: Exception) {
                    EDRLogger.v(TAG, "Domain resolution for '$domain' skipped: ${e.message}")
                }
            }

            for (ip in threatIps) {
                try {
                    builder.addRoute(ip, 32)
                    EDRLogger.d(TAG, "Added captive threat route: $ip/32")
                } catch (e: Exception) {
                    EDRLogger.v(TAG, "Failed to add captive route for $ip: ${e.message}")
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
                EDRLogger.i(TAG, "NezhaVpnService established with ${threatIps.size} captive threat routes. General traffic bypasses VPN.")
                startPacketInspection()
            } catch (e: Exception) {
                EDRLogger.fatal(TAG, "Fatal failure establishing NezhaVpnService captive builder interface", e)
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
                    runCatching {
                        val destIp = parseDestinationIp(buffer, bytesRead)
                        val dnsDomain = parseDnsQueryDomain(buffer, bytesRead)

                        if (destIp != null || dnsDomain != null) {
                            val targetIp = destIp ?: "10.0.0.1"
                            val domainName = dnsDomain ?: ""
                            EDRLogger.w(TAG, "CAPTURED CAPTIVE THREAT PACKET -> Target IP: '$targetIp', Domain: '$domainName'")
                            _telemetryFlow.emit(
                                TelemetryEvent.Network(
                                    destinationIp = targetIp,
                                    domain = domainName,
                                    destinationPort = if (dnsDomain != null) 53 else 80,
                                    protocol = if (dnsDomain != null) "DNS/UDP" else "IPv4"
                                )
                            )
                        }
                    }.onFailure { e ->
                        EDRLogger.v(TAG, "Packet inspection exception handled: ${e.message}")
                    }
                }
            }
        }
    }

    private fun parseDnsQueryDomain(buffer: ByteArray, length: Int): String? {
        return runCatching {
            if (length < 28) return null
            val versionAndIhl = buffer[0].toInt() and 0xFF
            val ipVersion = versionAndIhl shr 4
            if (ipVersion != 4) return null

            val ihl = (versionAndIhl and 0x0F) * 4
            val protocol = buffer[9].toInt() and 0xFF
            if (protocol != 17) return null // UDP protocol = 17

            val udpHeaderStart = ihl
            if (length < udpHeaderStart + 8) return null
            val destPort = ((buffer[udpHeaderStart + 2].toInt() and 0xFF) shl 8) or (buffer[udpHeaderStart + 3].toInt() and 0xFF)
            if (destPort != 53) return null // DNS Port 53

            val dnsStart = udpHeaderStart + 8
            if (length < dnsStart + 12) return null

            var pos = dnsStart + 12
            val sb = StringBuilder()
            while (pos < length) {
                val len = buffer[pos].toInt() and 0xFF
                if (len == 0 || pos + 1 + len > length) break
                if (sb.isNotEmpty()) sb.append(".")
                sb.append(String(buffer, pos + 1, len, Charsets.US_ASCII))
                pos += len + 1
            }
            if (sb.isNotEmpty()) sb.toString() else null
        }.getOrNull()
    }

    fun protectSocket(socket: Socket): Boolean {
        return protect(socket)
    }

    fun protectDatagramSocket(socket: DatagramSocket): Boolean {
        return protect(socket)
    }

    private fun parseDestinationIp(buffer: ByteArray, length: Int): String? {
        return runCatching {
            if (length < 20) return null
            val versionAndIhl = buffer[0].toInt() and 0xFF
            val ipVersion = versionAndIhl shr 4
            if (ipVersion != 4) return null

            val destIp1 = buffer[16].toInt() and 0xFF
            val destIp2 = buffer[17].toInt() and 0xFF
            val destIp3 = buffer[18].toInt() and 0xFF
            val destIp4 = buffer[19].toInt() and 0xFF
            "$destIp1.$destIp2.$destIp3.$destIp4"
        }.getOrNull()
    }

    private fun stopVpn() {
        EDRLogger.i(TAG, "stopVpn: Shutting down NezhaVpnService")
        stopForeground(STOP_FOREGROUND_REMOVE)
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
        private const val FOREGROUND_CHANNEL_ID = "nezha_edr_background_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 8888

        const val ACTION_START_VPN = "xin.ctkqiang.nezhaedr.START_VPN"
        const val ACTION_STOP_VPN = "xin.ctkqiang.nezhaedr.STOP_VPN"

        private val _vpnState = MutableStateFlow(false)
        val vpnState: StateFlow<Boolean> = _vpnState.asStateFlow()

        private val _telemetryFlow = MutableSharedFlow<TelemetryEvent.Network>(extraBufferCapacity = 64)
        val telemetryFlow: SharedFlow<TelemetryEvent.Network> = _telemetryFlow.asSharedFlow()
    }
}
