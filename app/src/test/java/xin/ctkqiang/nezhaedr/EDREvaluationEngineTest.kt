package xin.ctkqiang.nezhaedr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import xin.ctkqiang.nezhaedr.data.parser.YamlRuleParser
import xin.ctkqiang.nezhaedr.data.repository.RuleRepositoryImpl
import xin.ctkqiang.nezhaedr.engine.EDREvaluationEngine
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent

class EDREvaluationEngineTest {

    @Test
    fun testRuleParsingAndEvaluation() {
        val repository = RuleRepositoryImpl()
        val rulePackage = repository.getCachedRules()
        assertNotNull(rulePackage)

        val engine = EDREvaluationEngine()
        engine.updateRules(rulePackage!!)

        val netEvent = TelemetryEvent.Network(
            destinationIp = "192.0.2.1",
            destinationPort = 80,
            protocol = "IPv4"
        )
        val matchResult = engine.evaluateNetworkEvent(netEvent)
        assertNotNull(matchResult)
        assertEquals("RULE-NET-001", matchResult?.rule?.id)

        val cleanNetEvent = TelemetryEvent.Network(
            destinationIp = "8.8.8.8",
            destinationPort = 443,
            protocol = "IPv4"
        )
        assertNull(engine.evaluateNetworkEvent(cleanNetEvent))

        val pkgEvent = TelemetryEvent.Package(
            packageName = "com.test.malware.app",
            action = "android.intent.action.PACKAGE_ADDED"
        )
        val pkgMatchResult = engine.evaluatePackageEvent(pkgEvent)
        assertNotNull(pkgMatchResult)
        assertEquals("RULE-PKG-001", pkgMatchResult?.rule?.id)
    }

    @Test
    fun testAdultContentDomainBlocking() {
        val repository = RuleRepositoryImpl()
        val rulePackage = repository.getCachedRules()
        assertNotNull(rulePackage)

        val engine = EDREvaluationEngine()
        engine.updateRules(rulePackage!!)

        val topPornEvent = TelemetryEvent.Network(
            destinationIp = "toppornsites.com",
            destinationPort = 443,
            protocol = "HTTPS"
        )
        val matchTopPorn = engine.evaluateNetworkEvent(topPornEvent)
        assertNotNull(matchTopPorn)
        assertEquals("RULE-NET-003", matchTopPorn?.rule?.id)
        assertEquals("BLOCK", matchTopPorn?.rule?.action?.name)

        val porna91Event = TelemetryEvent.Network(
            destinationIp = "91porna.com",
            destinationPort = 443,
            protocol = "HTTPS"
        )
        val match91Porna = engine.evaluateNetworkEvent(porna91Event)
        assertNotNull(match91Porna)
        assertEquals("RULE-NET-003", match91Porna?.rule?.id)
    }

    @Test
    fun testYamlRuleParsing() {
        val yamlSnippet = """
            id: RULE-NET-999
            name: Sample YAML Rule
            description: Test description for yaml rule
            severity: HIGH
            category: NETWORK
            condition:
              target_domains:
                - test-malicious-domain.com
            action: BLOCK
        """.trimIndent()

        val parsedRule = YamlRuleParser.parseYamlRule(yamlSnippet)
        assertNotNull(parsedRule)
        assertEquals("RULE-NET-999", parsedRule?.id)
        assertEquals("Sample YAML Rule", parsedRule?.name)
        assertEquals("BLOCK", parsedRule?.action?.name)
        assertEquals("test-malicious-domain.com", parsedRule?.condition?.targetDomains?.firstOrNull())
    }
}
