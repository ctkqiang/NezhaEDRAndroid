package xin.ctkqiang.nezhaedr.service.telemetry

sealed interface TelemetryEvent {
    val timestamp: Long

    data class Network(
        override val timestamp: Long = System.currentTimeMillis(),
        val destinationIp: String,
        val destinationPort: Int,
        val protocol: String
    ) : TelemetryEvent

    data class Package(
        override val timestamp: Long = System.currentTimeMillis(),
        val packageName: String,
        val action: String
    ) : TelemetryEvent
}
