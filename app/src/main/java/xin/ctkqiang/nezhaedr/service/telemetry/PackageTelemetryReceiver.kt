package xin.ctkqiang.nezhaedr.service.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PackageTelemetryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val action = intent.action ?: return

        val event = TelemetryEvent.Package(
            packageName = packageName,
            action = action
        )
        _packageEvents.tryEmit(event)
    }

    companion object {
        private val _packageEvents = MutableSharedFlow<TelemetryEvent.Package>(extraBufferCapacity = 64)
        val packageEvents: SharedFlow<TelemetryEvent.Package> = _packageEvents.asSharedFlow()
    }
}
