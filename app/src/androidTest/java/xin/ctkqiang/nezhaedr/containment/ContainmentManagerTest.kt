package xin.ctkqiang.nezhaedr.containment

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.RuleCondition
import xin.ctkqiang.nezhaedr.engine.RuleMatchResult
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent

@RunWith(AndroidJUnit4::class)
class ContainmentManagerTest {

    private lateinit var containmentManager: ContainmentManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        containmentManager = ContainmentManager(context)
    }

    @Test
    fun testHandleNetworkRuleMatchEmitsThreatLog() = runBlocking {
        val rule = DetectionRule(
            id = "scam_gambling_url_block",
            schemaVersion = "2.0",
            ruleId = "RULE-SCAM-001",
            name = "Scam & Gambling Keyword URL Block",
            description = "Detects and blocks suspicious scam URLs",
            severity = "HIGH",
            category = "NETWORK",
            enabled = true,
            engineType = "conditional",
            targetSource = "url_request",
            condition = RuleCondition(),
            action = ContainmentAction.BLOCK
        )

        val event = TelemetryEvent.Network(
            destinationIp = "1.1.1.1",
            domain = "888vip-casino.com",
            destinationPort = 443,
            protocol = "HTTPS"
        )

        val matchResult = RuleMatchResult(rule = rule, event = event)

        containmentManager.handleRuleMatch(matchResult)

        val threatLog = containmentManager.threatLogs.first()
        assertNotNull(threatLog)
        assertEquals("RULE-SCAM-001", threatLog.ruleId)
        assertEquals("Scam & Gambling Keyword URL Block", threatLog.ruleName)
        assertEquals("BLOCK", threatLog.actionTaken)
        assertEquals("HIGH", threatLog.severity)
    }

    @Test
    fun testHandlePackageRuleMatchEmitsThreatLog() = runBlocking {
        val rule = DetectionRule(
            id = "rogue_package_alert",
            schemaVersion = "2.0",
            ruleId = "RULE-ROGUE-001",
            name = "Suspicious Rogue Package Alert",
            description = "Detects rogue software installers",
            severity = "CRITICAL",
            category = "PACKAGE",
            enabled = true,
            engineType = "signature",
            targetSource = "package_manager",
            condition = RuleCondition(),
            action = ContainmentAction.NOTIFY
        )

        val event = TelemetryEvent.Package(
            packageName = "com.test.malware.app",
            action = "android.intent.action.PACKAGE_ADDED"
        )

        val matchResult = RuleMatchResult(rule = rule, event = event)

        containmentManager.handleRuleMatch(matchResult)

        val threatLog = containmentManager.threatLogs.first()
        assertNotNull(threatLog)
        assertEquals("RULE-ROGUE-001", threatLog.ruleId)
        assertEquals("com.test.malware.app", threatLog.packageName)
        assertEquals("NOTIFY", threatLog.actionTaken)
    }
}
