package xin.ctkqiang.nezhaedr.containment

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.RuleCondition
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent

@RunWith(AndroidJUnit4::class)
class EDRNotificationManagerTest {

    private lateinit var notificationManager: EDRNotificationManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        notificationManager = EDRNotificationManager(context)
    }

    @Test
    fun testSendThreatNotificationDoesNotCrash() {
        val rule = DetectionRule(
            id = "c2_threat_block",
            schemaVersion = "2.0",
            ruleId = "RULE-C2-001",
            name = "Known C2 Threat IP Block",
            description = "Detects egress to known C2 server",
            severity = "HIGH",
            category = "NETWORK",
            enabled = true,
            engineType = "signature",
            targetSource = "dns_query",
            condition = RuleCondition(),
            action = ContainmentAction.BLOCK
        )

        val event = TelemetryEvent.Network(
            destinationIp = "192.0.2.1",
            destinationPort = 80,
            protocol = "IPv4"
        )

        val matchResult = RuleMatchResult(rule = rule, event = event)

        // Verifies notification creation and posting cleanly execute without throw/crash
        notificationManager.sendThreatNotification(matchResult)
    }
}
