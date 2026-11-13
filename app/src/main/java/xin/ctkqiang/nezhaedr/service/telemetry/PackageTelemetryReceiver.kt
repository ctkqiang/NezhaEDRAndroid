package xin.ctkqiang.nezhaedr.service.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xin.ctkqiang.nezhaedr.util.EDRLogger

class PackageTelemetryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart
        val action = intent.action
        EDRLogger.i(TAG, "onReceive: Received broadcast action '$action' for package '$packageName'")

        if (packageName == null || action == null) {
            EDRLogger.w(TAG, "onReceive: Ignored broadcast due to null packageName or action")
            return
        }

        val event = TelemetryEvent.Package(
            packageName = packageName,
            action = action
        )
        val emitted = _packageEvents.tryEmit(event)
        EDRLogger.d(TAG, "onReceive: Package event emitted to telemetry flow (success: $emitted)")
    }

    companion object {
        private const val TAG = "PackageTelemetryReceiver"
        private val _packageEvents = MutableSharedFlow<TelemetryEvent.Package>(extraBufferCapacity = 64)
        val packageEvents: SharedFlow<TelemetryEvent.Package> = _packageEvents.asSharedFlow()
    }
}
